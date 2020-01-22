package com.fleetpin.graphql.builder.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fleetpin.graphql.builder.RestrictTypeFactory;

@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface Restrict {
	Class<? extends RestrictTypeFactory<?>> value();
	
}
