package com.fleetpin.graphql.builder.inputgenerics;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

@Entity(SchemaOption.BOTH)
public class Cat extends Animal {
	
	private boolean fur;
	
	
	public void setFur(boolean fur) {
		this.fur = fur;
	}
	
	public boolean isFur() {
		return fur;
	}
	
	@Mutation
	public static String addCat(CatAnimalInput input) {
		return input.getAnimal().getName();
	}
	
	
	@Mutation
	public static boolean addCatGenerics(AnimalInput<Cat> input) {
		return input.getAnimal().isFur();
	}

}
