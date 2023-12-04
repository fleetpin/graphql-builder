package com.fleetpin.graphql.builder.annotations;

import graphql.introspection.Introspection.DirectiveLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DirectiveLocations {
    // TODO: Whole class should be temporary as Location can replace
    //  the 'Class<? extends DirectiveOperation<?>> value();' in @Directive

    DirectiveLocation[] value();
}
