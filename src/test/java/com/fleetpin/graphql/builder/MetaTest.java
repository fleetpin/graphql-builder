package com.fleetpin.graphql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.schema.GraphQLObjectType;
import org.junit.jupiter.api.Test;

public class MetaTest {

	@Test
	public void testDeprecated() throws ReflectiveOperationException {
		var schema = SchemaBuilder.build("com.fleetpin.graphql.builder.type");

		var query = schema.getQueryType().getField("deprecatedTest");
		assertTrue(query.isDeprecated());
		assertEquals("old", query.getDeprecationReason());

		GraphQLObjectType type = (GraphQLObjectType) schema.getType("DeprecatedObject");
		var field = type.getField("naame");
		assertTrue(field.isDeprecated());
		assertEquals("spelling", field.getDeprecationReason());
	}

	@Test
	public void testDescription() throws ReflectiveOperationException {
		var schema = SchemaBuilder.build("com.fleetpin.graphql.builder.type");

		var query = schema.getQueryType().getField("descriptionTest");
		assertEquals("returns something", query.getDescription());

		GraphQLObjectType type = (GraphQLObjectType) schema.getType("DescriptionObject");
		assertEquals("test description comes through", type.getDescription());
		var field = type.getField("name");
		assertEquals("first and last", field.getDescription());
	}
}
