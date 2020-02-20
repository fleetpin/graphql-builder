package com.fleetpin.graphql.builder.publishRestrictions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

import com.fleetpin.graphql.builder.PublishRestrictions;
import com.fleetpin.graphql.builder.RestrictType;
import com.fleetpin.graphql.builder.RestrictTypeFactory;
import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Restrict;
import com.fleetpin.graphql.builder.annotations.Subscription;

import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Flowable;

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
	
	@Subscription
	public static Publisher<Test> test() {
		System.out.println("hi");
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