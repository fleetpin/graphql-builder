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
import com.fleetpin.graphql.builder.annotations.GraphQLIgnore;
import com.fleetpin.graphql.builder.annotations.Id;
import com.fleetpin.graphql.builder.annotations.InputIgnore;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.Restrict;
import com.fleetpin.graphql.builder.annotations.Scalar;
import com.fleetpin.graphql.builder.annotations.SchemaOption;
import com.fleetpin.graphql.builder.annotations.Subscription;
import com.google.common.collect.Sets;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.introspection.Introspection.DirectiveLocation;
import graphql.schema.Coercing;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.TypeRuntimeWiring;

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

	private static graphql.GraphQL.Builder build(DirectivesSchema diretives, AuthorizerSchema authorizer, Set<Class<?>> types, Set<Class<?>> scalars, Set<Method> endPoints) throws ReflectiveOperationException {
		Builder graphQuery = GraphQLObjectType.newObject();
		graphQuery.name("Query");
		Builder graphMutations = GraphQLObjectType.newObject();
		graphMutations.name("Mutations");
		Builder graphSubscriptions = GraphQLObjectType.newObject();
		graphSubscriptions.name("Subscriptions");
		Map<String, GraphQLType> additionalTypes = new HashMap<>();
		GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
		
		
		GraphQLDirective directive = GraphQLDirective.newDirective()
				.name("authorization")
				.validLocation(DirectiveLocation.FIELD_DEFINITION).build();
		
		for(Class<?> scalar: scalars) {
			GraphQLScalarType.Builder scalarType = GraphQLScalarType.newScalar();
			String typeName = getName(scalar);
			scalarType.name(typeName);
			Class<? extends Coercing> coerecing = scalar.getAnnotation(Scalar.class).value();
			scalarType.coercing(coerecing.getDeclaredConstructor().newInstance());
			var built = scalarType.build();
			if(additionalTypes.put(built.getName(), built) != null) {
				throw new RuntimeException(built.getName() + "defined more than once");
			}
			continue;			
		}
		
		for(var method: endPoints) {
			if(!Modifier.isStatic(method.getModifiers())) {
    			throw new RuntimeException("End point must be a static method");
    		}
    		//TODO:query vs mutation
    		GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();
    		field.name(method.getName());
    		
    		TypeMeta meta = new TypeMeta(method.getReturnType(), method.getGenericReturnType());
    		field.type(getType(meta, method.getAnnotations()));
    		for(int i = 0; i < method.getParameterCount(); i++) {
    			GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
    			if(isContext(method.getParameterTypes()[i])) {
    				continue;
    			}
    			argument.type(getInputType(method.getParameterTypes()[i], method.getGenericParameterTypes()[i], method.getParameterAnnotations()[i]));//TODO:dirty cast
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
		
		
		for(Class<?> type: types) {
			
			//special handling
			if(type.isEnum()) {
				graphql.schema.GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum();
				String typeName = getName(type);
				enumType.name(typeName);
				
				Object[] enums = type.getEnumConstants();
				for(Object e: enums) {
					Enum a = (Enum) e;
					if(type.getDeclaredField(e.toString()).isAnnotationPresent(GraphQLIgnore.class)) {
						continue;
					}
					enumType.value(a.name(), a);
				}
				GraphQLEnumType built = enumType.build();
				if(additionalTypes.put(built.getName(), built) != null) {
					throw new RuntimeException(built.getName() + "defined more than once");
				}
				continue;
			}
			
			SchemaOption schemaType = SchemaOption.BOTH;
			Entity graphTypeAnnotation = type.getAnnotation(Entity.class);
			if(graphTypeAnnotation != null) {
				schemaType = graphTypeAnnotation.value();
			}
			
			Builder graphType = GraphQLObjectType.newObject();
			String typeName = getName(type);
			graphType.name(typeName);
			
			
			GraphQLInterfaceType.Builder interfaceBuilder = GraphQLInterfaceType.newInterface();
			interfaceBuilder.name(typeName);
			
			GraphQLInputObjectType.Builder graphInputType = GraphQLInputObjectType.newInputObject();
			if(schemaType == SchemaOption.INPUT) {
				graphInputType.name(typeName);
			}else {
				graphInputType.name(typeName + "Input");
			}
			
			{
				GraphQLInputObjectField.Builder field = GraphQLInputObjectField.newInputObjectField();
				field.name("__typename");
				field.type(Scalars.GraphQLString);
				graphInputType.field(field);
			}
			
			
			TypeRuntimeWiring.Builder runtime = new TypeRuntimeWiring.Builder();
			runtime.typeName(typeName);
			for(Method method: type.getMethods()) {
				if(method.isSynthetic()) {
					continue;
				}
				if(method.getDeclaringClass().equals(Object.class)) {
					continue;
				}
				if(method.isAnnotationPresent(GraphQLIgnore.class)) {
					continue;
				}
				//will also be on implementing class
				if(Modifier.isAbstract(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
					continue;
				}
				if(Modifier.isStatic(method.getModifiers())) {
					continue;
				}else {
					//getter type
					if(method.getName().matches("(get|is)[A-Z].*")) {
						String name;
						if(method.getName().startsWith("get")) {
							name = method.getName().substring("get".length(), "get".length() + 1).toLowerCase() + method.getName().substring("get".length() + 1);
						}else {
							name = method.getName().substring("is".length(), "is".length() + 1).toLowerCase() + method.getName().substring("is".length() + 1);
						}
	
						GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();
						field.name(name);
						
						
						TypeMeta meta = new TypeMeta(method.getReturnType(), method.getGenericReturnType());
						field.type(getType(meta, method.getAnnotations()));
						graphType.field(field);
						interfaceBuilder.field(field);
		
						if(method.getParameterCount() > 0 || diretives.target(method, meta)) {
							codeRegistry.dataFetcher(FieldCoordinates.coordinates(typeName, name), buildDirectiveWrapper(diretives, method, meta));
						}
					}else if(method.getName().matches("set[A-Z].*")) {
						if(method.getParameterCount() == 1 && !method.isAnnotationPresent(InputIgnore.class)) {
							String name = method.getName().substring("set".length(), "set".length() + 1).toLowerCase() + method.getName().substring("set".length() + 1);
							GraphQLInputObjectField.Builder field = GraphQLInputObjectField.newInputObjectField();
							field.name(name);
							field.type(getInputType(method.getParameterTypes()[0], method.getGenericParameterTypes()[0], method.getParameterAnnotations()[0]));
							graphInputType.field(field);
						}
					}
				}
			}
			
			if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
				GraphQLInterfaceType built = interfaceBuilder.build();
				if(additionalTypes.put(built.getName(), built) != null) {
					throw new RuntimeException(built.getName() + "defined more than once");
				}
				
				codeRegistry.typeResolver(built.getName(), env -> {
					if(type.isInstance(env.getObject())) {	
						return (GraphQLObjectType) additionalTypes.get(getName(env.getObject().getClass()));
					}
					return null;
				});
				
				continue;
			}
			Class<?> parent = type.getSuperclass();
			while(parent != null) {
				if(parent.isAnnotationPresent(Entity.class)) {
					String interfaceName = getName(parent);
					graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
					break;
				}
				parent = parent.getSuperclass();
			}
			
			if(schemaType == SchemaOption.BOTH || schemaType == SchemaOption.TYPE) {
				GraphQLObjectType built = graphType.build();
				if(additionalTypes.put(built.getName(), built) != null) {
					throw new RuntimeException(built.getName() + "defined more than once");
				}
				codeRegistry.typeResolver(built.getName(), env -> {
					if(type.isInstance(env.getObject())) {	
						return built;
					}
					return null;
				});
			}
			if(schemaType == SchemaOption.BOTH || schemaType == SchemaOption.INPUT) {
				GraphQLInputObjectType inputBuild = graphInputType.build();
				if(additionalTypes.put(inputBuild.getName(), inputBuild) != null) {
					throw new RuntimeException(inputBuild.getName() + " defined more than once");
				}
			}
			
			
			
		}
		codeRegistry.typeResolver("ID", env -> {
			return null;
		});
		
		
		return GraphQL.newGraphQL(GraphQLSchema.newSchema().codeRegistry(codeRegistry.build()).additionalTypes(new HashSet<>(additionalTypes.values())).query(graphQuery.build()).mutation(graphMutations).subscription(graphSubscriptions).additionalDirective(directive).build());

	}

	private static boolean isContext(Class<?> class1) {
		return class1.isAssignableFrom(DataFetchingEnvironment.class) || class1.isAnnotationPresent(Context.class);
	}

	private static <T extends Annotation> DataFetcher<?> buildDirectiveWrapper(DirectivesSchema diretives, Method method, TypeMeta meta) {
		DataFetcher<?> fetcher = env -> {
			Object[] args = new Object[method.getParameterCount()];
			for(int i = 0; i < args.length; i++) {
				
				if(method.getParameterTypes()[i].isAssignableFrom(env.getClass())) {
					args[i] = env;
				}else if(method.getParameterTypes()[i].isAssignableFrom(env.getContext().getClass())) {
					args[i] = env.getContext();
				}else {
					Object obj = env.getArgument(method.getParameters()[i].getName());
					args[i] = obj;
				}
			}
			try {
				return method.invoke(env.getSource(), args);
			}catch (Exception e) {
				System.out.println(method);
				System.out.println((Object) env.getSource());
				System.out.println(Arrays.toString(args));
				if(e.getCause() instanceof Exception) {
					throw (Exception) e.getCause();
				}else {
					throw e;
				}
						
			}
		};
		
		fetcher = diretives.wrap(method, meta, fetcher);
		return fetcher;
		
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
						if(type.isInstance(obj) ) {
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
								args[i] = MAPPER.convertValue(obj, method.getParameters()[i].getType());	
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

	public static Class<?> extraOptionalType(Type type) {
		if(type instanceof Class) {
			return (Class<?>) type;
		}else if(type instanceof ParameterizedType){
			return extraOptionalType(((ParameterizedType) type).getActualTypeArguments()[0]);
		}
		throw new RuntimeException("extraction failure for " + type.getClass());
	}

	private static String getName(Class<?> type) {
		return type.getSimpleName();
	}

	private static GraphQLOutputType getType(TypeMeta meta, Annotation[] annotations) {
		
		
		GraphQLOutputType toReturn = getTypeInner(meta.getType(), annotations);
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
	
	
	private static GraphQLOutputType getTypeInner(Class<?> type, Annotation[] annotations) {
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
		}
		
		
		if(type.isEnum()) {
			return GraphQLTypeReference.typeRef(getName(type));
		}
		
		if(type.isAnnotationPresent(Entity.class)) {
			return GraphQLTypeReference.typeRef(getName(type));
		}
		
		if(type.isAnnotationPresent(Scalar.class)) {
			return GraphQLTypeReference.typeRef(getName(type));
		}
		
		throw new RuntimeException("Unsupport type " + type);
	}
	
	
	private static GraphQLInputType getInputType(Class<?> type, Type genericType, Annotation[] annotations) {
		
		TypeMeta meta = new TypeMeta(type, genericType);
		GraphQLInputType toReturn = getInputTypeInner(meta.getType(), annotations);
		
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
	
	private static GraphQLInputType getInputTypeInner(Class<?> type, Annotation[] annotations) {

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
		}

		
		
		
		if(type.isEnum()) {
			return GraphQLTypeReference.typeRef(getName(type));
		}
		
		if(type.isAnnotationPresent(Scalar.class)) {
			return GraphQLTypeReference.typeRef(getName(type));
		}
		
		
		if(type.isAnnotationPresent(Entity.class)) {
			if(type.getAnnotation(Entity.class).value() == SchemaOption.BOTH) {
				return GraphQLTypeReference.typeRef(getName(type) + "Input");
			}else {
				return GraphQLTypeReference.typeRef(getName(type));
			}
		}
		
		throw new RuntimeException("Unsupport type " + type);
	}

	public static graphql.GraphQL.Builder build(String... classPath) throws ReflectiveOperationException {
		
		
		Reflections reflections = new Reflections(classPath, new MethodAnnotationsScanner(), new SubTypesScanner(), new TypeAnnotationsScanner());
		
		Set<Class<? extends Authorizer>> authorizers = reflections.getSubTypesOf(Authorizer.class);
		//want to make everything split by package
		AuthorizerSchema authorizer = AuthorizerSchema.build(Sets.newHashSet(classPath), authorizers);
		
		
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
		
		return build(diretivesSchema, authorizer, types, scalars, endPoints);
	}

}

