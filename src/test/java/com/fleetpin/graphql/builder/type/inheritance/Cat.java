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
import com.fleetpin.graphql.builder.annotations.SchemaOption;
import java.util.Optional;

@Entity(SchemaOption.BOTH)
public class Cat extends Animal {

	private boolean calico;
	private int age;
	private Optional<Boolean> fur;

	public Cat() {
		calico = true;
		age = 3;
		fur = Optional.of(true);
	}

	public Cat(boolean calico, int age, Optional<Boolean> fur) {
		super();
		this.calico = calico;
		this.age = age;
		this.fur = fur;
	}

	public boolean isCalico() {
		return calico;
	}

	public void setCalico(boolean calico) {
		this.calico = calico;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Optional<Boolean> getFur() {
		return fur;
	}

	public void setFur(Optional<Boolean> fur) {
		this.fur = fur;
	}

	public void setError(Optional<String> ignore) {
		throw new RuntimeException("ERROR");
	}

	@Mutation
	public static Cat getCat() {
		return null;
	}
}
