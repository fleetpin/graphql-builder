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

import graphql.GraphQLContext;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class ConstructorFieldBuilder implements InputTypeBuilder {

	private final InputTypeBuilder map;

	public ConstructorFieldBuilder(Class<?> type, ArrayList<RecordMapper> mappers) {
		try {
			var argTypes = mappers.stream().map(t -> t.type).toArray(Class[]::new);
			var constructor = type.getDeclaredConstructor(argTypes);
			constructor.setAccessible(true);
			map =
				(obj, context, locale) -> {
					try {
						Map map = (Map) obj;

						var args = new Object[argTypes.length];

						for (int i = 0; i < args.length; i++) {
							var mapper = mappers.get(i);
							args[i] = mapper.resolver.convert(map.get(mapper.name), context, locale);
						}

						return constructor.newInstance(args);
					} catch (ReflectiveOperationException e) {
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

	public static class RecordMapper {

		private final String name;
		private final Class<?> type;
		private final InputTypeBuilder resolver;

		public RecordMapper(String name, Class<?> type, InputTypeBuilder resolver) {
			this.name = name;
			this.type = type;
			this.resolver = resolver;
		}
	}
}
