package com.fleetpin.graphql.builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class LocalDateCoercing implements Coercing<LocalDate, LocalDate> {

	@Override
	public LocalDate serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public LocalDate parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private LocalDate convertImpl(Object input) {
         if (input instanceof LocalDate) {
             return (LocalDate) input;
         } else if (input instanceof String) {
             return LocalDate.parse((String) input);
         }
         return null;
     }


}
