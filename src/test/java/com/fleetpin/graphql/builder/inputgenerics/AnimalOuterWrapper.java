package com.fleetpin.graphql.builder.inputgenerics;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

@Entity(SchemaOption.BOTH)
public class AnimalOuterWrapper<T extends Animal> {

	String id;
	AnimalWrapper<T> animal;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AnimalWrapper<T> getAnimal() {
		return animal;
	}

	public void setAnimal(AnimalWrapper<T> animal) {
		this.animal = animal;
	}
}
