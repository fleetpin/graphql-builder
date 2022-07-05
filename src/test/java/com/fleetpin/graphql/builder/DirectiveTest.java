package com.fleetpin.graphql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import graphql.GraphQL;
import graphql.schema.FieldCoordinates;

public class DirectiveTest {
	@Test
	public void testDirectiveAppliedToQuery() throws ReflectiveOperationException {

		GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.type.directive")).build();
		var cat = schema.getGraphQLSchema()
				.getFieldDefinition(FieldCoordinates.coordinates(schema.getGraphQLSchema().getQueryType(), "getCat"));
		var capture = cat.getAppliedDirective("Capture");
		var argument = capture.getArgument("color");
		var color = argument.getValue();
		assertEquals("meow", color);

	}
	
	@Test
	public void testPresentOnSchema() throws ReflectiveOperationException {

		GraphQL schema = GraphQL.newGraphQL(SchemaBuilder.build("com.fleetpin.graphql.builder.type.directive")).build();
		var capture = schema.getGraphQLSchema().getSchemaAppliedDirective("Capture");
		var argument = capture.getArgument("color");
		var color = argument.getValue();
		assertEquals("top", color);

	}

}
