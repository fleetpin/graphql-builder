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
