package com.fleetpin.graphql.builder.type.directive;

import com.fleetpin.graphql.builder.annotations.Directive;
import com.fleetpin.graphql.builder.annotations.DirectiveLocations;
import graphql.introspection.Introspection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Directive
@Retention(RUNTIME)
@Target({ ElementType.PARAMETER })
@DirectiveLocations(Introspection.DirectiveLocation.ARGUMENT_DEFINITION)
public @interface Input {

    String value();
}
