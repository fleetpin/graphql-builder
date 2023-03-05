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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.schema.GraphQLObjectType;
import org.junit.jupiter.api.Test;

public class MetaTest {

	@Test
	public void testDeprecated() throws ReflectiveOperationException {
		var schema = SchemaBuilder.build("com.fleetpin.graphql.builder.type");

		var query = schema.getQueryType().getField("deprecatedTest");
		assertTrue(query.isDeprecated());
		assertEquals("old", query.getDeprecationReason());

		GraphQLObjectType type = (GraphQLObjectType) schema.getType("DeprecatedObject");
		var field = type.getField("naame");
		assertTrue(field.isDeprecated());
		assertEquals("spelling", field.getDeprecationReason());
	}

	@Test
	public void testDescription() throws ReflectiveOperationException {
		var schema = SchemaBuilder.build("com.fleetpin.graphql.builder.type");

		var query = schema.getQueryType().getField("descriptionTest");
		assertEquals("returns something", query.getDescription());

		GraphQLObjectType type = (GraphQLObjectType) schema.getType("DescriptionObject");
		assertEquals("test description comes through", type.getDescription());
		var field = type.getField("name");
		assertEquals("first and last", field.getDescription());
	}
}
