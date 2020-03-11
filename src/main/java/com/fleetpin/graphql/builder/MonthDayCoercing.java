package com.fleetpin.graphql.builder;

import java.time.MonthDay;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class MonthDayCoercing implements Coercing<MonthDay, MonthDay> {

	@Override
	public MonthDay serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public MonthDay parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public MonthDay parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private MonthDay convertImpl(Object input) {
         if (input instanceof MonthDay) {
             return (MonthDay) input;
         } else if (input instanceof String) {
             return MonthDay.parse((String) input);
         }
         return null;
     }
}