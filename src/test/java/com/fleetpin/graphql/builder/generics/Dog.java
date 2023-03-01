package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;

@Entity
public class Dog extends Animal<DogFur> {

	public Dog() {
		super(new DogFur());
	}

	public int getAge() {
		return 6;
	}

	@Mutation
	public static Dog getDog() {
		return null;
	}
}
