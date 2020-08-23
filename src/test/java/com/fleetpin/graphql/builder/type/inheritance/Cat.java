package com.fleetpin.graphql.builder.type.inheritance;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;

@Entity
public class Cat extends Animal{

	public boolean isCalico() {
		return true;
	}
	
	public int getAge() {
		return 3;
	}
	
	public boolean getFur() {
		return true;
	}
	
	@Mutation
	public static Cat getCat() {
		return null;
	}
}

