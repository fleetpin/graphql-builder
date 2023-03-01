package com.fleetpin.graphql.builder.type.directive;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;

@Entity
public class Cat {

	public boolean isCalico() {
		return true;
	}

	public int getAge() {
		return 3;
	}

	public boolean getFur() {
		return true;
	}

	@Query
	@Capture("meow")
	public static Cat getCat() {
		return new Cat();
	}
}
