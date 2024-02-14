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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class FilteredPublisher<T> implements Publisher<T> {

	private final RestrictType<T> restrict;
	private final Publisher<T> publisher;

	public FilteredPublisher(Publisher<T> publisher, RestrictType<T> restrict) {
		this.publisher = publisher;
		this.restrict = restrict;
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		publisher.subscribe(
			new Subscriber<T>() {
				private CompletableFuture<?> current = CompletableFuture.completedFuture(null);
				private Subscription s;

				@Override
				public void onSubscribe(Subscription s) {
					this.s = s;
					subscriber.onSubscribe(s);
				}

				@Override
				public void onNext(T t) {
					synchronized (this) {
						current = current.thenCompose(__ -> restrict.allow(t)).whenComplete(process(subscriber, t));
					}
				}

				private BiConsumer<Boolean, Throwable> process(Subscriber<? super T> subscriber, T t) {
					return (allow, error) -> {
						if (error != null) {
							subscriber.onError(error);
							return;
						}
						if (allow) {
							subscriber.onNext(t);
						} else {
							s.request(1);
						}
					};
				}

				@Override
				public void onError(Throwable t) {
					synchronized (this) {
						current =
							current.whenComplete((__, error) -> {
								if (error != null) {
									subscriber.onError(error);
								}
								subscriber.onError(t);
							});
					}
				}

				@Override
				public void onComplete() {
					synchronized (this) {
						current =
							current.whenComplete((__, error) -> {
								if (error != null) {
									subscriber.onError(error);
								}
								subscriber.onComplete();
							});
					}
				}
			}
		);
	}
}
