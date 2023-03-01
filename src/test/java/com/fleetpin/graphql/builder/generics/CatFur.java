package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public class CatFur extends CatFamilyFur {

	public boolean isCalico() {
		return true;
	}
}
