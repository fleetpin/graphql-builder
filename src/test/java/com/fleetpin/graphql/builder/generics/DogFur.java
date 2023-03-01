package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public class DogFur extends Fur {

	public boolean isShaggy() {
		return true;
	}

	public String getLong() {
		return "very";
	}
}
