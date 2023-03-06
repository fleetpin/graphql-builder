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
import com.fleetpin.graphql.builder.annotations.OneOf;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OneOfBuilder implements InputTypeBuilder {

	private final InputTypeBuilder map;

	public OneOfBuilder(EntityProcessor entityProcessor, Class<?> type, OneOf oneOf) {
		Map<String, InputTypeBuilder> builders = new HashMap<>();

		for (var typeOf : oneOf.value()) {
			builders.put(typeOf.name(), entityProcessor.getResolver(typeOf.type()));
		}

		map =
			(obj, context, locale) -> {
				Map<String, Object> map = (Map) obj;

				if (map.size() > 1) {
					throw new RuntimeException("OneOf must only have a single field set");
				}

				for (var entry : map.entrySet()) {
					var builder = builders.get(entry.getKey());
					return builder.convert(entry.getValue(), context, locale);
				}

				throw new RuntimeException("OneOf must only have a single field set");
			};
	}

	@Override
	public Object convert(Object obj, GraphQLContext graphQLContext, Locale locale) {
		return map.convert(obj, graphQLContext, locale);
	}
}
