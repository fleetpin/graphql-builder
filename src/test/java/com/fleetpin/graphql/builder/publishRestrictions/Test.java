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
package com.fleetpin.graphql.builder.publishRestrictions;

import com.fleetpin.graphql.builder.RestrictType;
import com.fleetpin.graphql.builder.RestrictTypeFactory;
import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.Restrict;
import com.fleetpin.graphql.builder.annotations.Subscription;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Flowable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Publisher;

@Entity
@Restrict(Test.Restrictor.class)
public class Test {

	static Executor executor = CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS);
	private boolean value;

	public Test(boolean value) {
		this.value = value;
	}

	public boolean isValue() {
		return value;
	}

	@Query
	public static String MustHaveAQuery() {
		return "String";
	}

	@Subscription
	public static Publisher<Test> test() {
		return Flowable.just(new Test(false)).flatMap(f -> Flowable.fromCompletionStage(CompletableFuture.supplyAsync(() -> f, executor)));
	}

	public static class Restrictor implements RestrictTypeFactory<Test>, RestrictType<Test> {

		@Override
		public CompletableFuture<RestrictType<Test>> create(DataFetchingEnvironment context) {
			return CompletableFuture.supplyAsync(() -> this, executor);
		}

		@Override
		public CompletableFuture<Boolean> allow(Test obj) {
			return CompletableFuture.supplyAsync(() -> false, executor);
		}
	}
}
