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

package com.fleetpin.graphql.builder.mapper;

import com.fleetpin.graphql.builder.EntityProcessor;
import com.fleetpin.graphql.builder.TypeMeta;
import graphql.GraphQLContext;
import graphql.com.google.common.base.Throwables;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class ObjectFieldBuilder implements InputTypeBuilder {

	private final InputTypeBuilder map;

	public ObjectFieldBuilder(Class<?> type, ArrayList<FieldMapper> mappers) {
		try {
			var constructor = type.getConstructor();
			map =
				(obj, context, locale) -> {
					try {
						var toReturn = constructor.newInstance();

						Map map = (Map) obj;

						for (var mapper : mappers) {
							var name = mapper.getName();
							if (map.containsKey(name)) {
								mapper.map(toReturn, map.get(name), context, locale);
							}
						}

						return toReturn;
					} catch (Throwable e) {
						Throwables.throwIfUnchecked(e);
						throw new RuntimeException(e);
					}
				};
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object convert(Object obj, GraphQLContext graphQLContext, Locale locale) {
		return map.convert(obj, graphQLContext, locale);
	}

	public static class FieldMapper {

		private final Method method;
		private final String name;
		private final InputTypeBuilder mapper;

		private FieldMapper(String name, Method method, InputTypeBuilder objectBuilder) {
			this.name = name;
			this.method = method;
			this.mapper = objectBuilder;
		}

		public String getName() {
			return name;
		}

		protected void map(Object inputType, Object argument, GraphQLContext graphQLContext, Locale locale) throws Throwable {
			try {
				method.invoke(inputType, mapper.convert(argument, graphQLContext, locale));
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		public static FieldMapper build(EntityProcessor entityProcessor, TypeMeta inputType, String name, Method method) {
			return new FieldMapper(name, method, entityProcessor.getResolver(inputType));
		}
	}
}
