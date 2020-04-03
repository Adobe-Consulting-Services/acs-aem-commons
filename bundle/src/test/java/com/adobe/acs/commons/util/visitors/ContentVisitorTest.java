/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.util.visitors;

import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContentVisitorTest {
    @Mock
    Resource resource;

    ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>());

    @Spy
    ResourceRunnable runnable = new TestRunnable();

    @Before
    public void setUp() throws Exception {
        when(resource.getValueMap()).thenReturn(properties);
        when(resource.listChildren()).thenReturn(IteratorUtils.EMPTY_LIST_ITERATOR);
    }

    @Test
    public void accept_ContainerSlingFolder() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");

        ContentVisitor visitor = spy(new ContentVisitor(runnable));
        visitor.accept(resource);
        verify(visitor, times(1)).visit(resource);
    }

    @Test
    public void accept_ContainerNtFolder() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "nt:folder");

        ContentVisitor visitor = spy(new ContentVisitor(runnable));
        visitor.accept(resource);
        verify(visitor, times(1)).visit(resource);
    }

    @Test
    public void accept_ContainerSlingOrderedFolder() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "sling:OrderedFolder");

        ContentVisitor visitor = spy(new ContentVisitor(runnable));
        visitor.accept(resource);
        verify(visitor, times(1)).visit(resource);
    }

    @Test
    public void accept_ContainerInvalid() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured");

        ContentVisitor visitor = spy(new ContentVisitor(runnable));
        visitor.accept(resource);
        verify(visitor, times(0)).visit(resource);
    }

    @Test
    public void accept_ContentDAMAsset() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "dam:Asset");

        ContentVisitor visitor = new ContentVisitor(runnable);
        visitor.accept(resource);
        verify(runnable, times(1)).run(resource);
    }

    @Test
    public void accept_ContentCqPage() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Page");

        ContentVisitor visitor = new ContentVisitor(runnable);
        visitor.accept(resource);
        verify(runnable, times(1)).run(resource);
    }

    @Test
    public void accept_ContentCqTag() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Tag");

        ContentVisitor visitor = new ContentVisitor(runnable);
        visitor.accept(resource);
        verify(runnable, times(1)).run(resource);
    }

    @Test
    public void accept_ContentInvalid() throws Exception {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, "oak:Unstructured");

        ContentVisitor visitor = new ContentVisitor(runnable);
        visitor.accept(resource);
        verify(runnable, times(0)).run(resource);
    }

    public static class TestRunnable implements ResourceRunnable {
        @Override
        public void run(Resource resource) throws Exception {
        }
    }
}