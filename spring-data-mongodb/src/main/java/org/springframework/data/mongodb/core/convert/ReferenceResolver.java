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

import com.mongodb.DBRef;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2021/03
 */
public interface ReferenceResolver {

	@Nullable
	Object resolveReference(MongoPersistentProperty property, Object source, ResolutionContext context);

	@Nullable
	Document fetch(Bson filter, ReferenceContext context);

	Streamable<Document> bulkFetch(Bson filter, ReferenceContext context);

	class ReferenceContext {

		@Nullable final String database;
		final String  collection;

		public ReferenceContext(@Nullable String database, String collection) {
			this.database = database;
			this.collection = collection;
		}

		static ReferenceContext fromDBRef(DBRef dbRef) {
			return new ReferenceContext(dbRef.getDatabaseName(), dbRef.getCollectionName());
		}

		public String getCollection() {
			return collection;
		}

		@Nullable
		public String getDatabase() {
			return database;
		}
	}

	class ResolutionContext {
		ReferenceResolverCallback callback;
		ReferenceProxyHandler proxyHandler;
		OrderFunction orderFunction;
	}

	interface ReferenceResolverCallback {

		/**
		 * Resolve the final object for the given {@link MongoPersistentProperty}.
		 *
		 * @param property will never be {@literal null}.
		 * @return
		 */
		Object resolve(MongoPersistentProperty property);
	}

	interface ReferenceProxyHandler {
		Object populateId(MongoPersistentProperty property, @Nullable Object source, Object proxy);
	}

	interface OrderFunction {
		BiFunction<Object, Document, Document> order();
	}
}
