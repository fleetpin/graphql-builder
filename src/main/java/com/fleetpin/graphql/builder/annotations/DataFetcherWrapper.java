package com.fleetpin.graphql.builder.annotations;

import com.fleetpin.graphql.builder.DirectiveOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DataFetcherWrapper {
    Class<? extends DirectiveOperation<?>> value();
}
