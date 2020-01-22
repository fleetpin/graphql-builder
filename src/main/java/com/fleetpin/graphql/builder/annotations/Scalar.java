package com.fleetpin.graphql.builder.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import graphql.schema.Coercing;

@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface Scalar {
	Class<? extends Coercing<?, ?>> value();

}
