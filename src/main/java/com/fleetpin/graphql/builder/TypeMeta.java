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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;


public class TypeMeta {

	enum Flag{ASYNC, ARRAY, OPTIONAL, SUBSCRIPTION}
	
	private List<Flag> flags;
	
	private Class<?> type;
	
	TypeMeta(Class<?> type, Type genericType) {
		flags = new ArrayList<>();
		process(type, genericType);
		Collections.reverse(flags);
	}
	
	private void process(Class<?> type, Type genericType) {
		if(type.isArray()) {
			flags.add(Flag.ARRAY);
			process(type.getComponentType(), null);
			return;
		}

		if(Collection.class.isAssignableFrom(type)) {
			flags.add(Flag.ARRAY);
			genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
			//TODO:cast failure possible??
			process((Class<?>) genericType, null);
			return;
		}
		if(CompletableFuture.class.isAssignableFrom(type)) {
			flags.add(Flag.ASYNC);
			genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
			if(genericType instanceof ParameterizedType) {
				process((Class<?>) ((ParameterizedType)genericType).getRawType(), genericType);
			}else {
				process((Class<?>) genericType, null);
			}
			return;

		}
		
		if(Optional.class.isAssignableFrom(type)) {
			flags.add(Flag.OPTIONAL);
			genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
			if(genericType instanceof ParameterizedType) {
				process((Class<?>) ((ParameterizedType)genericType).getRawType(), genericType);
			}else {
				process((Class<?>) genericType, null);
			}
			return;
		}
		
		
		if(Publisher.class.isAssignableFrom(type)) {
			flags.add(Flag.SUBSCRIPTION);
			genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
			if(genericType instanceof ParameterizedType) {
				process((Class<?>) ((ParameterizedType)genericType).getRawType(), genericType);
			}else {
				process((Class<?>) genericType, null);
			}
			return;
		}
		this.type = type;
	}

	Class<?> getType() {
		return type;
	}
	
	public List<Flag> getFlags() {
		return flags;
	}
	
	
}
