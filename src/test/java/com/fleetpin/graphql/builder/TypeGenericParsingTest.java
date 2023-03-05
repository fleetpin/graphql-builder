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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeGenericParsingTest {

	@Test
	public void testAnimalName() throws ReflectiveOperationException {
		var name = getField("Animal", "INTERFACE", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testAnimalFur() throws ReflectiveOperationException {
		var name = getField("Animal", "INTERFACE", "fur");
		var nonNull = confirmNonNull(name);
		confirmInterface(nonNull, "Fur");
	}

	@Test
	public void testAnimalFurs() throws ReflectiveOperationException {
		var name = getField("Animal", "INTERFACE", "furs");
		var type = confirmNonNull(name);
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmInterface(type, "Fur");
	}

	@Test
	public void testCatName() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testCatFur() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "fur");
		var nonNull = confirmNonNull(name);
		confirmObject(nonNull, "CatFur");
	}

	@Test
	public void testCatFurs() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "furs");
		var type = confirmNonNull(name);
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmObject(type, "CatFur");
	}

	@Test
	public void testDogName() throws ReflectiveOperationException {
		var name = getField("Dog", "OBJECT", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testDogFur() throws ReflectiveOperationException {
		var name = getField("Dog", "OBJECT", "fur");
		var nonNull = confirmNonNull(name);
		confirmObject(nonNull, "DogFur");
	}

	@Test
	public void testDogFurs() throws ReflectiveOperationException {
		var name = getField("Dog", "OBJECT", "furs");
		var type = confirmNonNull(name);
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmObject(type, "DogFur");
	}

	@Test
	public void testFurName() throws ReflectiveOperationException {
		var name = getField("Fur", "INTERFACE", "length");
		var nonNull = confirmNonNull(name);
		confirmNumber(nonNull);
	}

	@Test
	public void testCatFurCalico() throws ReflectiveOperationException {
		var name = getField("CatFur", "OBJECT", "calico");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	@Test
	public void testCatFurLong() throws ReflectiveOperationException {
		var name = getField("CatFur", "OBJECT", "long");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	@Test
	public void testDogFurShaggy() throws ReflectiveOperationException {
		var name = getField("DogFur", "OBJECT", "shaggy");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	@Test
	public void testCatFamilyName() throws ReflectiveOperationException {
		var name = getField("CatFamily", "INTERFACE", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testCatFamilyFur() throws ReflectiveOperationException {
		var name = getField("CatFamily", "INTERFACE", "fur");
		var nonNull = confirmNonNull(name);
		confirmInterface(nonNull, "CatFamilyFur");
	}

	@Test
	public void testCatFamilyFurs() throws ReflectiveOperationException {
		var name = getField("CatFamily", "INTERFACE", "furs");
		var type = confirmNonNull(name);
		type = confirmArray(type);
		type = confirmNonNull(type);
		confirmInterface(type, "CatFamilyFur");
	}

	@Test
	public void testCatFamilyFurCalico() throws ReflectiveOperationException {
		var name = getField("CatFamilyFur", "INTERFACE", "length");
		var nonNull = confirmNonNull(name);
		confirmNumber(nonNull);
	}

	@Test
	public void testCatFamilyFurLong() throws ReflectiveOperationException {
		var name = getField("CatFamilyFur", "INTERFACE", "long");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	private void confirmString(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("String", type.get("name"));
	}

	private void confirmInterface(Map<String, Object> type, String name) {
		Assertions.assertEquals("INTERFACE", type.get("kind"));
		Assertions.assertEquals(name, type.get("name"));
	}

	private void confirmObject(Map<String, Object> type, String name) {
		Assertions.assertEquals("OBJECT", type.get("kind"));
		Assertions.assertEquals(name, type.get("name"));
	}

	private void confirmBoolean(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("Boolean", type.get("name"));
	}

	private void confirmNumber(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("Int", type.get("name"));
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

	public Map<String, Object> getField(String typeName, String kind, String name) throws ReflectiveOperationException {
		Map<String, Map<String, Object>> response = execute(
			"{" +
			"  __type(name: \"" +
			typeName +
			"\") {" +
			"    name" +
			"    kind" +
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
		Assertions.assertEquals(typeName, type.get("name"));
		Assertions.assertEquals(kind, type.get("kind"));

		List<Map<String, Object>> fields = (List<Map<String, Object>>) type.get("fields");
		var field = fields.stream().filter(map -> map.get("name").equals(name)).findAny().get();
		Assertions.assertEquals(name, field.get("name"));
		return (Map<String, Object>) field.get("type");
	}

	@Test
	public void testQueryCatFur() throws ReflectiveOperationException {
		Map<String, List<Map<String, Object>>> response = execute(
			"query {animals{" +
			"name " +
			"... on Cat { " +
			" fur{ " +
			"  calico " +
			"  length" +
			"  catFur: long" +
			" }" +
			"} " +
			"... on Dog {" +
			" fur {" +
			"   shaggy" +
			"   dogFur: long" +
			"   length" +
			" } " +
			"} " +
			"}} "
		)
			.getData();

		var animals = response.get("animals");

		var cat = animals.get(0);
		var dog = animals.get(1);

		var catFur = (Map<String, Object>) cat.get("fur");
		var dogFur = (Map<String, Object>) dog.get("fur");

		assertEquals("name", cat.get("name"));
		assertEquals(4, catFur.get("length"));
		assertEquals(true, catFur.get("calico"));
		assertEquals(true, catFur.get("catFur"));

		assertEquals(4, dogFur.get("length"));
		assertEquals(true, dogFur.get("shaggy"));
		assertEquals("very", dogFur.get("dogFur"));
	}

	@Test
	public void testMutationCatFur() throws ReflectiveOperationException {
		Map<String, Map<String, Map<String, Object>>> response = execute(
			"mutation {makeCat{" +
			"item { " +
			"  ... on Cat { " +
			"   name " +
			"   fur{ " +
			"    calico " +
			"    length" +
			"    long" +
			"   }" +
			"  } " +
			"}" +
			"}} "
		)
			.getData();

		var makeCat = response.get("makeCat");

		var cat = makeCat.get("item");

		var catFur = (Map<String, Object>) cat.get("fur");

		assertEquals("name", cat.get("name"));
		assertEquals(4, catFur.get("length"));
		assertEquals(true, catFur.get("calico"));
		assertEquals(true, catFur.get("long"));
	}

	private ExecutionResult execute(String query) {
		try {
			GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.generics")).build();
			ExecutionResult result = schema.execute(query);
			if (!result.getErrors().isEmpty()) {
				throw new RuntimeException(result.getErrors().toString());
			}
			return result;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
