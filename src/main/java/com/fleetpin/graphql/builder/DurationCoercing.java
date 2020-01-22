package com.fleetpin.graphql.builder;

import java.time.Duration;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class DurationCoercing implements Coercing<Duration, Duration> {

	@Override
	public Duration serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public Duration parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public Duration parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private Duration convertImpl(Object input) {
         if (input instanceof Duration) {
             return (Duration) input;
         } else if (input instanceof String) {
             return Duration.parse((String) input);
         }
         return null;
     }


}
