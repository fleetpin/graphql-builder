package com.fleetpin.graphql.builder.restrictions;

import com.fleetpin.graphql.builder.RestrictType;
import com.fleetpin.graphql.builder.RestrictTypeFactory;
import com.fleetpin.graphql.builder.restrictions.parameter.RestrictedEntity;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;

public class EntityRestrictions implements RestrictTypeFactory<RestrictedEntity> {

	@Override
	public CompletableFuture<RestrictType<RestrictedEntity>> create(DataFetchingEnvironment context) {
		return CompletableFuture.completedFuture(new DatabaseRestrict());
	}

	public static class DatabaseRestrict implements RestrictType<RestrictedEntity> {

		@Override
		public CompletableFuture<Boolean> allow(RestrictedEntity obj) {
			return CompletableFuture.completedFuture(obj.isAllowed());
		}
	}
}
