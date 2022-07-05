package com.fleetpin.graphql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import graphql.ExecutionResult;
import graphql.GraphQL;

public class TypeGenericInputParsingTest {

	@Test
	public void testAnimalName() throws ReflectiveOperationException {
		var name = getField("Animal", "INTERFACE", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}

	@Test
	public void testCatName() throws ReflectiveOperationException {
		var name = getField("Cat", "OBJECT", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}
	
	@Test
	public void testCatInputName() throws ReflectiveOperationException {
		var name = getInputField("CatInput", "INPUT_OBJECT", "name");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}
	
	@Test
	public void testCatInputFur() throws ReflectiveOperationException {
		var name = getInputField("CatInput", "INPUT_OBJECT", "fur");
		var nonNull = confirmNonNull(name);
		confirmBoolean(nonNull);
	}
	
	
	@Test
	public void testAnimalInputName() throws ReflectiveOperationException {
		var name = getInputField("CatAnimalInput", "INPUT_OBJECT", "id");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}
	
	
	@Test
	public void testAnimalInputGenericName() throws ReflectiveOperationException {
		var name = getInputField("AnimalInput_Cat", "INPUT_OBJECT", "id");
		var nonNull = confirmNonNull(name);
		confirmString(nonNull);
	}
	
	
	@Test
	public void testAnimalInputCat() throws ReflectiveOperationException {
		var name = getInputField("CatAnimalInput", "INPUT_OBJECT", "animal");
		var nonNull = confirmNonNull(name);
		confirmInputObject(nonNull, "CatInput");

	}
	
	@Test
	public void testAnimalInputGenericCat() throws ReflectiveOperationException {
		var name = getInputField("AnimalInput_Cat", "INPUT_OBJECT", "animal");
		var nonNull = confirmNonNull(name);
		confirmInputObject(nonNull, "CatInput");

	}
	
	
	private void confirmString(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("String", type.get("name"));
	}


	private void confirmInputObject(Map<String, Object> type, String name) {
		Assertions.assertEquals("INPUT_OBJECT", type.get("kind"));
		Assertions.assertEquals(name, type.get("name"));
	}


	private Map<String, Object> confirmNonNull(Map<String, Object> type) {
		Assertions.assertEquals("NON_NULL", type.get("kind"));
		var toReturn = (Map<String, Object>) type.get("ofType");
		Assertions.assertNotNull(toReturn);
		return toReturn;
	}
	
	private void confirmBoolean(Map<String, Object> type) {
		Assertions.assertEquals("SCALAR", type.get("kind"));
		Assertions.assertEquals("Boolean", type.get("name"));
	}
	
	public Map<String, Object> getField(String typeName, String kind, String name) throws ReflectiveOperationException {
		Map<String, Map<String, Object>> response = execute("{" + 
				"  __type(name: \"" + typeName + "\") {" + 
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
				"} ").getData();
		var type = response.get("__type");
		Assertions.assertEquals(typeName, type.get("name"));
		Assertions.assertEquals(kind, type.get("kind"));
		List<Map<String, Object>> fields = (List<Map<String, Object>>) type.get("fields");
		var field = fields.stream().filter(map -> map.get("name").equals(name)).findAny().get();
		Assertions.assertEquals(name, field.get("name"));
		return (Map<String, Object>) field.get("type");
	}
	
	public Map<String, Object> getInputField(String typeName, String kind, String name) throws ReflectiveOperationException {
		Map<String, Map<String, Object>> response = execute("{" + 
				"  __type(name: \"" + typeName + "\") {" + 
				"    name" + 
				"    kind" +	
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
				"} ").getData();
		var type = response.get("__type");
		Assertions.assertEquals(typeName, type.get("name"));
		Assertions.assertEquals(kind, type.get("kind"));
		List<Map<String, Object>> fields = (List<Map<String, Object>>) type.get("inputFields");
		var field = fields.stream().filter(map -> map.get("name").equals(name)).findAny().get();
		Assertions.assertEquals(name, field.get("name"));
		return (Map<String, Object>) field.get("type");
	}

	@Test
	public void testQueryCatFur() throws ReflectiveOperationException {
		Map<String, String> response = execute("mutation {addCat(input: {id: \"1\", animal: {name: \"felix\", fur: false}})} ").getData();
		var cat = response.get("addCat");
		assertEquals("felix", cat);
	}
	
	@Test
	public void testQueryCatFurGeneric() throws ReflectiveOperationException {
		Map<String, Boolean> response = execute("mutation {addCatGenerics(input: {id: \"1\", animal: {name: \"felix\", fur: true}})} ").getData();
		var cat = response.get("addCatGenerics");
		assertEquals(true, cat);
	}



	private ExecutionResult execute(String query) {
		try {
			GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.inputgenerics")).build();
			ExecutionResult result = schema.execute(query);
			if(!result.getErrors().isEmpty()) {
				throw new RuntimeException(result.getErrors().toString());
			}
			return result;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}

	}

}
