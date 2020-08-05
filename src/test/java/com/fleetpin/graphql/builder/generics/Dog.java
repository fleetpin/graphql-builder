package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public class Dog extends Animal<DogFur> {

	public Dog() {
		super(new DogFur());
	}

	public int getAge() {
		return 6;
	}
}
