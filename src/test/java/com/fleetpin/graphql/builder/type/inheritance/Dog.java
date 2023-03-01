package com.fleetpin.graphql.builder.type.inheritance;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

@Entity(SchemaOption.BOTH)
public class Dog extends Animal {

	private int age = 6;
	private String fur = "shaggy";

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFur() {
		return fur;
	}

	public void setFur(String fur) {
		this.fur = fur;
	}

	@Mutation
	public static Dog getDog() {
		return null;
	}
}
