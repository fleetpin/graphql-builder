package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.annotations.GraphQLDescription;
import com.fleetpin.graphql.builder.annotations.Scalar;
import com.fleetpin.graphql.builder.mapper.InputTypeBuilder;
import graphql.schema.Coercing;
import graphql.schema.GraphQLNamedInputType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLScalarType;

public class ScalarEntity extends EntityHolder {

	private final GraphQLScalarType scalar;

	public ScalarEntity(GraphQLScalarType scalar) {
		this.scalar = scalar;
	}

	//

	public ScalarEntity(DirectivesSchema directives, TypeMeta meta) throws ReflectiveOperationException {
		GraphQLScalarType.Builder scalarType = GraphQLScalarType.newScalar();
		String typeName = EntityUtil.getName(meta);
		scalarType.name(typeName);

		var type = meta.getType();

		var description = type.getAnnotation(GraphQLDescription.class);
		if (description != null) {
			scalarType.description(description.value());
		}

		Class<? extends Coercing> coerecing = type.getAnnotation(Scalar.class).value();
		scalarType.coercing(coerecing.getDeclaredConstructor().newInstance());

		directives.addSchemaDirective(type, type, scalarType::withAppliedDirective);
		this.scalar = scalarType.build();
	}

	@Override
	protected GraphQLNamedInputType buildInput() {
		return scalar;
	}

	@Override
	protected GraphQLNamedOutputType buildType() {
		return scalar;
	}

	@Override
	protected String buildInputName() {
		return scalar.getName();
	}

	@Override
	public InputTypeBuilder buildResolver() {
		return scalar.getCoercing()::serialize;
	}
}
