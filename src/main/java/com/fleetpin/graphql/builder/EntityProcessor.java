package com.fleetpin.graphql.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.GraphQLIgnore;
import com.fleetpin.graphql.builder.annotations.InputIgnore;
import com.fleetpin.graphql.builder.annotations.Scalar;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

import graphql.schema.idl.TypeRuntimeWiring;
import graphql.Scalars;
import graphql.schema.Coercing;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

public class EntityProcessor {

	private final Map<String, GraphQLType> additionalTypes;
	private final GraphQLCodeRegistry.Builder codeRegistry;
	private final DirectivesSchema directives;


	public EntityProcessor(Map<String, GraphQLType> additionalTypes, GraphQLCodeRegistry.Builder codeRegistry, DirectivesSchema diretives) {
		this.additionalTypes = additionalTypes;
		this.codeRegistry = codeRegistry;
		this.directives = diretives;
	}
	
	
	public static Class<?> extraOptionalType(Type type) {
		if(type instanceof Class) {
			return (Class<?>) type;
		}else if(type instanceof ParameterizedType){
			return extraOptionalType(((ParameterizedType) type).getActualTypeArguments()[0]);
		}
		throw new RuntimeException("extraction failure for " + type.getClass());
	}

	private void addType(Class<?> type, Type genericType) {
		if(genericType == null) {
			genericType = type;
		}
		try {
			if(type.isAnnotationPresent(Scalar.class)) {
				GraphQLScalarType.Builder scalarType = GraphQLScalarType.newScalar();
				String typeName = getName(type, genericType);
				scalarType.name(typeName);
				Class<? extends Coercing> coerecing = type.getAnnotation(Scalar.class).value();
				scalarType.coercing(coerecing.getDeclaredConstructor().newInstance());
				var built = scalarType.build();
				if(additionalTypes.put(built.getName(), built) != null) {
					throw new RuntimeException(built.getName() + "defined more than once");
				}
			}
			
			if(type.isAnnotationPresent(Entity.class)) {
				//special handling
				if(type.isEnum()) {
					graphql.schema.GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum();
					String typeName = getName(type, genericType);
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
					return;
				}

				SchemaOption schemaType = SchemaOption.BOTH;
				Entity graphTypeAnnotation = type.getAnnotation(Entity.class);
				if(graphTypeAnnotation != null) {
					schemaType = graphTypeAnnotation.value();
				}

				Builder graphType = GraphQLObjectType.newObject();
				String typeName = getName(type, genericType);
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
					try {
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


								TypeMeta meta = new TypeMeta(this, type, method.getReturnType(), method.getGenericReturnType());
								field.type(SchemaBuilder.getType(meta, method.getAnnotations()));
								graphType.field(field);
								interfaceBuilder.field(field);

								if(method.getParameterCount() > 0 || directives.target(method, meta)) {
									codeRegistry.dataFetcher(FieldCoordinates.coordinates(typeName, name), buildDirectiveWrapper(directives, method, meta));
								}
							}else if(method.getName().matches("set[A-Z].*")) {
								if(method.getParameterCount() == 1 && !method.isAnnotationPresent(InputIgnore.class)) {
									String name = method.getName().substring("set".length(), "set".length() + 1).toLowerCase() + method.getName().substring("set".length() + 1);
									GraphQLInputObjectField.Builder field = GraphQLInputObjectField.newInputObjectField();
									field.name(name);
									TypeMeta meta = new TypeMeta(this, genericType, method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);
									field.type(SchemaBuilder.getInputType(meta, method.getParameterAnnotations()[0]));
									graphInputType.field(field);
								}
							}
						}
					}catch(RuntimeException e) {
						throw new RuntimeException("Failed to process method " + method, e);
					}
				}

				if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
					GraphQLInterfaceType built = interfaceBuilder.build();
					if(additionalTypes.put(built.getName(), built) != null) {
						throw new RuntimeException(built.getName() + "defined more than once");
					}

					codeRegistry.typeResolver(built.getName(), env -> {
						if(type.isInstance(env.getObject())) {	
							return (GraphQLObjectType) additionalTypes.get(getName(env.getObject().getClass(), env.getObject().getClass()));
						}
						return null;
					});
					return;
				}
				Class<?> parent = type.getSuperclass();
				while(parent != null) {
					if(parent.isAnnotationPresent(Entity.class)) {
						String interfaceName = process(parent, type.getGenericSuperclass());
						graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
						
						if(!parent.equals(type.getGenericSuperclass())) {
							interfaceName = process(parent, parent);
							graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
						}
						
					}
					parent = parent.getSuperclass();
				}
				
				if(!type.equals(genericType)) {
					String interfaceName = process(type, type);
					graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
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
		}catch (ReflectiveOperationException | RuntimeException e) {
			throw new RuntimeException("Failed to build schema for class " + type, e);
		}
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

	
	private String getName(Class<?> type, Type genericType) {

		String name = null;

		if(type.isEnum()) {
			name = type.getSimpleName();
		}
		if(type.isAnnotationPresent(Scalar.class)) {
			name = type.getSimpleName();
		}
		if(type.isAnnotationPresent(Entity.class)) {
			name = type.getSimpleName();
		}
		
		if(genericType instanceof ParameterizedType) {
			var parameterizedTypes = ((ParameterizedType) genericType).getActualTypeArguments();
			
			for(var t: parameterizedTypes) {
				if(t instanceof Class) {
					String extra = ((Class) t).getSimpleName();
					name += "_" + extra;
					
				}	
			}
		}
		
		return name;
	}

	public String process(Class<?> type, Type genericType) {
		String name = getName(type, genericType);
				if(name != null && !this.additionalTypes.containsKey(name)) {
			addType(type, genericType);
		}
		return name;
	}

	private String getNameInput(Class<?> type, Type genericType) {
		
		String name = null;
		if(type.isEnum()) {
			name = type.getSimpleName();
		}
		
		if(type.isAnnotationPresent(Scalar.class)) {
			name = type.getSimpleName();
		}
		
		if(type.isAnnotationPresent(Entity.class)) {
			if(type.getAnnotation(Entity.class).value() == SchemaOption.BOTH) {
				name = type.getSimpleName() + "Input";
			}else {
				name = type.getSimpleName();
			}
		}
		
		if(genericType instanceof ParameterizedType) {
			var parameterizedTypes = ((ParameterizedType) genericType).getActualTypeArguments();
			
			for(var t: parameterizedTypes) {
				if(t instanceof Class) {
					String extra = ((Class) t).getSimpleName();
					name += "_" + extra;
					
				}	
			}
		}
		
		return name;
	}


	public String processInput(Class<?> type, Type genericType) {
		String name = getNameInput(type, genericType);
		if(name != null && !this.additionalTypes.containsKey(name)) {
			addType(type, genericType);
		}
		return name;
	}

}
