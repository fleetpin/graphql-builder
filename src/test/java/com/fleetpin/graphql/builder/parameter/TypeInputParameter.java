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

package com.fleetpin.graphql.builder.parameter;

import java.util.List;
import java.util.Optional;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

public class TypeInputParameter {

	@Entity(SchemaOption.BOTH)
	public static class InputTest {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Query
	public static InputTest requiredType(InputTest type) {
		type.getValue(); // makes sure is right type and not odd runtime passthrough
		return type;
	}

	@Query
	public static Optional<InputTest> optionalType(Optional<InputTest> type) {
		type.map(InputTest::getValue);
		return type;
	}

	@Query
	public static List<InputTest> requiredListType(List<InputTest> type) {
		for (var t : type) {
			t.getValue();
		}
		return type;
	}

	@Query
	public static Optional<List<InputTest>> optionalListType(Optional<List<InputTest>> type) {
		type.map(tt -> tt.stream().map(t -> t.getValue()));
		return type;
	}

	@Query
	public static List<Optional<InputTest>> requiredListOptionalType(List<Optional<InputTest>> type) {
		for (var t : type) {
			t.map(InputTest::getValue);
		}
		return type;
	}

	@Query
	public static Optional<List<Optional<InputTest>>> optionalListOptionalType(
			Optional<List<Optional<InputTest>>> type) {
		type.map(tt -> tt.stream().map(t -> t.map(InputTest::getValue)));
		return type;
	}

}
