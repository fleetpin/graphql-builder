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

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionWithDirectivesSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

//does not test all of records as needs newer version of java. But Classes that look like records
public class RecordTest {

	@Test
	public void testEntireContext() {
		var type = Map.of("name", "foo", "age", 4);
		Map<String, Map<String, Object>> response = execute(
			"query passthrough($type: InputTypeInput!){passthrough(type: $type) {name age}} ",
			Map.of("type", type)
		)
			.getData();
		var passthrough = response.get("passthrough");
		assertEquals(type, passthrough);
	}

	private ExecutionResult execute(String query, Map<String, Object> variables) {
		try {
			GraphQL schema = GraphQL
				.newGraphQL(new IntrospectionWithDirectivesSupport().apply(SchemaBuilder.build("com.fleetpin.graphql.builder.record")))
				.build();
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
