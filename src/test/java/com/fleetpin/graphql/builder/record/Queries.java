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
package com.fleetpin.graphql.builder.record;

import com.fleetpin.graphql.builder.annotations.GraphQLDescription;
import com.fleetpin.graphql.builder.annotations.Query;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class Queries {

	@Query
	public static InputType passthrough(InputType type) {
		return type;
	}

	@Query
	@Nullable
	public static Boolean nullableTest(@Nullable Boolean type) {
		return type;
	}

	@Query
	@Nullable
	public static List<Boolean> nullableArrayTest(@Nullable List<Boolean> type) {
		return type;
	}

	// once move to java 17 change this to be a real record
	@GraphQLDescription("record Type")
	static final class InputType {

		private final String name;
		private final int age;
		private final Optional<Integer> weight;

		private InputType(@GraphQLDescription("the name") String name, int age, Optional<Integer> weight) {
			super();
			this.name = name;
			this.age = age;
			this.weight = weight;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}

		public Optional<Integer> getWeight() {
			return weight;
		}
	}
}
