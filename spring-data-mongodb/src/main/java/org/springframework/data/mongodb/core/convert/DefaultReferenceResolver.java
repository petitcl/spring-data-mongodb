/*
 * Copyright 2021. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core.convert;

import java.util.function.BiFunction;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.mapping.ManualReference;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2021/03
 */
public class DefaultReferenceResolver implements ReferenceResolver {

	private final ReferenceReader referenceReader;
	private final LazyLoadingProxyGenerator proxyGenerator;

	public DefaultReferenceResolver(ReferenceReader referenceLoader) {

		this.referenceReader = referenceLoader;
		this.proxyGenerator = new LazyLoadingProxyGenerator(referenceReader);
	}

	@Nullable
	@Override
	public Object resolveReference(MongoPersistentProperty property, Object source,
			BiFunction<ReferenceContext, Bson, Streamable<Document>> lookupFunction) {

		if (isLazyReference(property)) {
			return createLazyLoadingProxy(property, source, lookupFunction);
		}

		return referenceReader.readReference(property, source, lookupFunction);
	}

	private Object createLazyLoadingProxy(MongoPersistentProperty property, Object source,
			BiFunction<ReferenceContext, Bson, Streamable<Document>> lookupFunction) {
		return proxyGenerator.createLazyLoadingProxy(property, source, lookupFunction);
	}

	protected boolean isLazyReference(MongoPersistentProperty property) {

		if(property.findAnnotation(ManualReference.class) != null) {
			return property.findAnnotation(ManualReference.class).lazy();
		}

		return property.getDBRef() != null && property.getDBRef().lazy();
	}
}
