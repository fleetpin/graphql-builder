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

package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.mapper.InputTypeBuilder;
import graphql.schema.GraphQLNamedInputType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLTypeReference;

public class ReferenceEntity extends EntityHolder {

	private final GraphQLTypeReference reference;

	ReferenceEntity(String typeName) {
		this.reference = GraphQLTypeReference.typeRef(typeName);
	}

	@Override
	protected GraphQLNamedOutputType buildType() {
		return reference;
	}

	@Override
	protected GraphQLNamedInputType buildInput() {
		return reference;
	}

	@Override
	protected String buildInputName() {
		return reference.getName();
	}

	@Override
	protected InputTypeBuilder buildResolver() {
		return null;
	}
}
