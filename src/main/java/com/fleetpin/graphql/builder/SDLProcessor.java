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
package com.fleetpin.graphql.builder;

import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLAppliedDirectiveArgument;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SDL directives are applied directly to the schema. They can be used to export
 * extra information about methods By default graphql will not export this
 * information but it can be enabled with new
 * IntrospectionWithDirectivesSupport()
 */
public class SDLProcessor {

	private final SDLDirective<Annotation, ?> factory;
	private final GraphQLDirective directive;
	private final List<Function<Object, GraphQLAppliedDirectiveArgument>> builders;

	public SDLProcessor(SDLDirective<?, ?> factory, GraphQLDirective directive, List<Function<Object, GraphQLAppliedDirectiveArgument>> builders) {
		this.factory = (SDLDirective<Annotation, ?>) factory;
		this.directive = directive;
		this.builders = builders;
	}

	public static SDLProcessor build(EntityProcessor entityProcessor, SDLDirective<?, ?> directive) {
		for (var inter : directive.getClass().getAnnotatedInterfaces()) {
			if (inter.getType() instanceof ParameterizedType) {
				var type = (ParameterizedType) inter.getType();
				if (type.getRawType() instanceof Class) {
					var rawType = (Class) type.getRawType();
					if (SDLDirective.class.isAssignableFrom(rawType)) {
						var annotation = (Class) type.getActualTypeArguments()[0];
						var arguments = (Class) type.getActualTypeArguments()[1];

						var builder = GraphQLDirective.newDirective().name(annotation.getSimpleName());
						for (var location : directive.validLocations()) {
							builder.validLocation(location);
						}
						builder.repeatable(directive.repeatable());
						List<Function<Object, GraphQLAppliedDirectiveArgument>> builders = new ArrayList<>();
						for (Method method : arguments.getMethods()) {
							if (method.getParameterCount() != 0) {
								continue;
							}
							try {
								var name = EntityUtil.getter(method);
								name.ifPresent(n -> {
									GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
									argument.name(n);
									TypeMeta innerMeta = new TypeMeta(null, method.getReturnType(), method.getGenericReturnType());
									var argumentType = entityProcessor.getEntity(innerMeta).getInputType(innerMeta, method.getAnnotations());
									argument.type(argumentType);
									builder.argument(argument);
									builders.add(object -> {
										try {
											return GraphQLAppliedDirectiveArgument
												.newArgument()
												.name(n)
												.type(argumentType)
												.valueProgrammatic(method.invoke(object))
												.build();
										} catch (IllegalAccessException | InvocationTargetException e) {
											throw new RuntimeException(e);
										}
									});
								});
							} catch (RuntimeException e) {
								throw new RuntimeException("Failed to process method " + method, e);
							}
						}
						return new SDLProcessor(directive, builder.build(), builders);
					}
				}
			}
		}
		return null;
	}

	public void apply(Annotation annotation, Class<?> location, Consumer<GraphQLAppliedDirective> builder) {
		var built = factory.build(annotation, location);
		if (built != null) {
			// need to call the methods building out the arguments
			var arguments = GraphQLAppliedDirective.newDirective();
			arguments.name(directive.getName());
			for (var b : this.builders) {
				arguments.argument(b.apply(built));
			}
			builder.accept(arguments.build());
		}
	}

	public GraphQLDirective getDirective() {
		return directive;
	}
}
