/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.fleetpin.graphql.builder;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.time.Instant;

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
