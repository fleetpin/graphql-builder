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

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeParsingTest {

	@Test
	public void findTypes() throws ReflectiveOperationException {
		Map<String, Map<String, List<Map<String, String>>>> response = execute("{__schema {types {name}}} ").getData();
		var types = response.get("__schema").get("types");
		var count = types.stream().filter(map -> map.get("name").equals("SimpleType")).count();
		Assertions.assertEquals(1, count);
	}

	@Test
	public void testName() throws ReflectiveOperationException {
		var name = getField("name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testDeleted() throws ReflectiveOperationException {
		var name = getField("deleted");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	@Test
	public void testAlive() throws ReflectiveOperationException {
		var name = getField("alive");
		confirmBoolean(name);
	}

	@Test
	public void testParts() throws ReflectiveOperationException {
		var type = getField("parts");
		type = confirmNonNull(type);
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmString(type);
	}

	@Test
	public void testGappyParts() throws ReflectiveOperationException {
		var type = getField("gappyParts");
		type = confirmNonNull(type);
		type = confirmArray(type);
		confirmString(type);
	}

	@Test
	public void testOptioanlParts() throws ReflectiveOperationException {
		var type = getField("optionalParts");
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmString(type);
	}

	@Test
	public void testOptioanlGappyParts() throws ReflectiveOperationException {
		var type = getField("optionalGappyParts");
		type = confirmArray(type);
		confirmString(type);
	}

	@Test
	public void testNameFuture() throws ReflectiveOperationException {
		var name = getField("nameFuture");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void isDeletedFuture() throws ReflectiveOperationException {
		var name = getField("deletedFuture");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	@Test
	public void testAliveFuture() throws ReflectiveOperationException {
		var name = getField("aliveFuture");
		confirmBoolean(name);
	}

	@Test
	public void testPartsFuture() throws ReflectiveOperationException {
		var type = getField("partsFuture");
		type = confirmNonNull(type);
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmString(type);
	}

	@Test
	public void testGappyPartsFuture() throws ReflectiveOperationException {
		var type = getField("gappyPartsFuture");
		type = confirmNonNull(type);
		type = confirmArray(type);
		confirmString(type);
	}

	@Test
	public void testOptionalPartsFuture() throws ReflectiveOperationException {
		var type = getField("optionalPartsFuture");
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmString(type);
	}

	@Test
	public void testOptionalGappyPartsFuture() throws ReflectiveOperationException {
		var type = getField("optionalGappyPartsFuture");
		type = confirmArray(type);
		confirmString(type);
	}

	private void confirmString(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("String", type.get("name"));
	}

	private void confirmBoolean(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("Boolean", type.get("name"));
	}

	private Map<String, Object> confirmNonNull(Map<String, Object> type) {
		Assertions.assertEquals("NON_NULL", type.get("kind"));
		var toReturn = (Map<String, Object>) type.get("ofType");
		Assertions.assertNotNull(toReturn);
		return toReturn;
	}

	private Map<String, Object> confirmArray(Map<String, Object> type) {
		Assertions.assertEquals("LIST", type.get("kind"));
		var toReturn = (Map<String, Object>) type.get("ofType");
		Assertions.assertNotNull(toReturn);
		return toReturn;
	}

	public Map<String, Object> getField(String name) throws ReflectiveOperationException {
		Map<String, Map<String, Object>> response = execute(
			"{" +
			"  __type(name: \"SimpleType\") {" +
			"    name" +
			"    fields {" +
			"      name" +
			"      type {" +
			"        name" +
			"        kind" +
			"        ofType {" +
			"          name" +
			"          kind" +
			"          ofType {" +
			"            name" +
			"            kind" +
			"            ofType {" +
			"              name" +
			"              kind" +
			"            }" +
			"          }" +
			"        }" +
			"      }" +
			"    }" +
			"  }" +
			"} "
		)
			.getData();
		var type = response.get("__type");
		Assertions.assertEquals("SimpleType", type.get("name"));

		List<Map<String, Object>> fields = (List<Map<String, Object>>) type.get("fields");
		var field = fields.stream().filter(map -> map.get("name").equals(name)).findAny().get();
		Assertions.assertEquals(name, field.get("name"));
		return (Map<String, Object>) field.get("type");
	}

	@Test
	public void testQuery() throws ReflectiveOperationException {
		Map<String, Map<String, Object>> response = execute(
			"query {simpleType{" +
			"name " +
			"deleted " +
			"alive " +
			"parts " +
			"gappyParts " +
			"optionalParts " +
			"optionalGappyParts " +
			"nameFuture " +
			"deletedFuture " +
			"aliveFuture " +
			"partsFuture " +
			"gappyPartsFuture " +
			"optionalPartsFuture " +
			"optionalGappyPartsFuture " +
			"}} "
		)
			.getData();

		var simpleType = response.get("simpleType");
		assertEquals("green", simpleType.get("name"));
		assertEquals(false, simpleType.get("deleted"));
		assertEquals(null, simpleType.get("alive"));
		assertEquals(Arrays.asList("green", "eggs"), simpleType.get("parts"));
		assertEquals(Arrays.asList(null, "eggs"), simpleType.get("gappyParts"));
		assertEquals(null, simpleType.get("optionalParts"));
		assertEquals(Arrays.asList(), simpleType.get("optionalGappyParts"));

		assertEquals("green", simpleType.get("nameFuture"));
		assertEquals(false, simpleType.get("deletedFuture"));
		assertEquals(false, simpleType.get("aliveFuture"));
		assertEquals(Arrays.asList(), simpleType.get("partsFuture"));
		assertEquals(Arrays.asList(), simpleType.get("gappyPartsFuture"));
		assertEquals(Arrays.asList(), simpleType.get("optionalPartsFuture"));
		assertEquals(null, simpleType.get("optionalGappyPartsFuture"));
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
