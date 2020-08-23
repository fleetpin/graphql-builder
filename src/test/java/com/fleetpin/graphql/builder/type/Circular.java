
package com.fleetpin.graphql.builder.type;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;

@Entity
public class Circular {

	public Circular getCircular()  {
		return null;
	}
	
	
	@Mutation
	public static Circular circularTest() {
		return new Circular();
	}
}
