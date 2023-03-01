package com.fleetpin.graphql.builder.type;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.Union;
import java.util.List;

@Entity
public class UnionType {

	private final Object type;

	public UnionType(Object type) {
		this.type = type;
	}

	@Union({ SimpleType.class, UnionType.class })
	public Object getType() {
		return type;
	}

	@Query
	@Union({ SimpleType.class, UnionType.class })
	public static List<Object> union() {
		return List.of(new SimpleType(), new UnionType(new SimpleType()));
	}

	@Query
	@Union({ SimpleType.class, UnionType.class })
	public static List<Object> unionFailure() {
		return List.of(new UnionType(new UnionType(4d)), false);
	}
}
