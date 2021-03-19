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
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.mongodb.core.query.Criteria.*;
import static org.springframework.data.mongodb.core.query.Query.*;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.convert.LazyLoadingTestUtils;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.ManualReference;
import org.springframework.data.mongodb.core.mapping.ObjectReference;
import org.springframework.data.mongodb.test.util.Client;
import org.springframework.data.mongodb.test.util.MongoClientExtension;
import org.springframework.data.mongodb.test.util.MongoTestTemplate;
import org.springframework.lang.Nullable;

import com.mongodb.client.MongoClient;

/**
 * {@link DBRef} related integration tests for {@link MongoTemplate}.
 *
 * @author Christoph Strobl
 */
@ExtendWith(MongoClientExtension.class)
public class MongoTemplateManualReferenceTests {

	public static final String DB_NAME = "manual-reference-tests";

	static @Client MongoClient client;

	MongoTestTemplate template = new MongoTestTemplate(cfg -> {

		cfg.configureDatabaseFactory(it -> {

			it.client(client);
			it.defaultDb(DB_NAME);
		});

		cfg.configureConversion(it -> {
			it.customConverters(new ReferencableConverter());
		});

		cfg.configureMappingContext(it -> {
			it.autocreateIndex(false);
		});
	});

	@BeforeEach
	public void setUp() {
		template.flushDatabase();
	}

	@Test
	void readSimpleTypeObjectReference() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = template.getCollectionName(SimpleObjectRef.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("simpleValueRef", "ref-1");

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);
		assertThat(result.getSimpleValueRef()).isEqualTo(new SimpleObjectRef("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readCollectionOfSimpleTypeObjectReference() {

		String rootCollectionName = template.getCollectionName(CollectionRefRoot.class);
		String refCollectionName = template.getCollectionName(SimpleObjectRef.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("simpleValueRef",
				Collections.singletonList("ref-1"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		CollectionRefRoot result = template.findOne(query(where("id").is("id-1")), CollectionRefRoot.class);
		assertThat(result.getSimpleValueRef()).containsExactly(new SimpleObjectRef("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readLazySimpleTypeObjectReference() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = template.getCollectionName(SimpleObjectRef.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("simpleLazyValueRef", "ref-1");

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);

		LazyLoadingTestUtils.assertProxy(result.simpleLazyValueRef, (proxy) -> {

			assertThat(proxy.isResolved()).isFalse();
			assertThat(proxy.currentValue()).isNull();
		});
		assertThat(result.getSimpleLazyValueRef()).isEqualTo(new SimpleObjectRef("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readSimpleTypeObjectReferenceFromFieldWithCustomName() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = template.getCollectionName(SimpleObjectRef.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1")
				.append("simple-value-ref-annotated-field-name", "ref-1");

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);
		assertThat(result.getSimpleValueRefWithAnnotatedFieldName())
				.isEqualTo(new SimpleObjectRef("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readCollectionTypeObjectReferenceFromFieldWithCustomName() {

		String rootCollectionName = template.getCollectionName(CollectionRefRoot.class);
		String refCollectionName = template.getCollectionName(SimpleObjectRef.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1")
				.append("simple-value-ref-annotated-field-name", Collections.singletonList("ref-1"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		CollectionRefRoot result = template.findOne(query(where("id").is("id-1")), CollectionRefRoot.class);
		assertThat(result.getSimpleValueRefWithAnnotatedFieldName())
				.containsExactly(new SimpleObjectRef("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readObjectReferenceFromDocumentType() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = template.getCollectionName(ObjectRefOfDocument.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("objectValueRef",
				new Document("id", "ref-1").append("property", "without-any-meaning"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);
		assertThat(result.getObjectValueRef()).isEqualTo(new ObjectRefOfDocument("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readCollectionObjectReferenceFromDocumentType() {

		String rootCollectionName = template.getCollectionName(CollectionRefRoot.class);
		String refCollectionName = template.getCollectionName(ObjectRefOfDocument.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("objectValueRef",
				Collections.singletonList(new Document("id", "ref-1").append("property", "without-any-meaning")));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		CollectionRefRoot result = template.findOne(query(where("id").is("id-1")), CollectionRefRoot.class);
		assertThat(result.getObjectValueRef())
				.containsExactly(new ObjectRefOfDocument("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readObjectReferenceFromDocumentDeclaringCollectionName() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = "object-ref-of-document-with-embedded-collection-name";
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append(
				"objectValueRefWithEmbeddedCollectionName",
				new Document("id", "ref-1").append("collection", "object-ref-of-document-with-embedded-collection-name")
						.append("property", "without-any-meaning"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);
		assertThat(result.getObjectValueRefWithEmbeddedCollectionName())
				.isEqualTo(new ObjectRefOfDocumentWithEmbeddedCollectionName("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readCollectionObjectReferenceFromDocumentDeclaringCollectionName() {

		String rootCollectionName = template.getCollectionName(CollectionRefRoot.class);
		String refCollectionName = "object-ref-of-document-with-embedded-collection-name";
		org.bson.Document refSource = new Document("_id", "ref-1").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append(
				"objectValueRefWithEmbeddedCollectionName",
				Collections.singletonList(
						new Document("id", "ref-1").append("collection", "object-ref-of-document-with-embedded-collection-name")
								.append("property", "without-any-meaning")));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		CollectionRefRoot result = template.findOne(query(where("id").is("id-1")), CollectionRefRoot.class);
		assertThat(result.getObjectValueRefWithEmbeddedCollectionName())
				.containsExactly(new ObjectRefOfDocumentWithEmbeddedCollectionName("ref-1", "me-the-referenced-object"));
	}

	@Test
	void readObjectReferenceFromDocumentNotRelatingToTheIdProperty() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = template.getCollectionName(ObjectRefOnNonIdField.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("refKey1", "ref-key-1")
				.append("refKey2", "ref-key-2").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("objectValueRefOnNonIdFields",
				new Document("refKey1", "ref-key-1").append("refKey2", "ref-key-2").append("property", "without-any-meaning"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);
		assertThat(result.getObjectValueRefOnNonIdFields())
				.isEqualTo(new ObjectRefOnNonIdField("ref-1", "me-the-referenced-object", "ref-key-1", "ref-key-2"));
	}

	@Test
	void readLazyObjectReferenceFromDocumentNotRelatingToTheIdProperty() {

		String rootCollectionName = template.getCollectionName(SingleRefRoot.class);
		String refCollectionName = template.getCollectionName(ObjectRefOnNonIdField.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("refKey1", "ref-key-1")
				.append("refKey2", "ref-key-2").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("lazyObjectValueRefOnNonIdFields",
				new Document("refKey1", "ref-key-1").append("refKey2", "ref-key-2").append("property", "without-any-meaning"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		SingleRefRoot result = template.findOne(query(where("id").is("id-1")), SingleRefRoot.class);

		LazyLoadingTestUtils.assertProxy(result.lazyObjectValueRefOnNonIdFields, (proxy) -> {

			assertThat(proxy.isResolved()).isFalse();
			assertThat(proxy.currentValue()).isNull();
		});
		assertThat(result.getLazyObjectValueRefOnNonIdFields())
				.isEqualTo(new ObjectRefOnNonIdField("ref-1", "me-the-referenced-object", "ref-key-1", "ref-key-2"));
	}

	@Test
	void readCollectionObjectReferenceFromDocumentNotRelatingToTheIdProperty() {

		String rootCollectionName = template.getCollectionName(CollectionRefRoot.class);
		String refCollectionName = template.getCollectionName(ObjectRefOnNonIdField.class);
		org.bson.Document refSource = new Document("_id", "ref-1").append("refKey1", "ref-key-1")
				.append("refKey2", "ref-key-2").append("value", "me-the-referenced-object");
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("objectValueRefOnNonIdFields",
				Collections.singletonList(new Document("refKey1", "ref-key-1").append("refKey2", "ref-key-2").append("property",
						"without-any-meaning")));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		CollectionRefRoot result = template.findOne(query(where("id").is("id-1")), CollectionRefRoot.class);
		assertThat(result.getObjectValueRefOnNonIdFields())
				.containsExactly(new ObjectRefOnNonIdField("ref-1", "me-the-referenced-object", "ref-key-1", "ref-key-2"));
	}

	@Data
	static class SingleRefRoot {

		String id;
		String value;

		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		SimpleObjectRef simpleValueRef;

		@ManualReference(lookup = "{ '_id' : '?#{#target}' }", lazy = true) //
		SimpleObjectRef simpleLazyValueRef;

		@Field("simple-value-ref-annotated-field-name") //
		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		SimpleObjectRef simpleValueRefWithAnnotatedFieldName;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }") //
		ObjectRefOfDocument objectValueRef;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }", collection = "#collection") //
		ObjectRefOfDocumentWithEmbeddedCollectionName objectValueRefWithEmbeddedCollectionName;

		@ManualReference(lookup = "{ 'refKey1' : '?#{refKey1}', 'refKey2' : '?#{refKey2}' }") //
		ObjectRefOnNonIdField objectValueRefOnNonIdFields;

		@ManualReference(lookup = "{ 'refKey1' : '?#{refKey1}', 'refKey2' : '?#{refKey2}' }", lazy = true) //
		ObjectRefOnNonIdField lazyObjectValueRefOnNonIdFields;
	}

	@Data
	static class CollectionRefRoot {

		String id;
		String value;

		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		List<SimpleObjectRef> simpleValueRef;

		@Field("simple-value-ref-annotated-field-name") //
		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		List<SimpleObjectRef> simpleValueRefWithAnnotatedFieldName;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }") //
		List<ObjectRefOfDocument> objectValueRef;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }", collection = "?#{collection}") //
		List<ObjectRefOfDocumentWithEmbeddedCollectionName> objectValueRefWithEmbeddedCollectionName;

		@ManualReference(lookup = "{ 'refKey1' : '?#{refKey1}', 'refKey2' : '?#{refKey2}' }") //
		List<ObjectRefOnNonIdField> objectValueRefOnNonIdFields;
	}

	@FunctionalInterface
	interface ReferenceAble {
		Object toReference();
	}

	@Data
	@AllArgsConstructor
	@org.springframework.data.mongodb.core.mapping.Document("simple-object-ref")
	static class SimpleObjectRef implements ReferenceAble {

		@Id String id;
		String value;

		@Override
		public String toReference() {
			return id;
		}
	}

	@Data
	@AllArgsConstructor
	static class ObjectRefOfDocument implements ReferenceAble {

		@Id String id;
		String value;

		@Override
		public Object toReference() {
			return new Document("id", id).append("property", "without-any-meaning");
		}
	}

	@Data
	@AllArgsConstructor
	static class ObjectRefOfDocumentWithEmbeddedCollectionName implements ReferenceAble {

		@Id String id;
		String value;

		@Override
		public Object toReference() {
			return new Document("id", id).append("collection", "object-ref-of-document-with-embedded-collection-name");
		}
	}

	@Data
	@AllArgsConstructor
	static class ObjectRefOnNonIdField implements ReferenceAble {

		@Id String id;
		String value;
		String refKey1;
		String refKey2;

		@Override
		public Object toReference() {
			return new Document("refKey1", refKey1).append("refKey2", refKey2);
		}
	}

	static class ReferencableConverter implements Converter<ReferenceAble, ObjectReference> {

		@Nullable
		@Override
		public ObjectReference convert(ReferenceAble source) {
			return source::toReference;
		}
	}

}
