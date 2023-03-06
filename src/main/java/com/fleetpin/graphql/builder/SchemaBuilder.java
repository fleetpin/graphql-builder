/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.annotations.Context;
import com.fleetpin.graphql.builder.annotations.Directive;
import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.GraphQLDeprecated;
import com.fleetpin.graphql.builder.annotations.GraphQLDescription;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.Restrict;
import com.fleetpin.graphql.builder.annotations.Restricts;
import com.fleetpin.graphql.builder.annotations.SchemaOption;
import com.fleetpin.graphql.builder.annotations.Subscription;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public class SchemaBuilder {

	private final DirectivesSchema diretives;
	private final AuthorizerSchema authorizer;

	private final GraphQLCodeRegistry.Builder codeRegistry;

	private final GraphQLObjectType.Builder graphQuery;
	private final GraphQLObjectType.Builder graphMutations;
	private final GraphQLObjectType.Builder graphSubscriptions;

	private final EntityProcessor entityProcessor;

	private SchemaBuilder(List<GraphQLScalarType> scalars, DirectivesSchema diretives, AuthorizerSchema authorizer) {
		this.diretives = diretives;
		this.authorizer = authorizer;

		this.graphQuery = GraphQLObjectType.newObject();
		graphQuery.name("Query");
		this.graphMutations = GraphQLObjectType.newObject();
		graphMutations.name("Mutations");
		this.graphSubscriptions = GraphQLObjectType.newObject();
		graphSubscriptions.name("Subscriptions");
		this.codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

		this.entityProcessor = new EntityProcessor(scalars, codeRegistry, diretives);

		diretives.processSDL(entityProcessor);
	}

	private SchemaBuilder process(Set<Method> endPoints) throws ReflectiveOperationException {
		for (var method : endPoints) {
			if (!Modifier.isStatic(method.getModifiers())) {
				throw new RuntimeException("End point must be a static method");
			}
			//TODO:query vs mutation
			GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();

			var deprecated = method.getAnnotation(GraphQLDeprecated.class);
			if (deprecated != null) {
				field.deprecate(deprecated.value());
			}

			var description = method.getAnnotation(GraphQLDescription.class);
			if (description != null) {
				field.description(description.value());
			}

			field.name(method.getName());

			TypeMeta meta = new TypeMeta(null, method.getReturnType(), method.getGenericReturnType());
			var type = entityProcessor.getType(meta, method.getAnnotations());
			field.type(type);
			for (int i = 0; i < method.getParameterCount(); i++) {
				GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
				if (isContext(method.getParameterTypes()[i], method.getParameterAnnotations()[i])) {
					continue;
				}

				TypeMeta inputMeta = new TypeMeta(null, method.getParameterTypes()[i], method.getGenericParameterTypes()[i]);
				argument.type(entityProcessor.getInputType(inputMeta, method.getParameterAnnotations()[i])); //TODO:dirty cast
				argument.name(method.getParameters()[i].getName());
				//TODO: argument.defaultValue(defaultValue)
				field.argument(argument);
			}

			diretives.addSchemaDirective(method, method.getDeclaringClass(), field::withAppliedDirective);
			if (method.isAnnotationPresent(Query.class)) {
				graphQuery.field(field);
				DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);
				codeRegistry.dataFetcher(graphQuery.build(), field.build(), fetcher);
			} else if (method.isAnnotationPresent(Mutation.class)) {
				graphMutations.field(field);
				DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);
				codeRegistry.dataFetcher(FieldCoordinates.coordinates("Mutations", method.getName()), fetcher);
			} else if (method.isAnnotationPresent(Subscription.class)) {
				graphSubscriptions.field(field);
				DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);
				codeRegistry.dataFetcher(FieldCoordinates.coordinates("Subscriptions", method.getName()), fetcher);
			}
		}
		return this;
	}

	private SchemaBuilder processTypes(Set<Class<?>> types) {
		for (var type : types) {
			TypeMeta meta = new TypeMeta(null, type, type);

			var annotation = type.getAnnotation(Entity.class);
			if (annotation.value() != SchemaOption.INPUT) {
				this.entityProcessor.getEntity(meta).getInnerType(meta);
			}
		}
		return this;
	}

	private graphql.schema.GraphQLSchema.Builder build(Set<Class<? extends SchemaConfiguration>> schemaConfiguration) {
		var builder = GraphQLSchema.newSchema().codeRegistry(codeRegistry.build()).additionalTypes(entityProcessor.getAdditionalTypes());

		var query = graphQuery.build();
		builder.query(query);

		var mutations = graphMutations.build();
		if (!mutations.getFields().isEmpty()) {
			builder.mutation(mutations);
		}
		var subscriptions = graphSubscriptions.build();
		if (!subscriptions.getFields().isEmpty()) {
			builder.subscription(subscriptions);
		}

		diretives.getSchemaDirective().forEach(directive -> builder.additionalDirective(directive));

		for (var schema : schemaConfiguration) {
			this.diretives.addSchemaDirective(schema, schema, builder::withSchemaAppliedDirective);
		}
		return builder;
	}

	private static boolean isContext(Class<?> class1, Annotation[] annotations) {
		for (var annotation : annotations) {
			if (annotation instanceof Context) {
				return true;
			}
		}
		return (
			class1.isAssignableFrom(GraphQLContext.class) || class1.isAssignableFrom(DataFetchingEnvironment.class) || class1.isAnnotationPresent(Context.class)
		);
	}

	private <T extends Annotation> DataFetcher<?> buildFetcher(DirectivesSchema diretives, AuthorizerSchema authorizer, Method method, TypeMeta meta) {
		DataFetcher<?> fetcher = buildDataFetcher(meta, method);
		fetcher = diretives.wrap(method, meta, fetcher);

		if (authorizer != null) {
			fetcher = authorizer.wrap(fetcher, method);
		}
		return fetcher;
	}

	private DataFetcher<?> buildDataFetcher(TypeMeta meta, Method method) {
		Function<DataFetchingEnvironment, Object>[] resolvers = new Function[method.getParameterCount()];

		for (int i = 0; i < resolvers.length; i++) {
			Class<?> type = method.getParameterTypes()[i];
			var name = method.getParameters()[i].getName();
			var generic = method.getGenericParameterTypes()[i];
			var argMeta = new TypeMeta(meta, type, generic);
			resolvers[i] = buildResolver(name, argMeta, method.getParameterAnnotations()[i]);
		}

		DataFetcher<?> fetcher = env -> {
			try {
				Object[] args = new Object[resolvers.length];
				for (int i = 0; i < resolvers.length; i++) {
					args[i] = resolvers[i].apply(env);
				}
				return method.invoke(null, args);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof Exception) {
					throw (Exception) e.getCause();
				} else {
					throw e;
				}
			} catch (Exception e) {
				throw e;
			}
		};

		return fetcher;
	}

	private Function<DataFetchingEnvironment, Object> buildResolver(String name, TypeMeta argMeta, Annotation[] annotations) {
		if (isContext(argMeta.getType(), annotations)) {
			var type = argMeta.getType();
			if (type.isAssignableFrom(DataFetchingEnvironment.class)) {
				return env -> env;
			}
			if (type.isAssignableFrom(GraphQLContext.class)) {
				return env -> env.getGraphQlContext();
			}
			return env -> {
				var localContext = env.getLocalContext();
				if (localContext != null && type.isAssignableFrom(localContext.getClass())) {
					return localContext;
				}

				var context = env.getContext();
				if (context != null && type.isAssignableFrom(context.getClass())) {
					return context;
				}

				context = env.getGraphQlContext().get(name);
				if (context != null && type.isAssignableFrom(context.getClass())) {
					return context;
				}
				throw new RuntimeException("Context object " + name + " not found");
			};
		}

		var resolver = entityProcessor.getResolver(argMeta);

		return env -> {
			var arg = env.getArgument(name);
			return resolver.convert(arg, env.getGraphQlContext(), env.getLocale());
		};
	}

	public static GraphQLSchema build(String... classPath) throws ReflectiveOperationException {
		return builder(classPath).build();
	}

	public static GraphQLSchema.Builder builder(String... classpath) throws ReflectiveOperationException {
		var builder = builder();
		for (var path : classpath) {
			builder.classpath(path);
		}
		return builder.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private List<String> classpaths = new ArrayList<>();
		private List<GraphQLScalarType> scalars = new ArrayList<>();

		private Builder() {}

		public Builder classpath(String classpath) {
			this.classpaths.add(classpath);
			return this;
		}

		public Builder scalar(GraphQLScalarType scalar) {
			this.scalars.add(scalar);
			return this;
		}

		public GraphQLSchema.Builder build() throws ReflectiveOperationException {
			Reflections reflections = new Reflections(classpaths, Scanners.SubTypes, Scanners.MethodsAnnotated, Scanners.TypesAnnotated);
			Set<Class<? extends Authorizer>> authorizers = reflections.getSubTypesOf(Authorizer.class);
			//want to make everything split by package
			AuthorizerSchema authorizer = AuthorizerSchema.build(new HashSet<>(classpaths), authorizers);

			Set<Class<? extends SchemaConfiguration>> schemaConfiguration = reflections.getSubTypesOf(SchemaConfiguration.class);

			Set<Class<?>> dierctivesTypes = reflections.getTypesAnnotatedWith(Directive.class);

			Set<Class<?>> restrict = reflections.getTypesAnnotatedWith(Restrict.class);
			Set<Class<?>> restricts = reflections.getTypesAnnotatedWith(Restricts.class);
			List<RestrictTypeFactory<?>> globalRestricts = new ArrayList<>();

			for (var r : restrict) {
				Restrict annotation = r.getAnnotation(Restrict.class);
				var factoryClass = annotation.value();
				var factory = factoryClass.getConstructor().newInstance();
				if (!factory.extractType().isAssignableFrom(r)) {
					throw new RuntimeException("Restrict annotation does match class applied to targets" + factory.extractType() + " but was on class " + r);
				}
				globalRestricts.add(factory);
			}

			for (var r : restricts) {
				Restricts annotations = r.getAnnotation(Restricts.class);
				for (Restrict annotation : annotations.value()) {
					var factoryClass = annotation.value();
					var factory = factoryClass.getConstructor().newInstance();

					if (!factory.extractType().isAssignableFrom(r)) {
						throw new RuntimeException(
							"Restrict annotation does match class applied to targets" + factory.extractType() + " but was on class " + r
						);
					}
					globalRestricts.add(factory);
				}
			}

			DirectivesSchema diretivesSchema = DirectivesSchema.build(globalRestricts, dierctivesTypes);

			Set<Class<?>> types = reflections.getTypesAnnotatedWith(Entity.class);

			var mutations = reflections.getMethodsAnnotatedWith(Mutation.class);
			var subscriptions = reflections.getMethodsAnnotatedWith(Subscription.class);
			var queries = reflections.getMethodsAnnotatedWith(Query.class);

			var endPoints = new HashSet<>(mutations);
			endPoints.addAll(subscriptions);
			endPoints.addAll(queries);

			types.removeIf(t -> t.getDeclaredAnnotation(Entity.class) == null);
			types.removeIf(t -> t.isAnonymousClass());

			return new SchemaBuilder(scalars, diretivesSchema, authorizer).processTypes(types).process(endPoints).build(schemaConfiguration);
		}
	}
}
