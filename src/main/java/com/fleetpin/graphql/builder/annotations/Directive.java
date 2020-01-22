package com.fleetpin.graphql.builder.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fleetpin.graphql.builder.DirectiveCaller;

@Retention(RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Directive {
	Class<? extends DirectiveCaller<?>> value();
	
}
