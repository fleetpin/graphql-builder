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
package com.fleetpin.graphql.builder.type.inheritance;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Mutation;
import com.fleetpin.graphql.builder.annotations.OneOf;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.SchemaOption;
import java.util.Arrays;
import java.util.List;

@Entity(SchemaOption.BOTH)
@OneOf(value = { @OneOf.Type(name = "cat", type = Cat.class), @OneOf.Type(name = "dog", type = Dog.class) })
public abstract class Animal {

	private String name = "name";

	public String getName() {
		return name;
	}

	@Query
	public static List<Animal> animals() {
		return Arrays.asList(new Cat(), new Dog());
	}

	public void setName(String name) {
		this.name = name;
	}

	@Mutation
	public static List<Animal> myAnimals(List<Animal> animals) {
		return animals;
	}
}
