package com.fleetpin.graphql.builder;

import java.lang.annotation.Annotation;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public interface DirectiveCaller<T extends Annotation> {
	public Object process(T annotation, DataFetchingEnvironment context, DataFetcher<?> fetcher) throws Exception;

}
