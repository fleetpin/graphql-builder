package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;

@Entity
public class Cat extends CatFamily<CatFur> {

	public Cat() {
		super(new CatFur());
	}

	@Mutation
	public static Cat getCat() {
		return null;
	}
}
