/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2023 Adobe
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

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.JcrConstants;
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


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BulkPageTaggerTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private BulkPageTagger bulkPageTagger;

    @Mock
    private ActionManager actionManager;

    @Before
    public void setUp() throws Exception {
        bulkPageTagger = new BulkPageTagger();
        bulkPageTagger.excelFile = getClass().getResourceAsStream("/com/adobe/acs/commons/mcp/impl/processes/bulkPageTagger.xlsx");
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                method.accept(context.resourceResolver());
                return null;
            }
        }).when(actionManager).withResolver(any(CheckedConsumer.class));

    }

    @Test
    public void testParseExcel() throws Exception {

        final String tagsRootPath = com.day.cq.tagging.TagConstants.TAG_ROOT_PATH;
        context.create().resource(tagsRootPath, JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        context.resourceResolver().commit();
        bulkPageTagger.parseExcel(actionManager);

        final int expected = 9;
        assertEquals(expected,bulkPageTagger.pageTagMapping.size() );
    }

    @Test
    public void testTagPages() throws Exception {

        bulkPageTagger.parseExcel(actionManager);
        context.create().page("/content/wknd/language-masters/en/about-us");
        context.create().resource("/content/wknd/language-masters/en/about-us/jcr:content",
                JcrConstants.JCR_PRIMARYTYPE,
                "cq:pagecontent","cq:tags","wknd-shared:activity/bulktagtest3;wknd-shared:activity/bulktagtest10");
        bulkPageTagger.tagPages(actionManager);

    }
}
