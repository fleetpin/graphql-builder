package com.fleetpin.graphql.builder.type.inheritance;

import java.util.Arrays;
import java.util.List;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;

@Entity
public abstract class Animal{

	public String getName() {
		return "name";
	}
	
	@Query
	public static List<Animal> animals() {
		return Arrays.asList(new Cat(), new Dog());
	}
}