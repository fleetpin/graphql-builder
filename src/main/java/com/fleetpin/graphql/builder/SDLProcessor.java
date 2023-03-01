package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.annotations.GraphQLIgnore;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLAppliedDirectiveArgument;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
							try {
								if (method.isSynthetic()) {
									continue;
								}
								if (method.getDeclaringClass().equals(Object.class)) {
									continue;
								}
								if (method.isAnnotationPresent(GraphQLIgnore.class)) {
									continue;
								}
								//will also be on implementing class
								if (Modifier.isAbstract(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
									continue;
								}
								if (Modifier.isStatic(method.getModifiers())) {
									continue;
								} else {
									if (method.getName().matches("(get|is)[A-Z].*") && method.getParameterCount() == 0) {
										String name;
										if (method.getName().startsWith("get")) {
											name =
												method.getName().substring("get".length(), "get".length() + 1).toLowerCase() +
												method.getName().substring("get".length() + 1);
										} else {
											name =
												method.getName().substring("is".length(), "is".length() + 1).toLowerCase() +
												method.getName().substring("is".length() + 1);
										}
										GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
										argument.name(name);
										TypeMeta innerMeta = new TypeMeta(null, method.getReturnType(), method.getGenericReturnType());
										var argumentType = entityProcessor.getEntity(innerMeta).getInputType(innerMeta, method.getAnnotations());
										argument.type(argumentType);
										builder.argument(argument);
										builders.add(object -> {
											try {
												return GraphQLAppliedDirectiveArgument
													.newArgument()
													.name(name)
													.type(argumentType)
													.valueProgrammatic(method.invoke(object))
													.build();
											} catch (IllegalAccessException | InvocationTargetException e) {
												throw new RuntimeException(e);
											}
										});
									}
								}
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
