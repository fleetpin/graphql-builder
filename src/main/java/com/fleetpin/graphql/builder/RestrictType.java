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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RestrictType<T> {

	public CompletableFuture<Boolean> allow(T obj);
	
	public default CompletableFuture<List<T>> filter(List<T> list) {
		
		boolean[] keep = new boolean[list.size()];
		
		CompletableFuture<?>[] all = new CompletableFuture[list.size()];
		for(int i = 0; i < list.size(); i++) {
			int offset = i;
			all[i] = allow(list.get(i)).thenAccept(allow -> keep[offset] = allow);
		}
		return CompletableFuture.allOf(all).thenApply(__ -> {
			List<T> toReturn = new ArrayList<>();
			for(int i = 0; i < list.size(); i++) {
				if(keep[i]) {
					toReturn.add(list.get(i));
				}
			}
			return toReturn;
		});
	}
	
	
}
