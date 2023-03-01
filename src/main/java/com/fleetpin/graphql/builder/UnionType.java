package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.annotations.Union;
import com.fleetpin.graphql.builder.mapper.InputTypeBuilder;
import graphql.schema.GraphQLNamedInputType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;

public class UnionType extends EntityHolder {

	private final EntityProcessor entityProcessor;
	private final Union union;

	public UnionType(EntityProcessor entityProcessor, Union union) {
		this.entityProcessor = entityProcessor;
		this.union = union;
	}

	@Override
	protected GraphQLNamedOutputType buildType() {
		String name = buildInputName();
		var builder = GraphQLUnionType.newUnionType();
		builder.name(name);

		for (var type : union.value()) {
			var possible = entityProcessor.getEntity(type).getInnerType(new TypeMeta(null, type, type));
			builder.possibleType(GraphQLTypeReference.typeRef(possible.getName()));
		}

		entityProcessor
			.getCodeRegistry()
			.typeResolver(
				name,
				env -> {
					for (var type : union.value()) {
						if (type.isInstance(env.getObject())) {
							return (GraphQLObjectType) entityProcessor.getEntity(type).getInnerType(new TypeMeta(null, type, type));
						}
					}
					throw new RuntimeException("Union " + name + " Does not support type " + env.getObject().getClass().getSimpleName());
				}
			);
		return builder.build();
	}

	@Override
	protected GraphQLNamedInputType buildInput() {
		return null;
	}

	@Override
	protected String buildInputName() {
		return name(union);
	}

	static String name(Union union) {
		StringBuilder name = new StringBuilder("Union");

		for (var type : union.value()) {
			name.append("_");
			name.append(EntityUtil.getName(new TypeMeta(null, type, type)));
		}
		return name.toString();
	}

	@Override
	protected InputTypeBuilder buildResolver() {
		return null;
	}
}
