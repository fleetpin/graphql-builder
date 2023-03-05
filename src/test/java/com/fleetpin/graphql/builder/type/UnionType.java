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
