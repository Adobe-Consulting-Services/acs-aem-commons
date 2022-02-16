/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.models.MapEntryTest;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

public class TagsCellValueTest {

  private static final Logger log = LoggerFactory.getLogger(MapEntryTest.class);

  private static final String[] TAGS_VALUE = new String[] { "val1", "val2" };

  @Mock
  private SlingHttpServletRequest request;

  @Mock
  private Resource mockResource;

  @Mock
  private ResourceResolver resolver;

  @Mock
  private Tag tag1;

  @Mock
  private Tag tag2;

  @Mock
  private TagManager tagMgr;

  @Before
  public void init() {
    log.info("init");
    
    MockitoAnnotations.initMocks(this);

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("tags", TAGS_VALUE);

    when(request.getAttribute("result")).thenReturn(mockResource);
    when(mockResource.getValueMap()).thenReturn(new ValueMapDecorator(properties));

    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(TagManager.class)).thenReturn(tagMgr);

    when(tagMgr.resolve(TAGS_VALUE[0])).thenReturn(tag1);
    when(tagMgr.resolve(TAGS_VALUE[1])).thenReturn(tag2);

    when(tag1.getTitle()).thenReturn(TAGS_VALUE[0]);
    when(tag2.getTitle()).thenReturn(TAGS_VALUE[1]);
  }

  @Test
  public void testEmpty() throws IllegalAccessException {
    log.info("testEmpty");
    TagsCellValue val = new TagsCellValue();
    FieldUtils.writeField(val, "property", "tags2", true);
    FieldUtils.writeField(val, "request", request, true);
    List<Tag> tags = val.getTags();
    assertEquals(0, tags.size());
    log.info("Test successful!");
  }

  @Test
  public void testExporter() throws IllegalAccessException {
    log.info("testExporter");
    TagsCellValue val = new TagsCellValue();
    FieldUtils.writeField(val, "property", "tags", true);
    FieldUtils.writeField(val, "request", request, true);
    assertTrue(ArrayUtils.isEquals(new Tag[] { tag1, tag2 }, val.getTags().toArray(new Tag[val.getTags().size()])));
    log.info("Test successful!");
  }

}
