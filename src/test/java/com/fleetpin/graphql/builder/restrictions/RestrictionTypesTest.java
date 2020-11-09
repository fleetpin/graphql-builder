package com.fleetpin.graphql.builder.restrictions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fleetpin.graphql.builder.SchemaBuilder;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;

public class RestrictionTypesTest {
	/*
	 * Really basic tests, just to ensure restrictions work on different types.
	 */
	
	private static GraphQL schema;
	@BeforeAll
	public static void init() throws ReflectiveOperationException {
		schema = SchemaBuilder.build("com.fleetpin.graphql.builder.restrictions.parameter").build();	
	}
	
	private static String singleQueryGql = "query entityQuery( $allowed: Boolean! ) { single(allowed: $allowed) { __typename } }";
	private static String singleOptionalQueryGql = "query entityQuery( $allowed: Boolean ) { singleOptional(allowed: $allowed) { __typename } }";
	private static String listQueryGql = "query entityQuery( $allowed: [Boolean!]! ) { list(allowed: $allowed) { __typename } }";
	private static String listOptionalQueryGql = "query entityQuery( $allowed: [Boolean!] ) { listOptional(allowed: $allowed) { __typename } }";
	
	
	@Test
	public void singleEntityQuery() throws ReflectiveOperationException, JsonMappingException, JsonProcessingException {
		Map<String, Object> variables = new HashMap<>();
		variables.put("allowed", true);
		Map<String, Map<String,Object>> response = execute(singleQueryGql, variables).getData();
		// No fancy checks. Just want to make ensure it executes without issue.
		Assertions.assertTrue(response.get("single").containsKey("__typename"));
	}
	
	@Test
	public void singleOptionalEntityQuery() throws ReflectiveOperationException, JsonMappingException, JsonProcessingException {
		Map<String, Object> variables = new HashMap<>();
		
		// Allowed
		variables.put("allowed", true);
		Map<String, Map<String,Object>> responseAllowed = execute(singleOptionalQueryGql, variables).getData();
		Assertions.assertTrue(responseAllowed.get("singleOptional").containsKey("__typename"));
		
		// Not allowed
		variables.put("allowed",  false);
		Map<String, Object> responseDenied = execute(singleOptionalQueryGql, variables).getData();
		Assertions.assertNull(responseDenied.get("singleOptional"));
	}
	
	
	@Test
	public void listEntityQuery() throws ReflectiveOperationException, JsonMappingException, JsonProcessingException {
		Map<String, Object> variables = new HashMap<>();
		
		variables.put("allowed", Arrays.asList(true,true,true));
		Map<String, List<Object>> responseAllAllowed = execute(listQueryGql, variables).getData();
		Assertions.assertEquals(3, responseAllAllowed.get("list").size());
		
		variables.put("allowed", Arrays.asList(true,false,true));
		Map<String, List<Object>> responseSomeAllowed = execute(listQueryGql, variables).getData();
		Assertions.assertEquals(2, responseSomeAllowed.get("list").size());
		
		variables.put("allowed", Arrays.asList(false,false,false));
		Map<String, List<Object>> responseNoneAllowed = execute(listQueryGql, variables).getData();
		Assertions.assertEquals(0, responseNoneAllowed.get("list").size());
	}
	
	
	@Test
	public void optionalListEntityQuery() throws ReflectiveOperationException, JsonMappingException, JsonProcessingException {
		Map<String, Object> variables = new HashMap<>();
		
		// No list passed through
		Map<String, List<Object>> responseNoVariables = execute(listOptionalQueryGql, variables).getData();
		Assertions.assertNull(responseNoVariables.get("listOptional"));
		
		variables.put("allowed", Arrays.asList(true,true,true));
		Map<String, List<Object>> responseAllAllowed = execute(listOptionalQueryGql, variables).getData();
		Assertions.assertEquals(3, responseAllAllowed.get("listOptional").size());
		
		variables.put("allowed", Arrays.asList(true,false,true));
		Map<String, List<Object>> responseSomeAllowed = execute(listOptionalQueryGql, variables).getData();
		Assertions.assertEquals(2, responseSomeAllowed.get("listOptional").size());
		
		variables.put("allowed", Arrays.asList(false,false,false));
		Map<String, List<Object>> responseNoneAllowed = execute(listOptionalQueryGql, variables).getData();
		Assertions.assertEquals(0, responseNoneAllowed.get("listOptional").size());
	}
	
	private static ExecutionResult execute(String query, Map<String, Object> variables)
			throws JsonMappingException, JsonProcessingException {
		
		var input = ExecutionInput.newExecutionInput().query(query).variables(variables).build();
		ExecutionResult result = schema.execute(input);
		if (!result.getErrors().isEmpty()) {
			throw new RuntimeException(result.getErrors().toString());
		}
		return result;
	}

}
