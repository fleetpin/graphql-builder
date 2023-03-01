package com.fleetpin.graphql.builder.generics;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.Query;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Entity
public abstract class Animal<T extends Fur> {

	private final T fur;

	Animal(T fur) {
		this.fur = fur;
	}

	public String getName() {
		return "name";
	}

	public T getFur() {
		return fur;
	}

	public List<T> getFurs() {
		return Arrays.asList(fur);
	}

	@Query
	public static List<Animal<?>> animals() {
		return Arrays.asList(new Cat(), new Dog());
	}

	@Mutation
	public static MutationResponse makeCat() {
		return new GenericMutationResponse<>(Optional.of(new Cat()));
	}

	@Entity
	public abstract static class MutationResponse<T extends Fur> {

		private Optional<Animal<T>> item;

		public MutationResponse(Optional<Animal<T>> item) {
			this.item = item;
		}

		public Optional<Animal<T>> getItem() {
			return item;
		}
	}

	@Entity
	public static class GenericMutationResponse<T extends Fur> extends MutationResponse<T> {

		public GenericMutationResponse(Optional<Animal<T>> item) {
			super(item);
		}
	}
}
