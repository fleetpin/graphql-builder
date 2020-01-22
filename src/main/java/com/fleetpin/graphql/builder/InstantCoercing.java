package com.fleetpin.graphql.builder;

import java.time.Instant;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class InstantCoercing implements Coercing<Instant, Instant> {

	@Override
	public Instant serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public Instant parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public Instant parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private Instant convertImpl(Object input) {
         if (input instanceof Instant) {
             return (Instant) input;
         } else if (input instanceof String) {
             return Instant.parse((String) input);
         }
         if (input instanceof Long) {
             return Instant.ofEpochMilli((long) input);
         }
         return null;
     }


}
