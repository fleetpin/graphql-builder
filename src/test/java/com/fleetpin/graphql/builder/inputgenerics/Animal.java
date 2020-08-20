package com.fleetpin.graphql.builder.inputgenerics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public abstract class Animal {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
