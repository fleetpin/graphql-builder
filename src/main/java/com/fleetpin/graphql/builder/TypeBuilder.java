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

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.GraphQLDeprecated;
import com.fleetpin.graphql.builder.annotations.GraphQLDescription;
import com.fleetpin.graphql.builder.annotations.GraphQLIgnore;
import com.fleetpin.graphql.builder.annotations.Scalar;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

public abstract class TypeBuilder {

	protected final EntityProcessor entityProcessor;
	protected final TypeMeta meta;

	public TypeBuilder(EntityProcessor entityProcessor, TypeMeta meta) {
		this.entityProcessor = entityProcessor;
		this.meta = meta;
	}

	public GraphQLNamedOutputType buildType() throws ReflectiveOperationException {
		Builder graphType = GraphQLObjectType.newObject();
		String typeName = EntityUtil.getName(meta);
		graphType.name(typeName);

		GraphQLInterfaceType.Builder interfaceBuilder = GraphQLInterfaceType.newInterface();
		interfaceBuilder.name(typeName);
		var type = meta.getType();
		{
			var description = type.getAnnotation(GraphQLDescription.class);
			if (description != null) {
				graphType.description(description.value());
				interfaceBuilder.description(description.value());
			}
		}

		processFields(typeName, graphType, interfaceBuilder);

		boolean unmappedGenerics = meta.hasUnmappedGeneric();

		if (unmappedGenerics) {
			var name = EntityUtil.getName(meta.notDirect());

			graphType.withInterface(GraphQLTypeReference.typeRef(name));
			if (meta.isDirect()) {
				interfaceBuilder.withInterface(GraphQLTypeReference.typeRef(name));
			}
		}
		Class<?> parent = type.getSuperclass();
		while (parent != null) {
			if (parent.isAnnotationPresent(Entity.class)) {
				TypeMeta innerMeta = new TypeMeta(meta, parent, type.getGenericSuperclass());
				GraphQLInterfaceType interfaceName = (GraphQLInterfaceType) entityProcessor.getEntity(innerMeta).getInnerType(innerMeta);
				addInterface(graphType, interfaceBuilder, interfaceName);

				if (!parent.equals(type.getGenericSuperclass())) {
					innerMeta = new TypeMeta(meta, parent, parent);
					interfaceName = (GraphQLInterfaceType) entityProcessor.getEntity(innerMeta).getInnerType(innerMeta);
					addInterface(graphType, interfaceBuilder, interfaceName);
				}

				var genericMeta = new TypeMeta(null, parent, parent);
				if (!EntityUtil.getName(innerMeta).equals(EntityUtil.getName(genericMeta))) {
					interfaceName = (GraphQLInterfaceType) entityProcessor.getEntity(genericMeta).getInnerType(genericMeta);
					addInterface(graphType, interfaceBuilder, interfaceName);
				}
			}
			parent = parent.getSuperclass();
		}
		//generics
		TypeMeta innerMeta = new TypeMeta(meta, type, type);
		if (!EntityUtil.getName(innerMeta).equals(typeName)) {
			var interfaceName = entityProcessor.getEntity(innerMeta).getInnerType(innerMeta);
			graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName.getName()));
			interfaceBuilder.withInterface(GraphQLTypeReference.typeRef(interfaceName.getName()));
		}
		innerMeta = new TypeMeta(null, type, type);
		if (!EntityUtil.getName(innerMeta).equals(typeName)) {
			var interfaceName = entityProcessor.getEntity(innerMeta).getInnerType(innerMeta);
			graphType.withInterface(GraphQLTypeReference.typeRef(interfaceName.getName()));
			interfaceBuilder.withInterface(GraphQLTypeReference.typeRef(interfaceName.getName()));
		}

		boolean interfaceable = type.isInterface() || Modifier.isAbstract(type.getModifiers());
		if (!meta.isDirect() && (interfaceable || unmappedGenerics)) {
			entityProcessor.addSchemaDirective(type, type, interfaceBuilder::withAppliedDirective);
			GraphQLInterfaceType built = interfaceBuilder.build();

			entityProcessor
				.getCodeRegistry()
				.typeResolver(
					built.getName(),
					env -> {
						if (type.isInstance(env.getObject())) {
							var meta = new TypeMeta(null, env.getObject().getClass(), env.getObject().getClass());
							var t = entityProcessor.getEntity(meta).getInnerType(null);
							if (!(t instanceof GraphQLObjectType)) {
								t = entityProcessor.getEntity(meta.direct()).getInnerType(null);
							}
							try {
								return (GraphQLObjectType) t;
							} catch (ClassCastException e) {
								throw e;
							}
						}
						return null;
					}
				);

			if (unmappedGenerics && !meta.isDirect()) {
				var directType = meta.direct();
				entityProcessor.getEntity(directType).getInnerType(directType);
			}
			return built;
		}

		entityProcessor.addSchemaDirective(type, type, graphType::withAppliedDirective);
		var built = graphType.build();
		entityProcessor
			.getCodeRegistry()
			.typeResolver(
				built.getName(),
				env -> {
					if (type.isInstance(env.getObject())) {
						return built;
					}
					return null;
				}
			);
		return built;
	}

	private void addInterface(Builder graphType, GraphQLInterfaceType.Builder interfaceBuilder, GraphQLInterfaceType interfaceName) {
		graphType.withInterface(interfaceName);
		for (var inner : interfaceName.getInterfaces()) {
			graphType.withInterface(GraphQLTypeReference.typeRef(inner.getName()));
			interfaceBuilder.withInterface(GraphQLTypeReference.typeRef(inner.getName()));
		}
		interfaceBuilder.withInterface(interfaceName);
	}

	protected abstract void processFields(String typeName, Builder graphType, graphql.schema.GraphQLInterfaceType.Builder interfaceBuilder)
		throws ReflectiveOperationException;

	private static <T extends Annotation> DataFetcher<?> buildDirectiveWrapper(DirectivesSchema diretives, Method method, TypeMeta meta) {
		DataFetcher<?> fetcher = env -> {
			Object[] args = new Object[method.getParameterCount()];
			for (int i = 0; i < args.length; i++) {
				if (method.getParameterTypes()[i].isAssignableFrom(env.getClass())) {
					args[i] = env;
				} else if (method.getParameterTypes()[i].isAssignableFrom(env.getContext().getClass())) {
					args[i] = env.getContext();
				} else {
					Object obj = env.getArgument(method.getParameters()[i].getName());
					args[i] = obj;
				}
			}
			try {
				return method.invoke(env.getSource(), args);
			} catch (Exception e) {
				if (e.getCause() instanceof Exception) {
					throw (Exception) e.getCause();
				} else {
					throw e;
				}
			}
		};

		fetcher = diretives.wrap(method, meta, fetcher);
		return fetcher;
	}

	private String typeNameLookup(Object obj) {
		var type = obj.getClass();
		String name = null;

		if (type.isEnum()) {
			name = type.getSimpleName();
		}
		if (type.isAnnotationPresent(Scalar.class)) {
			name = type.getSimpleName();
		}
		if (type.isAnnotationPresent(Entity.class)) {
			name = type.getSimpleName();
		}

		for (var t : type.getTypeParameters()) {
			for (var method : type.getMethods()) {
				var methodType = method.getGenericReturnType();
				if (methodType instanceof TypeVariable) {
					var typeVariable = ((TypeVariable) methodType);
					if (typeVariable.equals(t)) {
						//maybe we should dig through private fields first
						try {
							name += "_" + typeNameLookup(method.invoke(obj));
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException("Could not infer type with regard to generics.");
						} //TODO: might have arguments might be a future which would make impossible to resolve
					}
				}
			}
		}

		return name;
	}

	public static class ObjectType extends TypeBuilder {

		public ObjectType(EntityProcessor entityProcessor, TypeMeta meta) {
			super(entityProcessor, meta);
		}

		@Override
		protected void processFields(String typeName, Builder graphType, graphql.schema.GraphQLInterfaceType.Builder interfaceBuilder)
			throws ReflectiveOperationException {
			var type = meta.getType();
			for (Method method : type.getMethods()) {
				try {
					var name = EntityUtil.getter(method);
					if (name.isEmpty()) {
						continue;
					}

					GraphQLFieldDefinition.Builder field = GraphQLFieldDefinition.newFieldDefinition();
					field.name(name.get());
					entityProcessor.addSchemaDirective(method, type, field::withAppliedDirective);
					var deprecated = method.getAnnotation(GraphQLDeprecated.class);
					if (deprecated != null) {
						field.deprecate(deprecated.value());
					}
					var description = method.getAnnotation(GraphQLDescription.class);
					if (description != null) {
						field.description(description.value());
					}

					TypeMeta innerMeta = new TypeMeta(meta, method.getReturnType(), method.getGenericReturnType());

					field.type(entityProcessor.getType(innerMeta, method.getAnnotations()));
					graphType.field(field);
					interfaceBuilder.field(field);

					var directives = entityProcessor.getDirectives();
					var codeRegistry = entityProcessor.getCodeRegistry();

					if (method.getParameterCount() > 0 || directives.target(method, innerMeta)) {
						codeRegistry.dataFetcher(FieldCoordinates.coordinates(typeName, name.get()), buildDirectiveWrapper(directives, method, innerMeta));
					}
				} catch (RuntimeException e) {
					throw new RuntimeException("Failed to process method " + method, e);
				}
			}
		}
	}

	public static class Record extends TypeBuilder {

		public Record(EntityProcessor entityProcessor, TypeMeta meta) {
			super(entityProcessor, meta);
		}

		@Override
		protected void processFields(String typeName, Builder graphType, graphql.schema.GraphQLInterfaceType.Builder interfaceBuilder)
			throws ReflectiveOperationException {
			var type = meta.getType();

			for (var field : type.getDeclaredFields()) {
				try {
					if (field.isSynthetic()) {
						continue;
					}
					if (field.getDeclaringClass().equals(Object.class)) {
						continue;
					}
					if (field.isAnnotationPresent(GraphQLIgnore.class)) {
						continue;
					}
					//will also be on implementing class
					if (Modifier.isAbstract(field.getModifiers()) || field.getDeclaringClass().isInterface()) {
						continue;
					}
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					} else {
						var method = type.getMethod(field.getName());
						if (method.isAnnotationPresent(GraphQLIgnore.class)) {
							continue;
						}
						//getter type
						String name = field.getName();

						GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition();
						fieldBuilder.name(name);
						entityProcessor.addSchemaDirective(method, type, fieldBuilder::withAppliedDirective);
						var deprecated = field.getAnnotation(GraphQLDeprecated.class);
						if (deprecated != null) {
							fieldBuilder.deprecate(deprecated.value());
						}
						var description = field.getAnnotation(GraphQLDescription.class);
						if (description != null) {
							fieldBuilder.description(description.value());
						}

						TypeMeta innerMeta = new TypeMeta(meta, method.getReturnType(), method.getGenericReturnType());
						fieldBuilder.type(entityProcessor.getType(innerMeta, method.getAnnotations()));
						graphType.field(fieldBuilder);
						interfaceBuilder.field(fieldBuilder);

						var directives = entityProcessor.getDirectives();

						if (method.getParameterCount() > 0 || directives.target(method, innerMeta)) {
							entityProcessor
								.getCodeRegistry()
								.dataFetcher(FieldCoordinates.coordinates(typeName, name), buildDirectiveWrapper(directives, method, innerMeta));
						}
					}
				} catch (RuntimeException e) {
					throw new RuntimeException("Failed to process method " + field, e);
				}
			}
		}
	}
}
