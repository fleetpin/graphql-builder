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
import java.time.ZoneId;

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
