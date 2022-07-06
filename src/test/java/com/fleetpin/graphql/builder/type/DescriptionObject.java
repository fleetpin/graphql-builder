
package com.fleetpin.graphql.builder.type;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.GraphQLDescription;
import com.fleetpin.graphql.builder.annotations.Query;

@Entity
@GraphQLDescription("test description comes through")
public class DescriptionObject {

	@GraphQLDescription("first and last")
	public DescriptionObject getName()  {
		return null;
	}
	
	@Query
	@GraphQLDescription("returns something")
	public static DescriptionObject descriptionTest() {
		return new DescriptionObject();
	}
}
