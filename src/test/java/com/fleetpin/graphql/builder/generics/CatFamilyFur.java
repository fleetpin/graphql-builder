package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public abstract class CatFamilyFur extends Fur {

	public boolean isLong() {
		return true;
	}
}
