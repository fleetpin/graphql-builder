package com.fleetpin.graphql.builder;

import static graphql.Assert.assertNotNull;

import graphql.Internal;
import graphql.language.IntValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class GraphqlLongCoercing implements Coercing<Long, Long> {

	private Long convertImpl(Object input) {
		if (input instanceof Long) {
			return (Long) input;
		} else if (isNumberIsh(input)) {
			BigDecimal value;
			try {
				value = new BigDecimal(input.toString());
			} catch (NumberFormatException e) {
				return null;
			}
			try {
				return value.longValueExact();
			} catch (ArithmeticException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Long serialize(Object input) {
		Long result = convertImpl(input);
		if (result == null) {
			throw new CoercingSerializeException("Expected type 'Long' but was '" + typeName(input) + "'.");
		}
		return result;
	}

	@Override
	public Long parseValue(Object input) {
		Long result = convertImpl(input);
		if (result == null) {
			throw new CoercingParseValueException("Expected type 'Int' but was '" + typeName(input) + "'.");
		}
		return result;
	}

	@Override
	public Long parseLiteral(Object input) {
		if (!(input instanceof IntValue)) {
			throw new CoercingParseLiteralException("Expected AST type 'IntValue' but was '" + typeName(input) + "'.");
		}
		BigInteger value = ((IntValue) input).getValue();
		return value.longValue();
	}

	private boolean isNumberIsh(Object input) {
		return input instanceof Number || input instanceof String;
	}

	private String typeName(Object input) {
		if (input == null) {
			return "null";
		}

		return input.getClass().getSimpleName();
	}
}
