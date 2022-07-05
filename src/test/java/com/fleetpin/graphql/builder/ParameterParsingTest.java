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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fleetpin.graphql.builder.annotations.Id;
import com.fleetpin.graphql.builder.annotations.Query;
import com.google.common.base.Optional;

import graphql.ExecutionResult;
import graphql.GraphQL;

public class ParameterParsingTest {

	//TODO:add failure cases
	@Test
	public void testRequiredString() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {requiredString(type: \"There\")} ").getData();
		assertEquals("There", response.get("requiredString"));
	}
	
	@Test
	public void testOptionalStringPresent() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {optionalString(type: \"There\")} ").getData();
		assertEquals("There", response.get("optionalString"));
	}
	
	@Test
	public void testOptionalStringNull() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {optionalString(type: null)} ").getData();
		assertEquals(null, response.get("optionalString"));
	}
	

	@Test
	public void testOptionalStringMissing() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {optionalString} ").getData();
		assertEquals(null, response.get("optionalString"));
	}
	
	//TODO:id checks don't confirm actual an id
	@Test
	public void testRequiredId() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {testRequiredId(type: \"There\")} ").getData();
		assertEquals("There", response.get("testRequiredId"));
	}	
	@Test
	public void testOptionalIdPresent() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {optionalId(type: \"There\")} ").getData();
		assertEquals("There", response.get("optionalId"));
	}
	
	@Test
	public void testOptionalIdNull() throws ReflectiveOperationException {
		Map<String, List<String>> response = execute("query {optionalId(type: null)} ").getData();
		assertEquals(null, response.get("optionalId"));
	}
	
	
	@Test
	public void testRequiredListStringEmpty() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {requiredListString(type: [])} ").getData();
		assertEquals(Collections.emptyList(), response.get("requiredListString"));
	}
	
	@Test
	public void testRequiredListString() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {requiredListString(type: [\"free\"])} ").getData();
		assertEquals(Arrays.asList("free"), response.get("requiredListString"));
	}
	
	@Test
	public void testOptionalListStringEmpty() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListString(type: [])} ").getData();
		assertEquals(Collections.emptyList(), response.get("optionalListString"));
	}
	
	@Test
	public void testOptionalListString() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListString(type: [\"free\"])} ").getData();
		assertEquals(Arrays.asList("free"), response.get("optionalListString"));
	}
	
	@Test
	public void testOptionalListStringNull() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListString} ").getData();
		assertEquals(null, response.get("optionalListString"));
	}
	
	
	@Test
	public void testRequiredListOptionalString() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {requiredListOptionalString(type: [null, \"free\"])} ").getData();
		assertEquals(Arrays.asList(null, "free"), response.get("requiredListOptionalString"));
	}
	
	
	@Test
	public void testOptionalListOptionalString() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListOptionalString(type: [null, \"free\"])} ").getData();
		assertEquals(Arrays.asList(null, "free"), response.get("optionalListOptionalString"));
	}
	
	@Test
	public void testOptionalListOptionalStringNull() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListOptionalString} ").getData();
		assertEquals(null, response.get("optionalListOptionalString"));
	}

	@Test
	public void testRequiredListIdEmpty() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {requiredListId(type: [])} ").getData();
		assertEquals(Collections.emptyList(), response.get("requiredListId"));
	}
	
	@Test
	public void testRequiredListId() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {requiredListId(type: [\"free\"])} ").getData();
		assertEquals(Arrays.asList("free"), response.get("requiredListId"));
	}
	
	@Test
	public void testOptionalListIdEmpty() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListId(type: [])} ").getData();
		assertEquals(Collections.emptyList(), response.get("optionalListId"));
	}
	
	@Test
	public void testOptionalListId() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListId(type: [\"free\"])} ").getData();
		assertEquals(Arrays.asList("free"), response.get("optionalListId"));
	}
	
	@Test
	public void testOptionalListIdNull() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListId} ").getData();
		assertEquals(null, response.get("optionalListId"));
	}
	
	
	@Test
	public void testRequiredListOptionalId() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {requiredListOptionalId(type: [null, \"free\"])} ").getData();
		assertEquals(Arrays.asList(null, "free"), response.get("requiredListOptionalId"));
	}
	
	
	@Test
	public void testOptionalListOptionalId() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListOptionalId(type: [null, \"free\"])} ").getData();
		assertEquals(Arrays.asList(null, "free"), response.get("optionalListOptionalId"));
	}
	
	@Test
	public void testOptionalListOptionalIdNull() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {optionalListOptionalId} ").getData();
		assertEquals(null, response.get("optionalListOptionalId"));
	}
	
	
	
	@Test
	public void testMultipleArguments() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArguments(first: \"free\", second: \"bird\")} ").getData();
		assertEquals("free:bird", response.get("multipleArguments"));
	}
	
	
	@Test
	public void testMultipleArgumentsOptional() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsOptional(first: \"free\", second: \"bird\")} ").getData();
		assertEquals("free:bird", response.get("multipleArgumentsOptional"));
	}
	
	@Test
	public void testMultipleArgumentsOptionalPartial1() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsOptional(second: \"bird\")} ").getData();
		assertEquals(":bird", response.get("multipleArgumentsOptional"));
	}
	
	@Test
	public void testMultipleArgumentsOptionalPartial2() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsOptional(second: null)} ").getData();
		assertEquals(":", response.get("multipleArgumentsOptional"));
	}
	
	@Test
	public void testMultipleArgumentsOptionalPartial3() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsOptional} ").getData();
		assertEquals(":", response.get("multipleArgumentsOptional"));
	}
	
	@Test
	public void testMultipleArgumentsMix1() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsMix(first: \"free\")} ").getData();
		assertEquals("free:", response.get("multipleArgumentsMix"));
	}
	
	@Test
	public void testMultipleArgumentsMix2() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsMix(first: \"free\", second: null)} ").getData();
		assertEquals("free:", response.get("multipleArgumentsMix"));
	}
	
	@Test
	public void testMultipleArgumentsMix3() throws ReflectiveOperationException {
		Map<String, List<List<String>>> response = execute("query {multipleArgumentsMix(first: \"free\", second: \"bird\")} ").getData();
		assertEquals("free:bird", response.get("multipleArgumentsMix"));
	}
	
	
	private ExecutionResult execute(String query) throws ReflectiveOperationException {
		var schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.parameter")).build();
		ExecutionResult result = schema.execute(query);
		if(!result.getErrors().isEmpty()) {
			throw new RuntimeException(result.getErrors().toString()); //TODO:cleanup
		}
		return result;
	}
	
}
