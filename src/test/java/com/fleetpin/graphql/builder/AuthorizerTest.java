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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class AuthorizerTest {

	@Test
	public void testQueryCatAllowed() throws ReflectiveOperationException {
		var result = execute("query {getCat(name: \"socks\") {age}}");
		Map<String, Map<String, Object>> response = result.getData();

		var cat = response.get("getCat");

		assertEquals(3, cat.get("age"));

		assertTrue(result.getErrors().isEmpty());
	}

	@Test
	public void testQueryCatNotAllowed() throws ReflectiveOperationException {
		var result = execute("query {getCat(name: \"boots\") {age}}");

		assertNull(result.getData());

		assertEquals(1, result.getErrors().size());
		var error = result.getErrors().get(0);

		assertEquals("Exception while fetching data (/getCat) : unauthorized", error.getMessage());
		//assertEquals("", error.getErrorType());
	}

	private ExecutionResult execute(String query) {
		return execute(query, null);
	}

	private ExecutionResult execute(String query, Map<String, Object> variables) {
		try {
			GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.authorizer")).build();
			var input = ExecutionInput.newExecutionInput();
			input.query(query);
			if (variables != null) {
				input.variables(variables);
			}
			ExecutionResult result = schema.execute(input);
			return result;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
