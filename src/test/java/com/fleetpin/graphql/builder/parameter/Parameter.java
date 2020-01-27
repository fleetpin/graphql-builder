package com.fleetpin.graphql.builder.parameter;

import java.util.List;
import java.util.Optional;

import com.fleetpin.graphql.builder.annotations.Query;

public class Parameter {

	@Query
	public static List<String> test(Optional<List<String>> type) {
		return type.orElse(null);
	}
	
}
