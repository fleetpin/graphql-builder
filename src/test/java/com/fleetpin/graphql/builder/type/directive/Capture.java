package com.fleetpin.graphql.builder.type.directive;


import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import com.fleetpin.graphql.builder.SDLDirective;
import com.fleetpin.graphql.builder.annotations.Directive;

import graphql.introspection.Introspection.DirectiveLocation;

@Directive(Capture.Processor.class)
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Capture {

	String value();

	static class Processor implements SDLDirective<Capture, CaptureType> {

	
		@Override
		public List<graphql.introspection.Introspection.DirectiveLocation> validLocations() {
			return List.of(DirectiveLocation.FIELD_DEFINITION, DirectiveLocation.SCHEMA);
		}

		@Override
		public CaptureType build(Capture annotation, Class<?> location) {
			return new CaptureType().setColor(annotation.value());
		}

	}

}
