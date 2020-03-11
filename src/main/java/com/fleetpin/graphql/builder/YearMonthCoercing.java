package com.fleetpin.graphql.builder;

import java.time.YearMonth;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class YearMonthCoercing implements Coercing<YearMonth, YearMonth> {

	@Override
	public YearMonth serialize(Object dataFetcherResult) throws CoercingSerializeException {
		return convertImpl(dataFetcherResult);
	}

	@Override
	public YearMonth parseValue(Object input) throws CoercingParseValueException {
		return convertImpl(input);
	}

	@Override
	public YearMonth parseLiteral(Object input) throws CoercingParseLiteralException {
		return convertImpl(input);
	}
	
	
	 private YearMonth convertImpl(Object input) {
         if (input instanceof YearMonth) {
             return (YearMonth) input;
         } else if (input instanceof String) {
             return YearMonth.parse((String) input);
         }
         return null;
     }
}