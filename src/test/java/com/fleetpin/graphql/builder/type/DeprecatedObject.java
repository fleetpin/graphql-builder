package com.fleetpin.graphql.builder.type;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.GraphQLDeprecated;
import com.fleetpin.graphql.builder.annotations.Query;

@Entity
public class DeprecatedObject {

	@GraphQLDeprecated("spelling")
	public DeprecatedObject getNaame() {
		return null;
	}

	@Query
	@GraphQLDeprecated("old")
	public static DeprecatedObject deprecatedTest() {
		return new DeprecatedObject();
	}
}
