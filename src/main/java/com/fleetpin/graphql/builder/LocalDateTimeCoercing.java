package com.fleetpin.graphql.builder;

import java.time.LocalDateTime;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class LocalDateTimeCoercing implements Coercing<LocalDateTime, LocalDateTime> {

	@Override
	public LocalDateTime serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private LocalDateTime convertImpl(Object input) {
         if (input instanceof LocalDateTime) {
             return (LocalDateTime) input;
         } else if (input instanceof String) {
             return LocalDateTime.parse((String) input);
         }
         return null;
     }


}
