package com.fleetpin.graphql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeInheritanceParsingTest {

	@Test
	public void findTypes() throws ReflectiveOperationException {
		Map<String, Map<String, List<Map<String, String>>>> response = execute("{__schema {types {name}}} ").getData();
		var types = response.get("__schema").get("types");
		var count = types.stream().filter(map -> map.get("name").equals("SimpleType")).count();
		Assertions.assertEquals(1, count);
	}

	@Test
	public void testAnimalName() throws ReflectiveOperationException {
		var name = getField("Animal", "INTERFACE", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testAnimalInputName() throws ReflectiveOperationException {
		var name = getField("AnimalInput", "INPUT_OBJECT", "cat");
		confirmInputObject(name, "CatInput");
	}

	@Test
	public void testCatName() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testCatAge() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "age");
		var nonNull = confirmNonNull(name);
		confirmNumber(nonNull);
	}

	@Test
	public void testCatFur() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "fur");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}

	@Test
	public void testCatCalico() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "calico");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
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
		confirmString(nonNull);
	}

	@Test
	public void testDogAge() throws ReflectiveOperationException {
		var name = getField("Dog", "OBJECT", "age");
		var nonNull = confirmNonNull(name);
		confirmNumber(nonNull);
	}

	private void confirmString(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("String", type.get("name"));
	}

	private void confirmInputObject(Map<String, Object> type, String name) {
		Assertions.assertEquals("INPUT_OBJECT", type.get("kind"));
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
			"    inputFields {" +
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
		if (fields == null) {
			fields = (List<Map<String, Object>>) type.get("inputFields");
		}
		var field = fields.stream().filter(map -> map.get("name").equals(name)).findAny().get();
		Assertions.assertEquals(name, field.get("name"));
		return (Map<String, Object>) field.get("type");
	}

	@Test
	public void testQueryCatFur() throws ReflectiveOperationException {
		Map<String, List<Map<String, Object>>> response = execute(
			"query {animals{" + "name " + "... on Cat { " + "  age " + "  fur " + "  calico " + "} " + "... on Dog {" + " age " + "} " + "}} "
		)
			.getData();

		var animals = response.get("animals");

		var cat = animals.get(0);
		var dog = animals.get(1);

		assertEquals("name", cat.get("name"));
		assertEquals(3, cat.get("age"));
		assertEquals(true, cat.get("fur"));
		assertEquals(true, cat.get("calico"));

		assertEquals("name", dog.get("name"));
		assertEquals(6, dog.get("age"));
	}

	@Test
	public void testQueryDogFur() throws ReflectiveOperationException {
		Map<String, List<Map<String, Object>>> response = execute(
			"query {animals{" + "name " + "... on Cat { " + "  age " + "  calico " + "} " + "... on Dog {" + " age " + " fur " + "} " + "}} "
		)
			.getData();

		var animals = response.get("animals");

		var cat = animals.get(0);
		var dog = animals.get(1);

		assertEquals("name", cat.get("name"));
		assertEquals(3, cat.get("age"));
		assertEquals(true, cat.get("calico"));

		assertEquals("name", dog.get("name"));
		assertEquals(6, dog.get("age"));
		assertEquals("shaggy", dog.get("fur"));
	}

	@Test
	public void testBothFurFails() throws ReflectiveOperationException {
		Assertions.assertThrows(
			RuntimeException.class,
			() -> {
				execute(
					"query {animals{" + "name " + "... on Cat { " + "  age " + "  fur " + "  calico " + "} " + "... on Dog {" + " age " + " fur " + "} " + "}} "
				);
			}
		);
	}

	@Test
	public void testOneOf() throws ReflectiveOperationException {
		Map<String, List<Map<String, Object>>> response = execute(
			"mutation {myAnimals(animals: [" +
			"{cat: {fur: true, calico: false, name: \"socks\", age: 4}}," +
			"{dog: {fur: \"short\", name: \"patches\", age: 5}}" +
			"]){" +
			"name " +
			"... on Cat { " +
			"  age " +
			"  calico " +
			"} " +
			"... on Dog {" +
			" age " +
			" fur " +
			"} " +
			"}} "
		)
			.getData();

		var animals = response.get("myAnimals");

		var cat = animals.get(0);
		var dog = animals.get(1);

		assertEquals("socks", cat.get("name"));
		assertEquals(4, cat.get("age"));
		assertEquals(false, cat.get("calico"));

		assertEquals("patches", dog.get("name"));
		assertEquals(5, dog.get("age"));
		assertEquals("short", dog.get("fur"));
	}

	@Test
	public void testOneOfError() throws ReflectiveOperationException {
		var exception = assertThrows(
			RuntimeException.class,
			() ->
				execute(
					"mutation {myAnimals(animals: [" +
					"{cat: {fur: true, calico: false, name: \"socks\", age: 4}," +
					"dog: {fur: \"short\", name: \"patches\", age: 5}}" +
					"]){" +
					"name " +
					"... on Cat { " +
					"  age " +
					"  calico " +
					"} " +
					"... on Dog {" +
					" age " +
					" fur " +
					"} " +
					"}} "
				)
					.getData()
		);

		assertTrue(exception.getMessage().contains("OneOf must only have a single field set"));
	}

	private ExecutionResult execute(String query) {
		try {
			GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.type")).build();
			ExecutionResult result = schema.execute(query);
			if (!result.getErrors().isEmpty()) {
				throw new RuntimeException(result.getErrors().toString()); //TODO:cleanup
			}
			return result;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
