package com.fleetpin.graphql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ParameterParsingTest {

	
	@Test
	public void testOptionalArray() throws ReflectiveOperationException {
		var schema = SchemaBuilder.build("com.fleetpin.graphql.builder.parameter").build();
		Map<String, List<String>> response = schema.execute("query {test(type: [\"Hi\", \"There\"])} ").getData();
		assertEquals(Arrays.asList("Hi", "There"), response.get("test"));
	}
	
}
