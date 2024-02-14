package com.fleetpin.graphql.builder;

import graphql.schema.DataFetcher;
import java.lang.reflect.Method;

public interface DataFetcherRunner {
	public DataFetcher<?> manage(Method method, DataFetcher<?> fetcher);
}
