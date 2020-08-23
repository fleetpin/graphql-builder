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

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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

	private final Class<?> owningClass;

	TypeMeta(Class<?> owningClass, Class<?> type, Type genericType) {
		this.owningClass = owningClass;
		flags = new ArrayList<>();
		process(type, genericType);
		Collections.reverse(flags);
	}

	private void processGeneric(TypeVariable type) {
		findType(type, owningClass);
	}

	private boolean matchType(String typeName, ParameterizedType type) {
		var raw = (Class) type.getRawType();
		
		for(int i = 0; i < raw.getTypeParameters().length; i++) {
			var arg = type.getActualTypeArguments()[i];
			var param = raw.getTypeParameters()[i];
			if(param.getTypeName().equals(typeName)) {
				if(arg instanceof TypeVariable) {
					processGeneric((TypeVariable) arg);
					return true;
				}else {
					var klass = (Class) arg;
					process(klass, klass);
					return true;
				}
			}

		}
		return false;
	}

	private void findType(TypeVariable type, Class<?> start) {
		var genericDeclaration = type.getGenericDeclaration();
		if(start.equals(genericDeclaration) ) {
			//we don't have any implementing logic we are at this level so take the bounds
			for(var bound: type.getBounds()) {
				if(bound instanceof ParameterizedType) {
					process((Class<?>) ((ParameterizedType)bound).getRawType(), bound);
				}else if(bound instanceof TypeVariable) {
					processGeneric((TypeVariable)bound);
				}else {
					process((Class<?>) bound, null);
				}
			}
		}
		if(start.getSuperclass() != null && start.getSuperclass().equals(genericDeclaration)) {
			var generic = (ParameterizedType) start.getGenericSuperclass();
			if(matchType(type.getTypeName(), generic)) {
				return;
			}
		}
		for(var inter: start.getGenericInterfaces()) {
			if(inter instanceof ParameterizedType) {
				var generic = (ParameterizedType) inter;
				if(generic.getRawType().equals(genericDeclaration)) {
					if(matchType(type.getTypeName(), generic)) {
						return;
					}
				}
			}
		}
		if(start.getSuperclass() != null) {
			findType(type, start.getSuperclass());
		}

		for(var inter: start.getInterfaces()) {
			findType(type, inter);
		}


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
			if(genericType instanceof ParameterizedType) {
				process((Class<?>) ((ParameterizedType)genericType).getRawType(), genericType);
			}else if(genericType instanceof TypeVariable) {
				processGeneric((TypeVariable)genericType);
			}else {
				process((Class<?>) genericType, null);
			}
			return;
		}
		if(CompletableFuture.class.isAssignableFrom(type)) {
			flags.add(Flag.ASYNC);
			genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
			if(genericType instanceof ParameterizedType) {
				process((Class<?>) ((ParameterizedType)genericType).getRawType(), genericType);
			}else if(genericType instanceof TypeVariable) {
				processGeneric((TypeVariable)genericType);
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
			}else if(genericType instanceof TypeVariable) {
				processGeneric((TypeVariable)genericType);
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
			}else if(genericType instanceof TypeVariable) {
				processGeneric((TypeVariable)genericType);
			}else {
				process((Class<?>) genericType, null);
			}
			return;
		}
		if(genericType != null && genericType instanceof TypeVariable) {
			processGeneric((TypeVariable) genericType);
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
