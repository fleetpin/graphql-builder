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

import com.fleetpin.graphql.builder.annotations.GraphQLIgnore;
import com.fleetpin.graphql.builder.annotations.InputIgnore;
import com.fleetpin.graphql.builder.mapper.ObjectFieldBuilder.FieldMapper;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

public class EntityUtil {

	private static final Method IS_RECORD_METHOD;

	static {
		Class<? extends Class> classClass = Class.class;
		Method isRecord;
		try {
			isRecord = classClass.getMethod("isRecord");
		} catch (NoSuchMethodException e) {
			isRecord = null;
		}
		IS_RECORD_METHOD = isRecord;
	}

	static boolean isRecord(Class<?> type) {
		if (IS_RECORD_METHOD == null) {
			return false;
		}
		try {
			return (Boolean) IS_RECORD_METHOD.invoke(type);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	static String getName(TypeMeta meta) {
		var type = meta.getType();

		String name = type.getSimpleName();

		var genericType = meta.getGenericType();

		for (int i = 0; i < type.getTypeParameters().length; i++) {
			if (genericType instanceof ParameterizedType) {
				var t = ((ParameterizedType) genericType).getActualTypeArguments()[i];
				if (t instanceof Class) {
					String extra = ((Class) t).getSimpleName();
					name += "_" + extra;
				} else if (t instanceof TypeVariable) {
					var variable = (TypeVariable) t;
					Class extra = meta.resolveToType(variable);
					if (extra != null) {
						name += "_" + extra.getSimpleName();
					}
				}
			} else {
				Class extra = meta.resolveToType(type.getTypeParameters()[i]);
				if (extra != null) {
					name += "_" + extra.getSimpleName();
				}
			}
		}
		if (meta.isDirect()) {
			name += "_DIRECT";
		}

		return name;
	}

	public static Optional<String> getter(Method method) {
		if (method.isSynthetic()) {
			return Optional.empty();
		}
		if (method.getDeclaringClass().equals(Object.class)) {
			return Optional.empty();
		}
		if (method.isAnnotationPresent(GraphQLIgnore.class)) {
			return Optional.empty();
		}
		//will also be on implementing class
		if (Modifier.isAbstract(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
			return Optional.empty();
		}
		if (Modifier.isStatic(method.getModifiers())) {
			return Optional.empty();
		} else {
			if (method.getName().matches("(get|is)[A-Z].*")) {
				String name;
				if (method.getName().startsWith("get")) {
					name = method.getName().substring("get".length(), "get".length() + 1).toLowerCase() + method.getName().substring("get".length() + 1);
				} else {
					name = method.getName().substring("is".length(), "is".length() + 1).toLowerCase() + method.getName().substring("is".length() + 1);
				}
				return Optional.of(name);
			}
		}
		return Optional.empty();
	}

	public static Optional<String> setter(Method method) {
		if (method.isSynthetic()) {
			return Optional.empty();
		}
		if (method.getDeclaringClass().equals(Object.class)) {
			return Optional.empty();
		}
		if (method.isAnnotationPresent(GraphQLIgnore.class)) {
			return Optional.empty();
		}
		//will also be on implementing class
		if (Modifier.isAbstract(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
			return Optional.empty();
		}
		if (Modifier.isStatic(method.getModifiers())) {
			return Optional.empty();
		} else {
			//getter type
			if (method.getName().matches("set[A-Z].*")) {
				if (method.getParameterCount() == 1 && !method.isAnnotationPresent(InputIgnore.class)) {
					String name = method.getName().substring("set".length(), "set".length() + 1).toLowerCase() + method.getName().substring("set".length() + 1);
					return Optional.of(name);
				}
			}
		}
		return Optional.empty();
	}
}
