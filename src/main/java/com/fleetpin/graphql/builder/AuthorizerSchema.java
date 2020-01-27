package com.fleetpin.graphql.builder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Throwables;

import graphql.schema.DataFetcher;

public class AuthorizerSchema {



	private final Set<String> basePackages;
	private final Map<String, Authorizer> targets;

	private AuthorizerSchema(Set<String> basePackages, Map<String, Authorizer> targets) {
		this.basePackages = basePackages;
		this.targets = targets;
	}

	public static AuthorizerSchema build(Set<String> basePackage, Set<Class<? extends Authorizer>> authorizers) throws ReflectiveOperationException {
		Map<String, Authorizer> targets = new HashMap<>();

		for(var type: authorizers) {
			Authorizer auth = type.getDeclaredConstructor().newInstance();
			targets.put(type.getPackageName(), auth);
		}
		return new AuthorizerSchema(basePackage, targets);
	}

	public Authorizer getAuthorizer(Class type) {
		String name = type.getPackageName();
		Authorizer auth = null;
		while(true) {
			auth = targets.get(name);
			if(auth == null) {
				if(basePackages.contains(name)) {
					return null;
				}
				name = name.substring(0, name.lastIndexOf('.'));
			}else {
				return auth;
			}
		}
	}

	public DataFetcher<?> wrap(DataFetcher<?> fetcher, Method method) {
		Authorizer wrapper = getAuthorizer(method.getDeclaringClass());
		if(wrapper == null) {
			return fetcher; 
		}
		Set<String> parameterNames = new HashSet<>();
		
		for(var parameter: method.getParameters()) {
			parameterNames.add(parameter.getName());
		}
		
		
		int longest = 0;
		
		Method[] targets = wrapper.getClass().getMethods();

		for(Method target: targets) {
			boolean valid = false;
			valid |= target.getReturnType() == Boolean.TYPE;
			
			
			if(target.getReturnType().isAssignableFrom(CompletableFuture.class)) {
				Type genericType = ((ParameterizedType) target.getGenericReturnType()).getActualTypeArguments()[0];
				if(Boolean.class.isAssignableFrom((Class<?>) genericType)) {
					valid = true;
				}
			}
			if(!valid) {
				continue;
			}
			if(target.getDeclaringClass().equals(Object.class)) {
				continue;
			}
			int matched = 0;
			for(var parameter: target.getParameters()) {
				if(parameterNames.contains(parameter.getName())) {
					matched++;
				}
			}
			if(matched > longest) {
				longest = matched;
			}
		}
		
		List<Method> toRun = new ArrayList<>();
		for(Method target: targets) {
			
			
			boolean valid = false;
			valid |= target.getReturnType() == Boolean.TYPE;
			
			
			if(target.getReturnType().isAssignableFrom(CompletableFuture.class)) {
				Type genericType = ((ParameterizedType) target.getGenericReturnType()).getActualTypeArguments()[0];
				if(Boolean.class.isAssignableFrom((Class<?>) genericType)) {
					valid = true;
				}
			}
			if(!valid) {
				continue;
			}
			if(target.getDeclaringClass().equals(Object.class)) {
				continue;
			}
			
			int matched = 0;
			for(var parameter: target.getParameters()) {
				if(parameterNames.contains(parameter.getName())) {
					matched++;
				}
			}
			if(matched == longest) {
				toRun.add(target);
			}
		}
		
		if(toRun.isEmpty()) {
			throw new RuntimeException("No authorizer found for " + method);
		}
		
		
		
		
		
		
		
		return env -> {
			for(Method authorizer: toRun) {
				Object[] args = new Object[authorizer.getParameterCount()];
				
				for(int i = 0; i < args.length; i++) {
					if(authorizer.getParameterTypes()[i].isAssignableFrom(env.getClass())) {
						args[i] = env;
					}else if(authorizer.getParameterTypes()[i].isAssignableFrom(env.getContext().getClass())) {
						args[i] = env.getContext();
					}else {
						args[i] = env.getArgument(authorizer.getParameters()[i].getName());
					}
				}
				try {
					Object allow = authorizer.invoke(wrapper, args);
					if(allow instanceof Boolean) {
						if((Boolean) allow) {
							return fetcher.get(env);				
						}else {
							throw new RuntimeException("Invalid access");	
						}
					}else {
						//only other type that passes checks above
						CompletableFuture<Boolean> allowed = (CompletableFuture<Boolean>) allow;
						
						return allowed.handle((r, e) -> {
							if(e != null) {
								if(e.getCause() instanceof Exception) {
									e = e.getCause();
								}
								Throwables.throwIfUnchecked(e);
								throw new RuntimeException(e);
							}
							if(r) {
								try {
									return fetcher.get(env);
								} catch (Throwable e1) {
									if(e1.getCause() instanceof Exception) {
										e1 = e1.getCause();
									}
									Throwables.throwIfUnchecked(e1);
									throw new RuntimeException(e1);
								}				
							}else {
								throw new RuntimeException("Invalid access");
							}
						}).thenCompose(a -> {
							if(a instanceof CompletableFuture) {
								return (CompletableFuture) a;
							}else {
								return CompletableFuture.completedFuture(a);
							}
							
						});
					}
				}catch (InvocationTargetException e) {
					if(e.getCause() instanceof Exception) {
						throw (Exception) e.getCause();
					}else {
						throw e;
					}
				}
			}
			return fetcher.get(env);

		};
	}

}
