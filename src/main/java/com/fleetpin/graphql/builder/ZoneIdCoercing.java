package com.fleetpin.graphql.builder;

import java.time.ZoneId;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class ZoneIdCoercing implements Coercing<ZoneId, ZoneId> {

	@Override
	public ZoneId serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public ZoneId parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public ZoneId parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private ZoneId convertImpl(Object input) {
         if (input instanceof ZoneId) {
             return (ZoneId) input;
         } else if (input instanceof String) {
             return ZoneId.of((String) input);
         }
         return null;
     }


}
