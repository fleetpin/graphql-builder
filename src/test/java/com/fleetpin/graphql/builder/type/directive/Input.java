package com.fleetpin.graphql.builder.type.directive;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.fleetpin.graphql.builder.annotations.Directive;
import graphql.introspection.Introspection;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Directive(Introspection.DirectiveLocation.ARGUMENT_DEFINITION)
@Retention(RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface Input {
	String value();
}
