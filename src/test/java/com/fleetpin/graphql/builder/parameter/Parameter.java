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

import com.fleetpin.graphql.builder.annotations.Id;
import com.fleetpin.graphql.builder.annotations.Query;

public class Parameter {

	@Query
	public static String requiredString(String type) {
		return type;
	}

	@Query
	public static Optional<String> optionalString(Optional<String> type) {
		return type;
	}

	@Query
	@Id
	public static String testRequiredId(@Id String type) {
		return type;
	}

	@Query
	@Id
	public static Optional<String> optionalId(@Id Optional<String> type) {
		return type;
	}

	@Query
	public static List<String> requiredListString(List<String> type) {
		return type;
	}

	@Query
	public static Optional<List<String>> optionalListString(Optional<List<String>> type) {
		return type;
	}

	@Query
	public static List<Optional<String>> requiredListOptionalString(List<Optional<String>> type) {
		return type;
	}

	@Query
	public static Optional<List<Optional<String>>> optionalListOptionalString(Optional<List<Optional<String>>> type) {
		return type;
	}

	@Query
	@Id
	public static List<String> requiredListId(@Id List<String> type) {
		return type;
	}

	@Query
	@Id
	public static Optional<List<String>> optionalListId(@Id Optional<List<String>> type) {
		return type;
	}

	@Query
	@Id
	public static List<Optional<String>> requiredListOptionalId(@Id List<Optional<String>> type) {
		return type;
	}

	@Query
	@Id
	public static Optional<List<Optional<String>>> optionalListOptionalId(@Id Optional<List<Optional<String>>> type) {
		return type;
	}
	
	@Query
	public static String multipleArguments(String first, String second) {
		return first + ":" + second;
	}
	
	@Query
	public static String multipleArgumentsOptional(Optional<String> first, Optional<String> second) {
		return first.orElse("") + ":" + second.orElse("");
	}
	
	@Query
	public static String multipleArgumentsMix(String first, Optional<String> second) {
		return first + ":" + second.orElse("");
	}

}
