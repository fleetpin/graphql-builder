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

import com.fleetpin.graphql.builder.TypeMeta.Flag;
import com.fleetpin.graphql.builder.annotations.Id;
import com.fleetpin.graphql.builder.annotations.Union;
import com.fleetpin.graphql.builder.mapper.InputTypeBuilder;
import graphql.Scalars;
import graphql.com.google.common.collect.Sets;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedInputType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class EntityHolder {

	private GraphQLNamedOutputType type;
	private GraphQLNamedInputType inputType;
	private InputTypeBuilder resolver;

	final GraphQLOutputType getType(TypeMeta meta, Annotation[] annotations) {
		if (type == null) {
			type = new GraphQLTypeReference(EntityUtil.getName(meta));
			type = buildType();
		}
		GraphQLOutputType toReturn = getTypeInner(annotations);
		return toType(meta, toReturn);
	}

	private GraphQLOutputType toType(TypeMeta meta, GraphQLOutputType toReturn) {
		boolean required = true;
		for (var flag : meta.getFlags()) {
			if (flag == Flag.OPTIONAL) {
				required = false;
			}
			if (flag == Flag.ARRAY) {
				if (required) {
					toReturn = GraphQLNonNull.nonNull(toReturn);
				}
				toReturn = GraphQLList.list(toReturn);
				required = true;
			}
		}
		if (required) {
			toReturn = GraphQLNonNull.nonNull(toReturn);
		}
		return toReturn;
	}

	public final GraphQLNamedOutputType getInnerType(TypeMeta meta) {
		if (type == null) {
			type = new GraphQLTypeReference(EntityUtil.getName(meta));
			type = buildType();
		}
		return type;
	}

	private GraphQLNamedOutputType getTypeInner(Annotation[] annotations) {
		if (annotations == null) {
			return type;
		}
		for (Annotation an : annotations) {
			if (an.annotationType().equals(Id.class)) {
				return Scalars.GraphQLID;
			}
		}
		return type;
	}

	protected abstract GraphQLNamedOutputType buildType();

	public final GraphQLInputType getInputType(TypeMeta meta, Annotation[] annotations) {
		if (inputType == null) {
			inputType = new GraphQLTypeReference(buildInputName());
			inputType = buildInput();
		}
		GraphQLInputType toReturn = getInputTypeInner(annotations);

		boolean required = true;
		for (var flag : meta.getFlags()) {
			if (flag == Flag.OPTIONAL) {
				required = false;
			}
			if (flag == Flag.ARRAY) {
				if (required) {
					toReturn = GraphQLNonNull.nonNull(toReturn);
				}
				toReturn = GraphQLList.list(toReturn);
				required = true;
			}
		}
		if (required) {
			toReturn = GraphQLNonNull.nonNull(toReturn);
		}
		return toReturn;
	}

	private GraphQLInputType getInputTypeInner(Annotation[] annotations) {
		for (Annotation an : annotations) {
			if (an.annotationType().equals(Id.class)) {
				return Scalars.GraphQLID;
			}
		}
		return inputType;
	}

	protected abstract GraphQLNamedInputType buildInput();

	protected abstract String buildInputName();

	public Stream<GraphQLNamedType> types() {
		List<GraphQLNamedType> types = new ArrayList<>(2);
		if (type != null) {
			types.add(type);
		}
		if (inputType != null && inputType != type) {
			types.add(inputType);
		}
		return types.stream();
	}

	protected abstract InputTypeBuilder buildResolver();

	public final InputTypeBuilder getResolver(TypeMeta meta) {
		if (resolver == null) {
			resolver = resolverPointer();
			resolver = buildResolver();
		}
		return process(meta.getTypes().iterator(), resolver);
	}

	private InputTypeBuilder resolverPointer() {
		return (obj, graphQLContext, locale) -> this.resolver.convert(obj, graphQLContext, locale);
	}

	private static InputTypeBuilder process(Iterator<Class<?>> iterator, InputTypeBuilder resolver) {
		if (iterator.hasNext()) {
			var type = iterator.next();

			if (List.class.isAssignableFrom(type)) {
				return processCollection(ArrayList::new, iterator, resolver);
			}

			if (Set.class.isAssignableFrom(type)) {
				return processCollection(size -> new LinkedHashSet<>((int) (size / 0.75 + 1)), iterator, resolver);
			}

			if (Optional.class.isAssignableFrom(type)) {
				return processOptional(iterator, resolver);
			}
			if (type.isArray()) {
				return processArray(type, iterator, resolver);
			}

			if (iterator.hasNext()) {
				throw new RuntimeException("Unsupported type " + type);
			}

			if (type.isEnum()) {
				return processEnum((Class<? extends Enum>) type);
			}
			return resolver;
		}
		throw new RuntimeException("No type");
	}

	private static InputTypeBuilder processEnum(Class<? extends Enum> type) {
		var constants = type.getEnumConstants();
		var map = new HashMap<String, Enum>();
		for (var c : constants) {
			map.put(c.name(), c);
		}

		return (obj, context, locale) -> {
			if (type.isInstance(obj)) {
				return obj;
			}
			return map.get(obj);
		};
	}

	private static InputTypeBuilder processOptional(Iterator<Class<?>> iterator, InputTypeBuilder resolver) {
		var mapper = process(iterator, resolver);
		return (obj, context, locale) -> {
			if (obj instanceof Optional) {
				if (((Optional) obj).isEmpty()) {
					return obj;
				} else {
					obj = ((Optional) obj).get();
				}
			}
			if (obj == null) {
				return Optional.empty();
			}
			return Optional.of(mapper.convert(obj, context, locale));
		};
	}

	private static InputTypeBuilder processArray(Class<?> type, Iterator<Class<?>> iterator, InputTypeBuilder resolver) {
		var component = type.getComponentType();
		if (component.isPrimitive()) {
			throw new RuntimeException("Do not support primitive array");
		}

		var mapper = process(iterator, resolver);
		return (obj, context, locale) -> {
			if (obj instanceof Collection) {
				var collection = (Collection) obj;
				Object[] toReturn = (Object[]) Array.newInstance(component, collection.size());
				int i = 0;
				for (var c : collection) {
					toReturn[i] = mapper.convert(c, context, locale);
				}
				return toReturn;
			} else {
				throw new RuntimeException("Expected a Collection got " + obj.getClass());
			}
		};
	}

	private static InputTypeBuilder processCollection(Function<Integer, Collection> create, Iterator<Class<?>> iterator, InputTypeBuilder resolver) {
		var mapper = process(iterator, resolver);
		return (obj, context, locale) -> {
			if (obj instanceof Collection) {
				var collection = (Collection) obj;
				var toReturn = create.apply(collection.size());
				for (var c : collection) {
					toReturn.add(mapper.convert(c, context, locale));
				}
				return toReturn;
			} else {
				throw new RuntimeException("Expected a Collection got " + obj.getClass());
			}
		};
	}
}
