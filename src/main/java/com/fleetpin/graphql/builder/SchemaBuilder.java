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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.fleetpin.graphql.builder.TypeMeta.Flag;
import com.fleetpin.graphql.builder.annotations.Context;
import com.fleetpin.graphql.builder.annotations.Directive;
import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Id;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.Restrict;
import com.fleetpin.graphql.builder.annotations.Scalar;
import com.fleetpin.graphql.builder.annotations.Subscription;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.introspection.Introspection.DirectiveLocation;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

public class SchemaBuilder {
	public final static ObjectMapper MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(new ParameterNamesModule())
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule())
			.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS).disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
			.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

	private static final GraphQLScalarType INSTANT_SCALAR = GraphQLScalarType.newScalar().name("DateTime").coercing(new InstantCoercing()).build();
	private static final GraphQLScalarType DATE_SCALAR = GraphQLScalarType.newScalar().name("Date").coercing(new LocalDateCoercing()).build();
	private static final GraphQLScalarType DURATION_SCALAR = GraphQLScalarType.newScalar().name("Duration").coercing(new DurationCoercing()).build();
	private static final GraphQLScalarType ZONE_ID_SCALAR = GraphQLScalarType.newScalar().name("Timezone").coercing(new ZoneIdCoercing()).build();
	private static final GraphQLScalarType MONTH_DAY_SCALAR = GraphQLScalarType.newScalar().name("MonthDay").coercing(new MonthDayCoercing()).build();
	private static final GraphQLScalarType YEAR_MONTH_SCALAR = GraphQLScalarType.newScalar().name("YearMonth").coercing(new YearMonthCoercing()).build();

	private final DirectivesSchema diretives;
	private final AuthorizerSchema authorizer;

	private final GraphQLCodeRegistry.Builder codeRegistry;
	private final GraphQLDirective directive;

	private final Map<String, GraphQLType> additionalTypes;
	
	private final Builder graphQuery;
	private final Builder graphMutations;
	private final Builder graphSubscriptions;
	
	private final EntityProcessor entityProcessor;

	
	private SchemaBuilder(DirectivesSchema diretives, AuthorizerSchema authorizer) {
		this.diretives = diretives;
		this.authorizer = authorizer;
		
		this.graphQuery = GraphQLObjectType.newObject();
		graphQuery.name("Query");
		this.graphMutations = GraphQLObjectType.newObject();
		graphMutations.name("Mutations");
		this.graphSubscriptions = GraphQLObjectType.newObject();
		graphSubscriptions.name("Subscriptions");
		this.additionalTypes = new HashMap<>();
		this.codeRegistry = GraphQLCodeRegistry.newCodeRegistry();

		this.entityProcessor = new EntityProcessor(additionalTypes, codeRegistry, diretives);

		this.directive = GraphQLDirective.newDirective()
				.name("authorization")
				.validLocation(DirectiveLocation.FIELD_DEFINITION).build();
	}
	
	private SchemaBuilder process(Set<Method> endPoints) throws ReflectiveOperationException {
		for(var method: endPoints) {
			if(!Modifier.isStatic(method.getModifiers())) {
				throw new RuntimeException("End point must be a static method");
			}
			//TODO:query vs mutation
			GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();
			field.name(method.getName());

			TypeMeta meta = new TypeMeta(entityProcessor, null, method.getReturnType(), method.getGenericReturnType());
			field.type(getType(meta, method.getAnnotations()));
			for(int i = 0; i < method.getParameterCount(); i++) {
				GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
				if(isContext(method.getParameterTypes()[i])) {
					continue;
				}
				
				TypeMeta inputMeta = new TypeMeta(this.entityProcessor, null, method.getParameterTypes()[i], method.getGenericParameterTypes()[i]);
				argument.type(getInputType(inputMeta, method.getParameterAnnotations()[i]));//TODO:dirty cast
				argument.name(method.getParameters()[i].getName());
				//TODO: argument.defaultValue(defaultValue)
				field.argument(argument);
			}

			if(method.isAnnotationPresent(Query.class)) {
				field.withDirective(directive);
				graphQuery.field(field);

				DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);

				codeRegistry.dataFetcher(graphQuery.build(), field.build(), fetcher);
			}else if(method.isAnnotationPresent(Mutation.class)) {
				graphMutations.field(field);
				DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);
				codeRegistry.dataFetcher(FieldCoordinates.coordinates("Mutations", method.getName()), fetcher);
			}else if(method.isAnnotationPresent(Subscription.class)) {
				graphSubscriptions.field(field);
				DataFetcher<?> fetcher = buildFetcher(diretives, authorizer, method, meta);
				codeRegistry.dataFetcher(FieldCoordinates.coordinates("Subscriptions", method.getName()), fetcher);
			}

		}
		return this;
	}

	private graphql.GraphQL.Builder build() {
		codeRegistry.typeResolver("ID", env -> {
			return null;
		});
		return GraphQL.newGraphQL(GraphQLSchema.newSchema().codeRegistry(codeRegistry.build()).additionalTypes(new HashSet<>(additionalTypes.values())).query(graphQuery.build()).mutation(graphMutations).subscription(graphSubscriptions).additionalDirective(directive).build());

	}

	private static boolean isContext(Class<?> class1) {
		return class1.isAssignableFrom(DataFetchingEnvironment.class) || class1.isAnnotationPresent(Context.class);
	}

	private static <T extends Annotation> DataFetcher<?> buildFetcher(DirectivesSchema diretives, AuthorizerSchema authorizer, Method method, TypeMeta meta) {
		DataFetcher<?> fetcher = env -> {
			try {
				Object[] args = new Object[method.getParameterCount()];
				for(int i = 0; i < args.length; i++) {
					Class<?> type = method.getParameterTypes()[i];
					if(type.isAssignableFrom(env.getClass())) {
						args[i] = env;
					}else if(type.isAssignableFrom(env.getContext().getClass())) {
						args[i] = env.getContext();
					}else {
						Object obj = env.getArgument(method.getParameters()[i].getName());
						//if they don't match use json to make them

						if(obj instanceof List) {
							var genericType = method.getGenericParameterTypes()[i];
							args[i] = MAPPER.convertValue(obj, new TypeReference<Object>() {
								@Override
								public Type getType() {
									return genericType;
								}
							});
						}else if(type.isInstance(obj) ) {
							args[i] = obj;
						}else {
							if(Optional.class.isAssignableFrom(type)) {
								if(obj == null) {
									args[i] = Optional.empty();
								}else {
									var genericType = method.getGenericParameterTypes()[i];
									var t = ((ParameterizedType) genericType).getActualTypeArguments()[0];
									args[i] = Optional.of(MAPPER.convertValue(obj, new TypeReference<Object>() {
										@Override
										public Type getType() {
											return t;
										}
									}));
								}
							}else {
								var t = method.getParameters()[i].getParameterizedType();
								args[i] = MAPPER.convertValue(obj, new TypeReference<Object>() {
									@Override
									public Type getType() {
										return t;
									}
								});
							}
						}
					}
				}

				return method.invoke(null, args);
			}catch (InvocationTargetException e) {
				e.printStackTrace();
				if(e.getCause() instanceof Exception) {
					throw (Exception) e.getCause();
				}else {
					throw e;
				}

			}catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		};
		fetcher = diretives.wrap(method, meta, fetcher);

		if(authorizer != null) {
			fetcher = authorizer.wrap(fetcher, method);
		}
		return fetcher;
	}

	static GraphQLOutputType getType(TypeMeta meta, Annotation[] annotations) {
		
		
		GraphQLOutputType toReturn = getTypeInner(meta.getType(), annotations, meta.getName());
		boolean required = true;
		for(var flag: meta.getFlags()) {
			if(flag == Flag.OPTIONAL) {
				required = false;
			}
			if(flag == Flag.ARRAY) {
				if(required) {
					toReturn = GraphQLNonNull.nonNull(toReturn);
				}
				toReturn = GraphQLList.list(toReturn);
				required = true;
			}
		}
		if(required) {
			toReturn = GraphQLNonNull.nonNull(toReturn);
		}
		return toReturn;

	}
	
	
	private static GraphQLOutputType getTypeInner(Class<?> type, Annotation[] annotations, String name) {
		for(Annotation an: annotations) {
			if(an.annotationType().equals(Id.class)) {
				return Scalars.GraphQLID;
			}
		}

		if(type.equals(BigDecimal.class)) {
			return Scalars.GraphQLBigDecimal;
		}else if(type.equals(BigInteger.class)) {
			return Scalars.GraphQLBigInteger;
		}else if(type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
			return Scalars.GraphQLBoolean;
		}else if(type.equals(Byte.class) || type.equals(Byte.TYPE)) {
			return Scalars.GraphQLByte;
		}else if(type.equals(Character.class) || type.equals(Character.TYPE)) {
			return Scalars.GraphQLChar;
		}else if(type.equals(Float.class) || type.equals(Float.TYPE)) {
			return Scalars.GraphQLFloat;
		}else if(type.equals(Double.class) || type.equals(Double.TYPE)) {
			return Scalars.GraphQLFloat;
		}else if(type.equals(Integer.class) || type.equals(Integer.TYPE)) {
			return Scalars.GraphQLInt;
		}else if(type.equals(Long.class) || type.equals(Long.TYPE)) {
			return Scalars.GraphQLLong;
		}else if(type.equals(Short.class) || type.equals(Short.TYPE)) {
			return Scalars.GraphQLShort;
		}else if(type.equals(String.class)) {
			return Scalars.GraphQLString;
		}else if(type.equals(Instant.class)) {
			return INSTANT_SCALAR;
		}else if(type.equals(LocalDate.class)) {
			return DATE_SCALAR;
		}else if(type.equals(ZoneId.class)) {
			return ZONE_ID_SCALAR;
		}else if(type.equals(Duration.class)) {
			return DURATION_SCALAR;
		}else if(type.equals(MonthDay.class)) {
			return MONTH_DAY_SCALAR;
		}else if(type.equals(YearMonth.class)) {
			return YEAR_MONTH_SCALAR;
		}
		
		
		if(type.isEnum()) {
			return GraphQLTypeReference.typeRef(name);
		}
		
		if(type.isAnnotationPresent(Entity.class)) {
			return GraphQLTypeReference.typeRef(name);
		}
		
		if(type.isAnnotationPresent(Scalar.class)) {
			return GraphQLTypeReference.typeRef(name);
		}
		
		throw new RuntimeException("Unsupport type " + type);
	}
	
	
	static GraphQLInputType getInputType(TypeMeta meta, Annotation[] annotations) {
		
		GraphQLInputType toReturn = getInputTypeInner(meta.getType(), annotations, meta.getInputName());
		
		boolean required = true;
		for(var flag: meta.getFlags()) {
			if(flag == Flag.OPTIONAL) {
				required = false;
			}
			if(flag == Flag.ARRAY) {
				if(required) {
					toReturn = GraphQLNonNull.nonNull(toReturn);
				}
				toReturn = GraphQLList.list(toReturn);
				required = true;
			}
		}
		if(required) {
			toReturn = GraphQLNonNull.nonNull(toReturn);
		}
		return toReturn;
	}
	
	private static GraphQLInputType getInputTypeInner(Class<?> type, Annotation[] annotations, String name) {

		for(Annotation an: annotations) {
			if(an.annotationType().equals(Id.class)) {
				return Scalars.GraphQLID;
			}
		}

		if(type.equals(BigDecimal.class)) {
			return Scalars.GraphQLBigDecimal;
		}else if(type.equals(BigInteger.class)) {
			return Scalars.GraphQLBigInteger;
		}else if(type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
			return Scalars.GraphQLBoolean;
		}else if(type.equals(Byte.class) || type.equals(Byte.TYPE)) {
			return Scalars.GraphQLByte;
		}else if(type.equals(Character.class) || type.equals(Character.TYPE)) {
			return Scalars.GraphQLChar;
		}else if(type.equals(Float.class) || type.equals(Float.TYPE)) {
			return Scalars.GraphQLFloat;
		}else if(type.equals(Double.class) || type.equals(Double.TYPE)) {
			return Scalars.GraphQLFloat;
		}else if(type.equals(Integer.class) || type.equals(Integer.TYPE)) {
			return Scalars.GraphQLInt;
		}else if(type.equals(Long.class) || type.equals(Long.TYPE)) {
			return Scalars.GraphQLLong;
		}else if(type.equals(Short.class) || type.equals(Short.TYPE)) {
			return Scalars.GraphQLShort;
		}else if(type.equals(String.class)) {
			return Scalars.GraphQLString;
		}else if(type.equals(Instant.class)) {
			return INSTANT_SCALAR;
		}else if(type.equals(LocalDate.class)) {
			return DATE_SCALAR;
		}else if(type.equals(ZoneId.class)) {
			return ZONE_ID_SCALAR;
		}else if(type.equals(Duration.class)) {
			return DURATION_SCALAR;
		}else if(type.equals(MonthDay.class)) {
			return MONTH_DAY_SCALAR;
		}else if(type.equals(YearMonth.class)) {
			return YEAR_MONTH_SCALAR;
		}

		
		
		
		if(type.isEnum()) {
			return GraphQLTypeReference.typeRef(name);
		}
		
		if(type.isAnnotationPresent(Scalar.class)) {
			return GraphQLTypeReference.typeRef(name);
		}
		
		
		if(type.isAnnotationPresent(Entity.class)) {
			return GraphQLTypeReference.typeRef(name);
		}
		
		throw new RuntimeException("Unsupport type " + type);
	}

	public static graphql.GraphQL.Builder build(String... classPath) throws ReflectiveOperationException {
		
		
		Reflections reflections = new Reflections(classPath, new SubTypesScanner(), new MethodAnnotationsScanner(), new TypeAnnotationsScanner());
		Set<Class<? extends Authorizer>> authorizers = reflections.getSubTypesOf(Authorizer.class);
		//want to make everything split by package
		AuthorizerSchema authorizer = AuthorizerSchema.build(new HashSet<>(Arrays.asList(classPath)), authorizers);
		
		
		Set<Class<?>> dierctivesTypes = reflections.getTypesAnnotatedWith(Directive.class);
		
		Set<Class<?>> restrict = reflections.getTypesAnnotatedWith(Restrict.class);
		List<RestrictTypeFactory<?>> globalRestricts = new ArrayList<>();
		
		for(var r: restrict) {
			Restrict annotation = r.getAnnotation(Restrict.class);
			var factoryClass = annotation.value();
			var factory = factoryClass.getConstructor().newInstance();
			if(!factory.extractType().equals(r)) {
				throw new RuntimeException("Restrict annotation does match class applied to targets" + factory.extractType() + " but was on class " + r);
			}
			globalRestricts.add(factory);
		}
		
		DirectivesSchema diretivesSchema = DirectivesSchema.build(globalRestricts, dierctivesTypes);
		
		Set<Class<?>> types = reflections.getTypesAnnotatedWith(Entity.class);
		
		Set<Class<?>> scalars = reflections.getTypesAnnotatedWith(Scalar.class);
		
		var mutations = reflections.getMethodsAnnotatedWith(Mutation.class);
		var subscriptions = reflections.getMethodsAnnotatedWith(Subscription.class);
		var queries = reflections.getMethodsAnnotatedWith(Query.class);
		
		var endPoints = new HashSet<>(mutations);
		endPoints.addAll(subscriptions);
		endPoints.addAll(queries);
		
		types.removeIf(t -> t.getDeclaredAnnotation(Entity.class) == null);
		types.removeIf(t -> t.isAnonymousClass());
		scalars.removeIf(t -> t.isAnonymousClass());
		
		return new SchemaBuilder(diretivesSchema, authorizer).process(endPoints).build();
	}


	
}
