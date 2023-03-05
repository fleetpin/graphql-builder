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
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnionTest {

	@Test
	public void testUnion() throws ReflectiveOperationException {
		Map<String, List<Map<String, Object>>> response = execute(
			"query {union{" +
			"  __typename" +
			"  ... on SimpleType {" +
			"    name" +
			"  }" +
			"  ... on UnionType {" +
			"    type {" +
			"      __typename" +
			"      ... on SimpleType {" +
			"        name" +
			"      }" +
			"    }" +
			"  }" +
			"}} "
		)
			.getData();
		var union = response.get("union");
		assertEquals("green", union.get(0).get("name"));
		Map<String, Object> simple = (Map<String, Object>) union.get(1).get("type");
		assertEquals("green", simple.get("name"));
	}

	@Test
	public void testUnionFailure() throws ReflectiveOperationException {
		var error = assertThrows(
			RuntimeException.class,
			() ->
				execute(
					"query {unionFailure{" +
					"  __typename" +
					"  ... on SimpleType {" +
					"    name" +
					"  }" +
					"  ... on UnionType {" +
					"    type {" +
					"      __typename" +
					"      ... on SimpleType {" +
					"        name" +
					"      }" +
					"    }" +
					"  }" +
					"}} "
				)
		);
		assertEquals("Union Union_SimpleType_UnionType Does not support type Boolean", error.getMessage());
	}

	private ExecutionResult execute(String query) throws ReflectiveOperationException {
		var schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.type")).build();
		ExecutionResult result = schema.execute(query);
		if (!result.getErrors().isEmpty()) {
			throw new RuntimeException(result.getErrors().toString()); //TODO:cleanup
		}
		return result;
	}
}
