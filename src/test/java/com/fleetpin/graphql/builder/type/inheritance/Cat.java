package com.fleetpin.graphql.builder.type.inheritance;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

@Entity(SchemaOption.BOTH)
public class Cat extends Animal {

	private boolean calico;
	private int age;
	private boolean fur;

	public Cat() {
		calico = true;
		age = 3;
		fur = true;
	}

	public Cat(boolean calico, int age, boolean fur) {
		super();
		this.calico = calico;
		this.age = age;
		this.fur = fur;
	}

	public boolean isCalico() {
		return calico;
	}

	public void setCalico(boolean calico) {
		this.calico = calico;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isFur() {
		return fur;
	}

	public void setFur(boolean fur) {
		this.fur = fur;
	}

	@Mutation
	public static Cat getCat() {
		return null;
	}
}
