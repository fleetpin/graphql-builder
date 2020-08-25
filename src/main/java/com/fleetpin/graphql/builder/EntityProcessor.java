package com.fleetpin.graphql.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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

	private void addType(TypeMeta meta, boolean input) {
		Class<?> type = meta.getType();
		Type genericType = meta.getGenericType();
		if(genericType == null) {
			genericType = type;
		}
		try {
			if(type.isAnnotationPresent(Scalar.class)) {
				GraphQLScalarType.Builder scalarType = GraphQLScalarType.newScalar();
				String typeName = getName(meta);
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
					String typeName = getName(meta);
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
				String typeName = getName(meta);
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
							if(!input && method.getName().matches("(get|is)[A-Z].*")) {
								String name;
								if(method.getName().startsWith("get")) {
									name = method.getName().substring("get".length(), "get".length() + 1).toLowerCase() + method.getName().substring("get".length() + 1);
								}else {
									name = method.getName().substring("is".length(), "is".length() + 1).toLowerCase() + method.getName().substring("is".length() + 1);
								}

								GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();
								field.name(name);


								TypeMeta innerMeta = new TypeMeta(this, meta, method.getReturnType(), method.getGenericReturnType());
								field.type(SchemaBuilder.getType(innerMeta, method.getAnnotations()));
								graphType.field(field);
								interfaceBuilder.field(field);

								if(method.getParameterCount() > 0 || directives.target(method, innerMeta)) {
									codeRegistry.dataFetcher(FieldCoordinates.coordinates(typeName, name), buildDirectiveWrapper(directives, method, innerMeta));
								}
							}else if(input && method.getName().matches("set[A-Z].*")) {
								if(method.getParameterCount() == 1 && !method.isAnnotationPresent(InputIgnore.class)) {
									String name = method.getName().substring("set".length(), "set".length() + 1).toLowerCase() + method.getName().substring("set".length() + 1);
									GraphQLInputObjectField.Builder field = GraphQLInputObjectField.newInputObjectField();
									field.name(name);
									TypeMeta innerMeta = new TypeMeta(this, meta, method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);
									field.type(SchemaBuilder.getInputType(innerMeta, method.getParameterAnnotations()[0]));
									graphInputType.field(field);
								}
							}
						}
					}catch(RuntimeException e) {
						e.printStackTrace();
						throw new RuntimeException("Failed to process method " + method, e);
					}
				}

				if(!input && (type.isInterface() || Modifier.isAbstract(type.getModifiers()) || meta.hasUnmappedGeneric())) {
					GraphQLInterfaceType built = interfaceBuilder.build();
					if(additionalTypes.put(built.getName(), built) != null) {
						throw new RuntimeException(built.getName() + "defined more than once");
					}
					
					codeRegistry.typeResolver(built.getName(), env -> {
						if(type.isInstance(env.getObject())) {	
							
							TypeMeta innerMeta = new TypeMeta(this, null, env.getObject().getClass(), env.getObject().getClass());
							try {
							return (GraphQLObjectType) additionalTypes.get(typeNameLookup(env.getObject()));
							}catch (ClassCastException e) {
								throw e;
							}
						}
						return null;
					});
					return;
				}
				Class<?> parent = type.getSuperclass();
				while(!input && parent != null) {
					if(parent.isAnnotationPresent(Entity.class)) {
						TypeMeta innerMeta = new TypeMeta(this, meta, parent, type.getGenericSuperclass());
						String interfaceName = process(innerMeta);
						graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
						
						if(!parent.equals(type.getGenericSuperclass())) {
							innerMeta = new TypeMeta(this, meta, parent, parent);
							interfaceName = process(innerMeta);
							graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
						}
						
						var genericMeta = new TypeMeta(this, null, parent, parent);
						if(!getName(innerMeta).equals(getName(genericMeta))) {
							interfaceName = process(genericMeta);
							graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
						}
						
					}
					parent = parent.getSuperclass();
				}
				//generics
				if(!input) {				
					TypeMeta innerMeta = new TypeMeta(this, meta, type, type);
					if(!getName(innerMeta).equals(typeName)) {
						String interfaceName = process(innerMeta);
						graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
					}
					innerMeta = new TypeMeta(this, null, type, type);
					if(!getName(innerMeta).equals(typeName)) {
						String interfaceName = process(innerMeta);
						graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName));
					}
				}

				if(!input && (schemaType == SchemaOption.BOTH || schemaType == SchemaOption.TYPE)) {
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
				if(input && (schemaType == SchemaOption.BOTH || schemaType == SchemaOption.INPUT)) {
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

	
	private String getName(TypeMeta meta) {
		var type = meta.getType();

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
		var genericType = meta.getGenericType();

		for(int i = 0; i < type.getTypeParameters().length; i++) {
			if(genericType instanceof ParameterizedType) {
				var t = ((ParameterizedType) genericType).getActualTypeArguments()[i];
				if(t instanceof Class) {
					String extra = ((Class) t).getSimpleName();
					name += "_" + extra;
					
				}else if(t instanceof TypeVariable){
					var variable = (TypeVariable) t;
					Class extra = meta.resolveToType(variable);
					if(extra != null) {
						name += "_" + extra.getSimpleName();
					}
				}
			}else {
				Class extra = meta.resolveToType(type.getTypeParameters()[i]);
				if(extra != null) {
					name += "_" + extra.getSimpleName();
				}
			}
		}
		return name;
	}

	private String typeNameLookup(Object obj) {
		var type = obj.getClass();
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
		
		for(var t: type.getTypeParameters()) {
			t.getTypeName();
			for(var method: type.getMethods()) {
				var methodType = method.getGenericReturnType();
				if(methodType instanceof TypeVariable) {
					var typeVariable = ((TypeVariable) methodType);
					if(typeVariable.equals(t)) {
						//maybe we should dig through private fields first
						try {
							name += "_" + typeNameLookup(method.invoke(obj));
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException("Could not infre type with regard to generics.");
						} //TODO: might have arguments might be a future which would make impossible to resolve
					}
				}
			}
		}
		
		return name;
	}
	
	public String process(TypeMeta meta) {
		
		TypeMeta rawMeta = new TypeMeta(this, null,meta.getType(), meta.getType());
		String rawName = getName(rawMeta);

		if(rawName != null && !this.additionalTypes.containsKey(rawName)) {
			this.additionalTypes.put(rawName, null); // so we don't go around in circles if depend on self
			addType(rawMeta, false);
		}
		
		String name = getName(meta);
		if(name != null && !this.additionalTypes.containsKey(name)) {
			this.additionalTypes.put(name, null); // so we don't go around in circles if depend on self
			addType(meta, false);
		}
		return name;
	}

	private String getNameInput(TypeMeta meta) {
		var type = meta.getType();
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
		
		var genericType = meta.getGenericType();
		
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


	public String processInput(TypeMeta meta) {
		String name = getNameInput(meta);
		if(name != null && !this.additionalTypes.containsKey(name)) {
			this.additionalTypes.put(name, null); // so we don't go around in circles if depend on self
			addType(meta, true);
		}
		return name;
	}

}
