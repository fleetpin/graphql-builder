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
package com.fleetpin.graphql.builder.type;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Entity
public class SimpleType {

	public String getName() {
		return "green";
	}

	public boolean isDeleted() {
		return false;
	}

	public Optional<Boolean> getAlive() {
		return Optional.empty();
	}

	public List<String> getParts() {
		return Arrays.asList("green", "eggs");
	}

	public List<Optional<String>> getGappyParts() {
		return Arrays.asList(Optional.empty(), Optional.of("eggs"));
	}

	public Optional<List<String>> getOptionalParts() {
		return Optional.empty();
	}

	public Optional<List<Optional<String>>> getOptionalGappyParts() {
		return Optional.of(Arrays.asList());
	}

	public CompletableFuture<String> getNameFuture() {
		return CompletableFuture.completedFuture("green");
	}

	public CompletableFuture<Boolean> isDeletedFuture() {
		return CompletableFuture.completedFuture(false);
	}

	public CompletableFuture<Optional<Boolean>> getAliveFuture() {
		return CompletableFuture.completedFuture(Optional.of(false));
	}

	public CompletableFuture<List<String>> getPartsFuture() {
		return CompletableFuture.completedFuture(Arrays.asList());
	}

	public CompletableFuture<List<Optional<String>>> getGappyPartsFuture() {
		return CompletableFuture.completedFuture(Arrays.asList());
	}

	public CompletableFuture<Optional<List<String>>> getOptionalPartsFuture() {
		return CompletableFuture.completedFuture(Optional.of(Arrays.asList()));
	}

	public CompletableFuture<Optional<List<Optional<String>>>> getOptionalGappyPartsFuture() {
		return CompletableFuture.completedFuture(Optional.empty());
	}

	@Query
	public static SimpleType simpleType() {
		return new SimpleType();
	}
}
