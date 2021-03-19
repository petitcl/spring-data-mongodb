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

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDatabaseUtils;
import org.springframework.data.mongodb.core.convert.ReferenceResolver.ReferenceContext;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.mongodb.client.MongoCollection;

/**
 * @author Christoph Strobl
 * @since 2021/03
 */
public class DefaultReferenceLoader implements ReferenceLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReferenceLoader.class);

	private final MongoDatabaseFactory mongoDbFactory;

	public DefaultReferenceLoader(MongoDatabaseFactory mongoDbFactory) {

		Assert.notNull(mongoDbFactory, "MongoDbFactory translator must not be null!");

		this.mongoDbFactory = mongoDbFactory;
	}

	@Nullable
	@Override
	public Document fetch(Bson filter, ReferenceContext context) {

		MongoCollection<Document> collection = getCollection(context);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Fetching Reference '{}' from {}.{}.", filter,
					StringUtils.hasText(context.getDatabase()) ? context.getDatabase()
							: collection.getNamespace().getDatabaseName(),
					context.getCollection());
		}
		return collection.find(filter).limit(1).first();
	}

	@Override
	public Streamable<Document> bulkFetch(Bson filter, ReferenceContext context) {

		MongoCollection<Document> collection = getCollection(context);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Bulk fetching {} from {}.{}.", filter,
					StringUtils.hasText(context.getDatabase()) ? context.getDatabase()
							: collection.getNamespace().getDatabaseName(),
					context.getCollection());
		}

		return Streamable.of(collection //
				.find(filter));
	}

	protected MongoCollection<Document> getCollection(ReferenceContext context) {

		return MongoDatabaseUtils.getDatabase(context.database, mongoDbFactory).getCollection(context.collection,
				Document.class);
	}
}
