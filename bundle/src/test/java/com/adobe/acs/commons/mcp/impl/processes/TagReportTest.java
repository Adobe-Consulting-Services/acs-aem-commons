/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
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
    Actions.setCurrentActionManager(actionManager);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
        method.accept(ctx.resourceResolver());
        return null;
      }
    }).when(actionManager).withResolver(any(CheckedConsumer.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
        method.accept(ctx.resourceResolver());
        return null;
      }
    }).when(actionManager).deferredWithResolver(any(CheckedConsumer.class));
  }

  @Test
  public void testTraverseTags() throws Exception {

    tagReporter.tagPath = "/etc/tags/workflow";
    tagReporter.rootSearchPath = "/content";
    tagReporter.includeReferences = false;
    tagReporter.traverseTags(actionManager);
    tagReporter.recordTags(actionManager);
    assertEquals(11, tagReporter.getReportRows().size());

    tagReporter.getReportRows().forEach(r -> {
      String id = (String) r.get(TagReporter.ReportColumns.TAG_ID);
      if ("workflow:wcm".equals(id) || "workflow:".equals(id)) {
        assertEquals("Missing reference for " + id, 1L, r.get(TagReporter.ReportColumns.REFERENCE_COUNT));
      } else {
        assertEquals("Found extra reference for " + id, 0L, r.get(TagReporter.ReportColumns.REFERENCE_COUNT));
      }
      assertTrue(!r.containsKey(TagReporter.ReportColumns.REFERENCES));
    });
  }

  @Test
  public void testInvalidRoot() throws Exception {

    tagReporter.tagPath = "/etc/tags/totally-a-tag";
    tagReporter.rootSearchPath = "/content";
    tagReporter.includeReferences = false;

    tagReporter.traverseTags(actionManager);
    tagReporter.recordTags(actionManager);
    assertEquals(1, tagReporter.getReportRows().size());

    assertEquals(StringUtil.getFriendlyName(TagReporter.ItemStatus.FAILURE.name()),
        tagReporter.getReportRows().get(0).get(TagReporter.ReportColumns.STATUS));
  }

  @Test
  public void testIncludeReferences() throws Exception {

    tagReporter.tagPath = "/etc/tags/workflow";
    tagReporter.rootSearchPath = "/content";
    tagReporter.includeReferences = true;

    tagReporter.traverseTags(actionManager);
    tagReporter.recordTags(actionManager);
    assertEquals(11, tagReporter.getReportRows().size());

    tagReporter.getReportRows().forEach(r -> {
      String id = (String) r.get(TagReporter.ReportColumns.TAG_ID);
      if ("workflow:wcm".equals(id) || "workflow:".equals(id)) {
        assertEquals("Missing reference for " + id, 1L, r.get(TagReporter.ReportColumns.REFERENCE_COUNT));
        assertTrue("Missing references value for " + id,
            StringUtils.isNotBlank((String) r.get(TagReporter.ReportColumns.REFERENCES)));
      } else {
        assertEquals("Found extra reference for " + id, 0L, r.get(TagReporter.ReportColumns.REFERENCE_COUNT));
        assertTrue("Unexpected references value for " + id,
            StringUtils.isBlank((String) r.get(TagReporter.ReportColumns.REFERENCES)));
      }
    });
  }

}