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

import graphql.GraphQL;
import graphql.schema.FieldCoordinates;
import org.junit.jupiter.api.Test;

public class DirectiveTest {

	@Test
	public void testDirectiveAppliedToQuery() throws ReflectiveOperationException {
		GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.type.directive")).build();
		var cat = schema.getGraphQLSchema().getFieldDefinition(FieldCoordinates.coordinates(schema.getGraphQLSchema().getQueryType(), "getCat"));
		var capture = cat.getAppliedDirective("Capture");
		var argument = capture.getArgument("color");
		var color = argument.getValue();
		assertEquals("meow", color);
	}

	@Test
	public void testPresentOnSchema() throws ReflectiveOperationException {
		GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.type.directive")).build();
		var capture = schema.getGraphQLSchema().getSchemaAppliedDirective("Capture");
		var argument = capture.getArgument("color");
		var color = argument.getValue();
		assertEquals("top", color);
	}
}
