/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.util.StringUtil;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class TagReportTest {

  @Rule
  public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

  @Mock
  private ActionManager actionManager;

  private TagReporter tagReporter;

  @Before
  public void setUp() throws Exception {

    tagReporter = new TagReporter();

    ctx.load().json("/com/adobe/acs/commons/mcp/impl/processes/tags.json", "/etc/tags");
    ctx.load().json("/com/adobe/acs/commons/mcp/impl/processes/content.json", "/content");

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
        method.accept(ctx.resourceResolver());
        return null;
      }
    }).when(actionManager).withResolver(any(CheckedConsumer.class));
  }

  @Test
  public void testTraverseTags() throws Exception {

    tagReporter.tagPath = "/etc/tags/workflow";

    tagReporter.traverseTags(actionManager);
    assertEquals(11, tagReporter.getReportRows().size());

    tagReporter.getReportRows().forEach(r -> {
      String id = (String) r.get(TagReporter.REPORT_COLUMNS.TAG_ID);
      if ("workflow:wcm".equals(id) || "workflow:".equals(id)) {
        assertEquals("Missing reference for " + id, 1L, r.get(TagReporter.REPORT_COLUMNS.REFERENCE_COUNT));
      } else {
        assertEquals("Found extra reference for " + id, 0L, r.get(TagReporter.REPORT_COLUMNS.REFERENCE_COUNT));
      }
    });
  }

  @Test
  public void testInvalidRoot() throws Exception {

    tagReporter.tagPath = "/etc/tags/totally-a-tag";

    tagReporter.traverseTags(actionManager);
    assertEquals(1, tagReporter.getReportRows().size());

    assertEquals(StringUtil.getFriendlyName(TagReporter.ITEM_STATUS.FAILURE.name()),
        tagReporter.getReportRows().get(0).get(TagReporter.REPORT_COLUMNS.STATUS));
  }

}