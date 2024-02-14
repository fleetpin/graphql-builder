/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.fleetpin.graphql.builder.inputgenerics;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.SchemaOption;

@Entity(SchemaOption.BOTH)
public class Cat extends Animal {

	private boolean fur;

	public void setFur(boolean fur) {
		this.fur = fur;
	}

	public boolean isFur() {
		return fur;
	}

	@Query
	public static String getCat() {
		return "cat";
	}

	@Mutation
	public static String addCat(CatAnimalInput input) {
		return input.getAnimal().getName();
	}

	@Mutation
	public static boolean addCatGenerics(AnimalInput<Cat> input) {
		return input.getAnimal().isFur();
	}

	@Mutation
	public static AnimalOuterWrapper<Cat> addNestedGenerics(AnimalInput<Cat> input) {
		var wrapper = new AnimalOuterWrapper<Cat>();
		wrapper.setAnimal(new AnimalWrapper<Cat>());
		wrapper.getAnimal().setAnimal(input.getAnimal());
		return wrapper;
	}
}
