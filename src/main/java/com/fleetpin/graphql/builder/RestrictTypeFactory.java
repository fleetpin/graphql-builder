package com.fleetpin.graphql.builder;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;

import graphql.schema.DataFetchingEnvironment;

public interface RestrictTypeFactory<T> {
	public CompletableFuture<RestrictType<T>> create(DataFetchingEnvironment context);

	default Class<T> extractType() {
		for(var inter: getClass().getGenericInterfaces()) {
			if(inter instanceof ParameterizedType) {
				var param = (ParameterizedType) inter;
				if(RestrictTypeFactory.class.equals(param.getRawType())) {
					return (Class<T>) param.getActualTypeArguments()[0];
				}
			}
		}
		return null;
	}
}
