package com.fleetpin.graphql.builder.generics;

import java.util.Arrays;
import java.util.List;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;

@Entity
public abstract class Animal<T extends Fur>{

	private final T fur;
	
	Animal(T fur) {
		this.fur = fur;
	}

	public String getName() {
		return "name";
	}
	
	public T getFur() {
		return fur;
	}
	
	public List<T> getFurs() {
		return Arrays.asList(fur);
	}
	
	@Query
	public static List<Animal<?>> animals() {
		return Arrays.asList(new Cat(), new Dog());
	}
}