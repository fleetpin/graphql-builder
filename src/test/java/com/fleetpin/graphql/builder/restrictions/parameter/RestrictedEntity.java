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

package com.fleetpin.graphql.builder.restrictions.parameter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fleetpin.graphql.builder.annotations.Entity;
import com.fleetpin.graphql.builder.annotations.Query;
import com.fleetpin.graphql.builder.annotations.Restrict;
import com.fleetpin.graphql.builder.restrictions.EntityRestrictions;

@Entity
@Restrict(EntityRestrictions.class)
public class RestrictedEntity {

	private boolean allowed;

	public boolean isAllowed() {
		return allowed;
	}

	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}

	
	@Query
	public static RestrictedEntity single(Boolean allowed) {
		RestrictedEntity entity = new RestrictedEntity();
		entity.setAllowed(allowed);
		return entity;
	}
	
	@Query
	public static Optional<RestrictedEntity> singleOptional(Optional<Boolean> allowed) {
		if (allowed.isEmpty()) return Optional.empty();
		RestrictedEntity entity = new RestrictedEntity();
		entity.setAllowed(allowed.get());
		return Optional.of(entity);
	}
	
	@Query
	public static List<RestrictedEntity> list(List<Boolean> allowed) {
		return allowed.stream().map(isAllowed -> {
			RestrictedEntity entity = new RestrictedEntity();
			entity.setAllowed(isAllowed);
			return entity; 
		}).collect(Collectors.toList());
	}
	
	@Query
	public static Optional<List<RestrictedEntity>> listOptional(Optional<List<Boolean>> allowed) {
		if (allowed.isEmpty()) return Optional.empty();
		
		return Optional.of(allowed.get().stream().map(isAllowed -> {
			RestrictedEntity entity = new RestrictedEntity();
			entity.setAllowed(isAllowed);
			return entity;
		}).collect(Collectors.toList()));
	}
	
}
