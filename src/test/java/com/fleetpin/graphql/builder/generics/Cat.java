package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public class Cat extends CatFamily<CatFur>{

	public Cat() {
		super(new CatFur());
	}
	
}
