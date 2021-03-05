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

import com.mongodb.client.MongoClient;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.convert.LazyLoadingProxy;
import org.springframework.data.mongodb.core.convert.LazyLoadingTestUtils;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.ManualReference;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.mapping.ObjectReference;
import org.springframework.data.mongodb.test.util.Client;
import org.springframework.data.mongodb.test.util.MongoClientExtension;
import org.springframework.data.mongodb.test.util.MongoTemplateExtension;
import org.springframework.data.mongodb.test.util.MongoTestTemplate;
import org.springframework.data.mongodb.test.util.Template;

import com.mongodb.client.model.Filters;
import org.springframework.lang.Nullable;

/**
 * {@link DBRef} related integration tests for
 * {@link MongoTemplate}.
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
		});

		cfg.configureMappingContext(it -> {
			it.autocreateIndex(false);
		});
	});

	@BeforeEach
	public void setUp() {
		template.flush(Root.class, JustSomeType.class);
	}

	@Test
	void xxx() {

		JustSomeType justSomeType = new JustSomeType();
		justSomeType.id = "id-1";
		justSomeType.value = "v-1";

		template.save(justSomeType);

		Root root = new Root();
		root.id = "r-1";
		root.value = "rv-1";
		root.justSomeType = justSomeType;

		template.save(root);


	}


	@Data
	static class Root {

		@Id String id;
		String value;

		@ManualReference(lookup = "{ _id : #{value} }")
		JustSomeType justSomeType;

	}

	@Data
	static class JustSomeType {

		@Id String id;
		String value;
	}

	static class ManualReferenceConverter implements Converter<JustSomeType, ObjectReference> {

		@Nullable
		@Override
		public ObjectReference convert(JustSomeType source) {
			return source::getId;
		}
	}

}
