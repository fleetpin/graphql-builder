package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.annotations.DirectiveLocations;
import graphql.introspection.Introspection;
import graphql.schema.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class DirectiveProcessor {

    private final GraphQLDirective directive;
    private final Map<String, Function<Object, GraphQLAppliedDirectiveArgument>> builders;
//    private final Map<String, GraphQLInputType> methodTypes; TODO: Deal with this too

    public DirectiveProcessor(GraphQLDirective directive, Map<String, Function<Object, GraphQLAppliedDirectiveArgument>> builders) {
        this.directive = directive;
        this.builders = builders;
//        this.methodTypes = methodTypes; TODO: DEAL WITH THIS
    }

    public static DirectiveProcessor build(EntityProcessor entityProcessor, Class<? extends Annotation> directive) {
        var processedDirectives = new ArrayList<GraphQLDirective>();


        var builder = GraphQLDirective.newDirective().name(directive.getSimpleName());
        var validLocations = directive.getAnnotation(DirectiveLocations.class).value();
        // loop through and add valid locations
        for (Introspection.DirectiveLocation location : validLocations) {
            builder.validLocation(location);
        }

        // Save method types so when we apply the values later we don't have to go looking for them
        Map<String, GraphQLInputType> methodTypes = new HashMap<>();

        // Go through each argument and add name/type to directive
        var methods = directive.getDeclaredMethods();
        Map<String, Function<Object, GraphQLAppliedDirectiveArgument>> builders = new HashMap<>();
        for (Method method : methods) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            var name = method.getName();

            GraphQLArgument.Builder argument = GraphQLArgument.newArgument();
            argument.name(name);

            // Get the type of the argument from the return type of the method
            TypeMeta innerMeta = new TypeMeta(null, method.getReturnType(), method.getGenericReturnType());
            var argumentType = entityProcessor.getEntity(innerMeta).getInputType(innerMeta, method.getAnnotations());
            argument.type(argumentType);

            // Add the argument to the directive builder to be used for declaration
            builder.argument(argument);

            // Add a builder to the builders list (in order to populate applied directives)
            builders.put(name, object -> {
                try {
                    return GraphQLAppliedDirectiveArgument.newArgument()
                            .name(name)
                            .type(argumentType)
                            .valueProgrammatic(method.invoke(object))
                            .build();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });



        }
        return new DirectiveProcessor(builder.build(), builders);
    }

    public void apply(Annotation annotation, EntityProcessor entityProcessor, Consumer<GraphQLAppliedDirective> builder) throws InvocationTargetException, IllegalAccessException {
        var methods = annotation.annotationType().getDeclaredMethods();

        // Create a new AppliedDirective which we will populate with the set values
        var arguments = GraphQLAppliedDirective.newDirective();
        arguments.name(directive.getName());

        // To get the value we loop through each method and get the method name and value
        for (Method m : methods) {
            // Using the builder created earlier populate the values of each method.
            arguments.argument(builders.get(m.getName()).apply(annotation));
        }

        // Add the argument to the Builder
        builder.accept(arguments.build());
    }

    public GraphQLDirective getDirective() {
        return this.directive;
    }
}
