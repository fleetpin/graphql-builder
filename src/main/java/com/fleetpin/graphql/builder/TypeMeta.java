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
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
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

	private Type genericType;
	private Class<?> type;


	private final EntityProcessor entityProcessor;
	private final TypeMeta parent;

	TypeMeta(EntityProcessor entityProcessor, TypeMeta parent, Class<?> type, Type genericType) {
		this.parent = parent;
		this.entityProcessor = entityProcessor;
		flags = new ArrayList<>();
		process(type, genericType);
		Collections.reverse(flags);
	}

	private void processGeneric(TypeMeta target, TypeVariable type) {
		var owningClass = target.genericType;
		if(owningClass == null) {
			owningClass = target.type;
		}
		if(owningClass instanceof Class) {
			findType(target, type, (Class) owningClass);	
		}else if(owningClass instanceof ParameterizedType) {
			var pt = (ParameterizedType) owningClass;
			if(!matchType(target, type.getTypeName(), pt, true)) {
				throw new UnsupportedOperationException("Does not handle type " + owningClass);
			}
		}else {
			throw new UnsupportedOperationException("Does not handle type " + owningClass);
		}
		
		
	}

	private boolean matchType(TypeMeta target, String typeName, ParameterizedType type, boolean parent) {
		var raw = (Class) type.getRawType();
		while(raw != null) {
			for(int i = 0; i < raw.getTypeParameters().length; i++) {
				var arg = type.getActualTypeArguments()[i];
				var param = raw.getTypeParameters()[i];
				if(param.getTypeName().equals(typeName)) {
					if(arg instanceof TypeVariable) {
						if(parent) {
							processGeneric(target.parent, (TypeVariable) arg);
						}else {
							processGeneric(target, (TypeVariable) arg);
						}
						return true;
					}else if(arg instanceof WildcardType) {
						for(var bound: param.getBounds()) {
							if(bound instanceof ParameterizedType) {
								process((Class<?>) ((ParameterizedType)bound).getRawType(), bound);
							}else if(bound instanceof TypeVariable) {
								processGeneric(target, (TypeVariable)bound);
							}else {
								process((Class<?>) bound, null);
							}
						}
						return true;
					}else {
						var klass = (Class) arg;
						process(klass, klass);
						return true;
					}
				}
	
			}
			raw = raw.getSuperclass();
		}
		return false;
	}

	private void findType(TypeMeta target, TypeVariable type, Class start) {
		var startClass = (Class) start;
		var genericDeclaration = type.getGenericDeclaration();
		if(start.equals(genericDeclaration) ) {
			//we don't have any implementing logic we are at this level so take the bounds
			for(var bound: type.getBounds()) {
				if(bound instanceof ParameterizedType) {
					process((Class<?>) ((ParameterizedType)bound).getRawType(), bound);
				}else if(bound instanceof TypeVariable) {
					processGeneric(target, (TypeVariable)bound);
				}else {
					process((Class<?>) bound, null);
				}
			}
		}
		if(startClass.getSuperclass() != null && startClass.getSuperclass().equals(genericDeclaration)) {
			var generic = (ParameterizedType) startClass.getGenericSuperclass();
			if(matchType(target, type.getTypeName(), generic, false)) {
				return;
			}
		}
		for(var inter: startClass.getGenericInterfaces()) {
			if(inter instanceof ParameterizedType) {
				var generic = (ParameterizedType) inter;
				if(generic.getRawType().equals(genericDeclaration)) {
					if(matchType(target, type.getTypeName(), generic, false)) {
						return;
					}
				}
			}
		}
		if(startClass.getSuperclass() != null) {
			findType(target, type, startClass.getSuperclass());
		}

		for(var inter: startClass.getInterfaces()) {
			findType(target, type, inter);
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
				processGeneric(parent, (TypeVariable)genericType);
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
				processGeneric(parent, (TypeVariable)genericType);
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
				processGeneric(parent, (TypeVariable)genericType);
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
				processGeneric(parent, (TypeVariable)genericType);
			}else {
				process((Class<?>) genericType, null);
			}
			return;
		}
		if(genericType != null && genericType instanceof TypeVariable) {
			processGeneric(parent, (TypeVariable) genericType);
			return;
		}
		this.type = type;
		this.genericType = genericType;
	}

	Class<?> getType() {
		return type;
	}
	
	public Type getGenericType() {
		return genericType;
	}

	public List<Flag> getFlags() {
		return flags;
	}

	public String getName() {
		return entityProcessor.process(this);
	}

	public String getInputName() {
		return entityProcessor.processInput(this);
	}

	public boolean hasUnmappedGeneric() {
		if(type.getTypeParameters().length == 0) {
			return false;
		}
		if(genericType == null || !(genericType instanceof ParameterizedType)) {
			return true;
		}
		
		ParameterizedType pt = (ParameterizedType) genericType;
		
		for(var type: pt.getActualTypeArguments()) {
			if(type instanceof TypeVariable) {
				if(resolveToType((TypeVariable) type) == null) {
					return true;
				}
			}else if(!(type instanceof Class)) {
				return true;
			}
		}
		return false;
		
	}

	
	

	public Class resolveToType(TypeVariable variable) {
		var parent = this.parent;
		while(parent  != null) {
			var parentType = parent.getType();
			var pt = parentType.getSuperclass().getTypeParameters();
			for(int i =0; i< pt.length; i++) {
				var p = pt[i];
				if(p.equals(variable)) {
					
					var superClass = (ParameterizedType) parentType.getGenericSuperclass(); //safe as has to if equal vaiable
					var implementingType = superClass.getActualTypeArguments()[i];
					
					if(implementingType instanceof Class) {
						return (Class) implementingType;
					}else {
						throw new RuntimeException("Generics are more complex that logic currently can handle");
					}
					
				}
				
			}
			
			parent = parent.parent;
		}
		return null;
	}


}
