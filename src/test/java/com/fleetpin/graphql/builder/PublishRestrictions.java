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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;

public class PublishRestrictions {

	
	@Test
	public void testOptionalArray() throws ReflectiveOperationException {
		var schema = SchemaBuilder.build("com.fleetpin.graphql.builder.publishRestrictions").build();
		var res = schema.execute("subscription {test {value}} ");
		Publisher<Test> response = res.getData();
		assertEquals(0, Flowable.fromPublisher(response).count().blockingGet());
	}
	
	
	
	
}
