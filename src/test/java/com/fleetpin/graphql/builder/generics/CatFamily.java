package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;

@Entity
public abstract class CatFamily<R extends CatFamilyFur> extends Animal<R> {

	CatFamily(R fur) {
		super(fur);
	}
}
