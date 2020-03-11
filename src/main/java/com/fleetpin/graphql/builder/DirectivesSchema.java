package com.fleetpin.graphql.builder;

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



import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.fleetpin.graphql.builder.annotations.Directive;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Flowable;

public class DirectivesSchema {

	private final Collection<RestrictTypeFactory<?>> global;
	private final Map<Class<? extends Annotation>, DirectiveCaller<?>> targets;

	private DirectivesSchema(Collection<RestrictTypeFactory<?>> global, Map<Class<? extends Annotation>, DirectiveCaller<?>> targets) {
		this.global = global;
		this.targets = targets;
	}
	//TODO:mess of exceptions
	public static DirectivesSchema build(List<RestrictTypeFactory<?>> globalDirectives, Set<Class<?>> dierctiveTypes) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Map<Class<? extends Annotation>, DirectiveCaller<?>> targets = new HashMap<>();
		for(Class<?> directiveType: dierctiveTypes) {
			if(!directiveType.isAnnotation()) {
				//TODO:better error management
				throw new RuntimeException("@Diretive Annotation must only be placed on annotations");
			}
			
			Directive directive = directiveType.getAnnotation(Directive.class);
			Class<? extends DirectiveCaller> caller = directive.value();
			//TODO: if target implents other things this won't lineup right
			Class<?> target = (Class<?>) ((ParameterizedType) caller.getGenericInterfaces()[0]).getActualTypeArguments()[0];
			if(!target.equals(directiveType)) {
				//TODO:better errors
				throw new RuntimeException("Annotation missmatch");		
			}
			
			
			
			//TODO error for no zero args constructor
			DirectiveCaller<?> callerInstance = caller.getConstructor().newInstance();
			targets.put((Class<? extends Annotation>) directiveType, callerInstance);
		}
		
		return new DirectivesSchema(globalDirectives, targets);
	}
	private DirectiveCaller<?> get(Annotation annotation) {
		return targets.get(annotation.annotationType());
	}
	private <T extends Annotation> DataFetcher<?> wrap(DirectiveCaller<T> directive, T annotation, DataFetcher<?> fetcher) {
		return env -> {
			return directive.process(annotation, env, fetcher);
		};
	}
	private DataFetcher<?> wrap(RestrictTypeFactory<?> directive, DataFetcher<?> fetcher) {
		//TODO: hate having this cache here would love to scope against the env object but nothing to hook into dataload caused global leak
		Map<DataFetchingEnvironment, CompletableFuture<RestrictType>> cache = Collections.synchronizedMap(new WeakHashMap<>());
		
		
		return env -> {
			return cache.computeIfAbsent(env, key -> directive.create(key).thenApply(t -> t)).thenCompose(restrict -> {
				try {
					Object response = fetcher.get(env);
					if(response instanceof CompletionStage) {
						return ((CompletionStage) response).thenCompose(r -> applyRestrict(restrict, r	));
					}
					return applyRestrict(restrict, response);
				} catch (Exception e) {
					if(e instanceof RuntimeException) {
						throw (RuntimeException) e;
					}
					throw new RuntimeException(e);
				}
			});
		};
	}

	public boolean target(Method method, TypeMeta meta) {
		
		for(var global: this.global) {
			//TODO: extract class
			if(meta.getType().equals(global.extractType())) {
				return true;
			}
		}
		for(Annotation annotation: method.getAnnotations()) {
			if(get(annotation) != null) {
				return true;
			}
		}
		return false;
	}
	public DataFetcher<?> wrap(Method method,  TypeMeta meta, DataFetcher<?> fetcher) {
		for(var g: global) {
			if(meta.getType().equals(g.extractType())) {
				fetcher = wrap(g, fetcher);
			}
		}
		for(Annotation annotation: method.getAnnotations()) {
			DirectiveCaller directive = (DirectiveCaller) get(annotation);
			if(directive != null) {
				fetcher =  wrap(directive, annotation, fetcher);
			}
		}
		return fetcher;
	}
	
	private <T> CompletableFuture<Object> applyRestrict(RestrictType restrict, Object response) {
		if(response instanceof List) {
			return restrict.filter((List)response);
		}else if(response instanceof Publisher) {
			return CompletableFuture.completedFuture(Flowable.fromPublisher((Publisher) response).flatMap(entry -> {
				return Flowable.fromCompletionStage(restrict.allow(entry)).filter(t -> t == Boolean.TRUE).map(t -> entry);
			}));
		}else if(response instanceof Optional) {
			var optional = (Optional) response;
			if(optional.isEmpty()) {
				return CompletableFuture.completedFuture(response);
			}
			return restrict.allow(optional.get()).thenApply(allow -> {
				if(allow == Boolean.TRUE) {
					return response;
				}else {
					return Optional.empty();
				}
			});
		}else {
			return restrict.allow(response).thenApply(allow -> {
				if(allow == Boolean.TRUE) {
					return response;
				}else {
					return null;
				}
			});
		}
	}
	
	private static <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> toReturn) {
		return CompletableFuture.allOf(toReturn.toArray(CompletableFuture[]::new))
				.thenApply(__ -> toReturn.stream().map(m -> {
					try {
						return m.get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				}).collect(Collectors.toList()));
	}
	
}
