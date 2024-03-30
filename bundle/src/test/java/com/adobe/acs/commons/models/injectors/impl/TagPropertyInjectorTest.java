/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.impl.TagPropertyAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestTagPropertyInjectModel;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestTagPropertyInjectModelImpl;
import com.day.cq.tagging.Tag;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class TagPropertyInjectorTest {

    @Rule
    public final AemContext context = InjectorAEMContext.provide();

    @InjectMocks
    private TagPropertyInjector systemUnderTest;

    private TestTagPropertyInjectModel adapted;



    @Before
    public void setUp() throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("tags.json");
        context.load().json(inputStream, "/content/cq:tags");

        context.currentPage("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten");
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content");

        context.registerService(Injector.class, systemUnderTest);
        context.registerService(StaticInjectAnnotationProcessorFactory.class, new TagPropertyAnnotationProcessorFactory());
        context.addModelsForClasses(TestTagPropertyInjectModelImpl.class);

        Resource adaptable = context.request().getResource();
        adapted = adaptable.adaptTo(TestTagPropertyInjectModel.class);
    }

    @Test
    public void test_model() {
        assertNotNull(adapted);
        assertNotNull(adapted.getSingleTag());
        assertNotNull(adapted.getSingleTagInherited());

        assertEquals("wknd-shared:activity/specific-tag", adapted.getSingleTag().getTagID());
        assertEquals("wknd-shared:activity/inherited-tag", adapted.getSingleTagInherited().getTagID());

        for (Tag tag : adapted.getMultipleTagsArray()) {
            assertNotNull(tag);
            assertNotNull(tag.getTagID());
            assertNotNull(tag.getPath());
            assertNotNull(tag.getName());
        }

        for (Tag tag : adapted.getMultipleTagsList()) {
            assertNotNull(tag);
            assertNotNull(tag.getTagID());
            assertNotNull(tag.getPath());
            assertNotNull(tag.getName());
        }

        for (Tag tag : adapted.getMultipleTagsSet()) {
            assertNotNull(tag);
            assertNotNull(tag.getTagID());
            assertNotNull(tag.getPath());
            assertNotNull(tag.getName());
        }

        for (Tag tag : adapted.getMultipleTagsCollection()) {
            assertNotNull(tag);
            assertNotNull(tag.getTagID());
            assertNotNull(tag.getPath());
            assertNotNull(tag.getName());
        }
    }
}
