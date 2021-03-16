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
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.ManualReference;
import org.springframework.data.mongodb.core.mapping.ObjectReference;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.SerializationUtils;
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
			it.customConverters(new ManualReferenceConverter());
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
		org.bson.Document source = new Document("_id", "id-1").append("value", "v1").append("simpleValueRef", Collections.singletonList("ref-1"));

		template.execute(db -> {

			db.getCollection(refCollectionName).insertOne(refSource);
			db.getCollection(rootCollectionName).insertOne(source);
			return null;
		});

		CollectionRefRoot result = template.findOne(query(where("id").is("id-1")), CollectionRefRoot.class);
		assertThat(result.getSimpleValueRef()).containsExactly(new SimpleObjectRef("ref-1", "me-the-referenced-object"));
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
	void xxx() {

		JustSomeType justSomeType = new JustSomeType();
		justSomeType.id = "id-1";
		justSomeType.value = "v-1";

		template.save(justSomeType);

		JustSomeType2 justSomeType2 = new JustSomeType2();
		justSomeType2.id = "t2";
		justSomeType2.value = "v-2";

		template.save(justSomeType2);

		Root root = new Root();
		root.id = "r-1";
		root.value = "rv-1";
		root.refValue = justSomeType;
		root.refValueIndexPlaceholder = justSomeType;
		root.refValueIndexPlaceholderSpel = justSomeType2;
		root.refValue2 = justSomeType2;
		// root.refValueList = Collections.singletonList(justSomeType);

		template.save(root);

		List<Root> result = template.find(query(where("id").is(root.id)), Root.class);
		System.out.println("result: " + result);
	}

	@Data
	static class Root {

		@Id String id;
		String value;

		/*
		
		{
			_id : ..
			value : ..
			refValue : id-2
		
		}
		
		 */
		// refValue : id-2 | { id : id-2, col : coll-1 }
		// _id : ?#{refValue.id}

		@ManualReference(lookup = "{ '_id' : '?#{#refValue}' }") // vs '?#{refValue}' vs target as variable name
		JustSomeType refValue;

		// refValue : { id : id-2, col : coll-1 }
		@ManualReference(lookup = "{ '_id' : '?#{id}' }") // vs ?#{#this[id]}
		JustSomeType2 refValue2;

		@ManualReference(lookup = "{ '_id' : '?0' }") // strange case
		JustSomeType refValueIndexPlaceholder;

		// TODO: still need to bind the named parameters - sigh - why oh why?
		@ManualReference(lookup = "{ '_id' : '?#{#target.id}' }") // should this work #{#id} check also target.id
		JustSomeType2 refValueIndexPlaceholderSpel;

		// ok and we need some sort of list handlign
		// not working by now

		// ripple load for single elements { '_id' : '?#{#this}' }
		// intern concat with OR on Mark you are a genius!!!
		@ManualReference(lookup = "{ '_id' : { $in : ?#{#this} } }",
				collection = "#{collection}") List<JustSomeType> refValueList;
	}

	@Data
	static class SingleRefRoot {

		String id;
		String value;

		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		SimpleObjectRef simpleValueRef;

		@Field("simple-value-ref-annotated-field-name") //
		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		SimpleObjectRef simpleValueRefWithAnnotatedFieldName;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }") //
		ObjectRefOfDocument objectValueRef;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }", collection = "#collection") //
		ObjectRefOfDocumentWithEmbeddedCollectionName objectValueRefWithEmbeddedCollectionName;

		@ManualReference(lookup = "{ 'refKey1' : '?#{refKey1}', 'refKey2' : '?#{refKey2}' }") //
		ObjectRefOnNonIdField objectValueRefOnNonIdFields;
	}

	@Data
	static class CollectionRefRoot {

		String id;
		String value;

		@ManualReference(lookup = "{ '_id' : '?#{#target}' }") //
		List<SimpleObjectRef> simpleValueRef;

		@Field("simple-value-ref-annotated-field-name") @ManualReference(
				lookup = "{ '_id' : '?#{#target}' }") List<SimpleObjectRef> simpleValueRefWithAnnotatedFieldName;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }") List<ObjectRefOfDocument> objectValueRef;

		@ManualReference(lookup = "{ '_id' : '?#{id}' }",
				collection = "?#{collection}") List<ObjectRefOfDocumentWithEmbeddedCollectionName> objectValueRefWithEmbeddedDocumentName;

		@ManualReference(
				lookup = "{ 'refKey1' : '?#{ref-key-1}', 'refKey2' : '?#{ref-key-2}' }") List<ObjectRefOnNonIdField> objectValueRefOnNonIdFields;
	}

	interface Identifyable {
		String getId();
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

	@Data
	@org.springframework.data.mongodb.core.mapping.Document("justSomeType")
	static class JustSomeType implements Identifyable {

		@Id String id;
		String value;
	}

	@Data
	@org.springframework.data.mongodb.core.mapping.Document("justSomeType")
	static class JustSomeType2 implements Identifyable {

		@Id String id;
		String value;
	}

	static class ManualReferenceConverter implements Converter<Identifyable, ObjectReference> {

		@Nullable
		@Override
		public ObjectReference convert(Identifyable source) {

			if (source instanceof JustSomeType2) {
				return () -> new Document("collection", "justSomeType").append("id", source.getId());
			}
			return source::getId;
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
