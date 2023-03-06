/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.fleetpin.graphql.builder.scalar;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.fleetpin.graphql.builder.SDLDirective;
import com.fleetpin.graphql.builder.annotations.Directive;
import graphql.introspection.Introspection.DirectiveLocation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

@Directive(Capture.Processor.class)
@Retention(RUNTIME)
@Target({ ElementType.TYPE })
public @interface Capture {
	static class Processor implements SDLDirective<Capture, CaptureType> {

		@Override
		public List<graphql.introspection.Introspection.DirectiveLocation> validLocations() {
			return List.of(DirectiveLocation.FIELD_DEFINITION, DirectiveLocation.SCHEMA);
		}

		@Override
		public CaptureType build(Capture annotation, Class<?> location) {
			return new CaptureType();
		}
	}
}