package com.fleetpin.graphql.builder.mapper;

import graphql.GraphQLContext;
import java.util.Locale;

public interface InputTypeBuilder {
	Object convert(Object obj, GraphQLContext graphQLContext, Locale locale);
}
