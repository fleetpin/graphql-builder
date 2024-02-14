package com.fleetpin.graphql.builder;

import static com.fleetpin.graphql.builder.EntityUtil.isContext;

import com.fleetpin.graphql.builder.annotations.*;
import graphql.GraphQLContext;
import graphql.Scalars;
import graphql.language.StringValue;
import graphql.schema.*;
import graphql.schema.GraphQLFieldDefinition.Builder;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

class MethodProcessor {

	private final DataFetcherRunner dataFetcherRunner;
	private final EntityProcessor entityProcessor;
	private final DirectivesSchema diretives;

	private final GraphQLCodeRegistry.Builder codeRegistry;

	private final GraphQLObjectType.Builder graphQuery;
	private final GraphQLObjectType.Builder graphMutations;
	private final GraphQLObjectType.Builder graphSubscriptions;

	public MethodProcessor(DataFetcherRunner dataFetcherRunner, EntityProcessor entityProcessor, DirectivesSchema diretives) {
		this.dataFetcherRunner = dataFetcherRunner;
		this.entityProcessor = entityProcessor;
		this.diretives = diretives;
		this.codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

		this.graphQuery = GraphQLObjectType.newObject();
		graphQuery.name("Query");
		this.graphMutations = GraphQLObjectType.newObject();
		graphMutations.name("Mutations");
		this.graphSubscriptions = GraphQLObjectType.newObject();
		graphSubscriptions.name("Subscriptions");
	}

	void process(AuthorizerSchema authorizer, Method method) throws ReflectiveOperationException {
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new RuntimeException("End point must be a static method");
		}
		FieldCoordinates coordinates;
		GraphQLObjectType.Builder object;
		if (method.isAnnotationPresent(Query.class)) {
			coordinates = FieldCoordinates.coordinates("Query", method.getName());
			object = graphQuery;
		} else if (method.isAnnotationPresent(Mutation.class)) {
			coordinates = FieldCoordinates.coordinates("Mutations", method.getName());
			object = graphMutations;
		} else if (method.isAnnotationPresent(Subscription.class)) {
			coordinates = FieldCoordinates.coordinates("Subscriptions", method.getName());
			object = graphSubscriptions;
		} else {
			return;
		}

		object.field(process(authorizer, coordinates, null, method));
	}

	Builder process(AuthorizerSchema authorizer, FieldCoordinates coordinates, TypeMeta parentMeta, Method method)
		throws InvocationTargetException, IllegalAccessException {
		GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();

		entityProcessor.addSchemaDirective(method, method.getDeclaringClass(), field::withAppliedDirective);

		var deprecated = method.getAnnotation(GraphQLDeprecated.class);
		if (deprecated != null) {
			field.deprecate(deprecated.value());
		}

		var description = method.getAnnotation(GraphQLDescription.class);
		if (description != null) {
			field.description(description.value());
		}

		field.name(coordinates.getFieldName());

		TypeMeta meta = new TypeMeta(parentMeta, method.getReturnType(), method.getGenericReturnType(), method);
		var type = entityProcessor.getType(meta, method.getAnnotations());
		field.type(type);
		for (int i = 0; i < method.getParameterCount(); i++) {
			GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
			if (isContext(method.getParameterTypes()[i], method.getParameterAnnotations()[i])) {
				continue;
			}

			TypeMeta inputMeta = new TypeMeta(null, method.getParameterTypes()[i], method.getGenericParameterTypes()[i], method.getParameters()[i]);
			argument.type(entityProcessor.getInputType(inputMeta, method.getParameterAnnotations()[i])); //TODO:dirty cast

			description = method.getParameters()[i].getAnnotation(GraphQLDescription.class);
			if (description != null) {
				argument.description(description.value());
			}

			var parameter = method.getParameters()[i];
			for (Annotation annotation : parameter.getAnnotations()) {
				// Check to see if the annotation is a directive
				if (!annotation.annotationType().isAnnotationPresent(Directive.class)) {
					continue;
				}
				var annotationType = annotation.annotationType();
				// Get the values out of the directive annotation
				var methods = annotationType.getDeclaredMethods();

				// Get the applied directive and add it to the argument
				var appliedDirective = getAppliedDirective(annotation, annotationType, methods);
				argument.withAppliedDirective(appliedDirective);
			}

			argument.name(method.getParameters()[i].getName());
			//TODO: argument.defaultValue(defaultValue)
			field.argument(argument);
		}

		DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);
		codeRegistry.dataFetcher(coordinates, fetcher);
		return field;
	}

	private GraphQLAppliedDirective getAppliedDirective(Annotation annotation, Class<? extends Annotation> annotationType, Method[] methods)
		throws IllegalAccessException, InvocationTargetException {
		var appliedDirective = new GraphQLAppliedDirective.Builder().name(annotationType.getSimpleName());
		for (var definedMethod : methods) {
			var name = definedMethod.getName();
			var value = definedMethod.invoke(annotation);
			if (value == null) {
				continue;
			}

			TypeMeta innerMeta = new TypeMeta(null, definedMethod.getReturnType(), definedMethod.getGenericReturnType());
			var argumentType = entityProcessor.getEntity(innerMeta).getInputType(innerMeta, definedMethod.getAnnotations());
			appliedDirective.argument(GraphQLAppliedDirectiveArgument.newArgument().name(name).type(argumentType).valueProgrammatic(value).build());
		}
		return appliedDirective.build();
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

		method.setAccessible(true);

		for (int i = 0; i < resolvers.length; i++) {
			Class<?> type = method.getParameterTypes()[i];
			var name = method.getParameters()[i].getName();
			var generic = method.getGenericParameterTypes()[i];
			var argMeta = new TypeMeta(meta, type, generic, method.getParameters()[i]);
			resolvers[i] = buildResolver(name, argMeta, method.getParameterAnnotations()[i]);
		}

		DataFetcher<?> fetcher = env -> {
			try {
				Object[] args = new Object[resolvers.length];
				for (int i = 0; i < resolvers.length; i++) {
					args[i] = resolvers[i].apply(env);
				}
				return method.invoke(env.getSource(), args);
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

		return dataFetcherRunner.manage(method, fetcher);
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

	public GraphQLCodeRegistry.Builder getCodeRegistry() {
		return codeRegistry;
	}

	GraphQLObjectType.Builder getGraphQuery() {
		return graphQuery;
	}

	GraphQLObjectType.Builder getGraphMutations() {
		return graphMutations;
	}

	GraphQLObjectType.Builder getGraphSubscriptions() {
		return graphSubscriptions;
	}
}
