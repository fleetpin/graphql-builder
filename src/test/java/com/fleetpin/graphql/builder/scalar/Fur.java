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
package com.fleetpin.graphql.builder.scalar;

import com.fleetpin.graphql.builder.annotations.GraphQLDescription;
import com.fleetpin.graphql.builder.annotations.Scalar;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

@Scalar(Fur.FurCoercing.class)
@GraphQLDescription("soft")
public class Fur {

	private String input;

	public Fur(String input) {
		this.input = input;
	}

	public String getInput() {
		return input;
	}

	public static class FurCoercing implements Coercing<Fur, Fur> {

		@Override
		public Fur serialize(Object dataFetcherResult) throws CoercingSerializeException {
			return convertImpl(dataFetcherResult);
		}

		@Override
		public Fur parseValue(Object input) throws CoercingParseValueException {
			return convertImpl(input);
		}

		@Override
		public Fur parseLiteral(Object input) throws CoercingParseLiteralException {
			return convertImpl(input);
		}

		private Fur convertImpl(Object input) {
			if (input instanceof Fur) {
				return (Fur) input;
			} else if (input instanceof String) {
				return new Fur((String) input);
			}
			throw new CoercingParseLiteralException();
		}
	}
}
