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
package com.adobe.acs.commons.util;

import com.day.cq.commons.Filter;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PageWrapperTest {

    @Mock
    private Page mocked;
    
    static final class PageWrapperImpl extends PageWrapper{

        public PageWrapperImpl(Page wrapped) {
            super(wrapped);
        }
    }
    
    private Page systemUnderTest;
    @Before
    public void setUp() throws Exception {
        systemUnderTest = new PageWrapperImpl(mocked);
    }

    @Test
    public void test_getPath() {
        systemUnderTest.getPath();
        verify(mocked, times(1)).getPath();
    }

    @Test
    public void test_getPageManager() {
        systemUnderTest.getPageManager();
        verify(mocked, times(1)).getPageManager();
    }

    @Test
    public void test_getContentResource() {
        systemUnderTest.getContentResource();
        verify(mocked, times(1)).getContentResource();
    }

    @Test
    public void test_testGetContentResource() {
        systemUnderTest.getContentResource("");
        verify(mocked, times(1)).getContentResource(anyString());
    }

    @Test
    public void test_listChildren() {
        systemUnderTest.listChildren();
        verify(mocked, times(1)).listChildren();
    }

    @Mock
    private Filter<Page> pageFilter;

    @Test
    public void test_testListChildren() {
        systemUnderTest.listChildren(pageFilter);
        verify(mocked, times(1)).listChildren(pageFilter);
    }

    @Test
    public void test_testListChildren1() {
        systemUnderTest.listChildren(pageFilter, false);
        verify(mocked, times(1)).listChildren(pageFilter, false);
    }

    @Test
    public void test_hasChild() {
        systemUnderTest.hasChild("child");
        verify(mocked, times(1)).hasChild(anyString());
    }

    @Test
    public void test_getDepth() {
        systemUnderTest.getDepth();
        verify(mocked, times(1)).getDepth();
    }

    @Test
    public void test_getParent() {
        systemUnderTest.getParent();
        verify(mocked, times(1)).getParent();
    }

    @Test
    public void test_testGetParent() {
        systemUnderTest.getParent(1);
        verify(mocked, times(1)).getParent(1);
    }

    @Test
    public void test_getAbsoluteParent() {
        systemUnderTest.getAbsoluteParent(1);
        verify(mocked, times(1)).getAbsoluteParent(1);
    }

    @Test
    public void test_getProperties() {
        systemUnderTest.getProperties();
        verify(mocked, times(1)).getProperties();
    }

    @Test
    public void test_testGetProperties() {
        systemUnderTest.getProperties("test");
        verify(mocked, times(1)).getProperties(anyString());
    }

    @Test
    public void test_getName() {
        systemUnderTest.getName();
        verify(mocked, times(1)).getName();
    }

    @Test
    public void test_getTitle() {
        systemUnderTest.getTitle();
        verify(mocked, times(1)).getTitle();
    }

    @Test
    public void test_getDescription() {
        systemUnderTest.getDescription();
        verify(mocked, times(1)).getDescription();
    }

    @Test
    public void test_getPageTitle() {
        systemUnderTest.getPageTitle();
        verify(mocked, times(1)).getPageTitle();
    }

    @Test
    public void test_getNavigationTitle() {
        systemUnderTest.getNavigationTitle();
        verify(mocked, times(1)).getNavigationTitle();
    }

    @Test
    public void test_isHideInNav() {
        systemUnderTest.isHideInNav();
        verify(mocked, times(1)).isHideInNav();
    }

    @Test
    public void test_hasContent() {
        systemUnderTest.hasContent();
        verify(mocked, times(1)).hasContent();
    }

    @Test
    public void test_isValid() {
        systemUnderTest.isValid();
        verify(mocked, times(1)).isValid();
    }

    @Test
    public void test_timeUntilValid() {
        systemUnderTest.timeUntilValid();
        verify(mocked, times(1)).timeUntilValid();
    }

    @Test
    public void test_getOnTime() {
        systemUnderTest.getOnTime();
        verify(mocked, times(1)).getOnTime();
    }

    @Test
    public void test_getOffTime() {
        systemUnderTest.getOffTime();
        verify(mocked, times(1)).getOffTime();
    }

    @Test
    public void test_getDeleted() {
        systemUnderTest.getDeleted();
        verify(mocked, times(1)).getDeleted();
    }

    @Test
    public void test_getDeletedBy() {
        systemUnderTest.getDeletedBy();
        verify(mocked, times(1)).getDeletedBy();
    }

    @Test
    public void test_getLastModifiedBy() {
        systemUnderTest.getLastModifiedBy();
        verify(mocked, times(1)).getLastModifiedBy();
    }

    @Test
    public void test_getLastModified() {
        systemUnderTest.getLastModified();
        verify(mocked, times(1)).getLastModified();
    }

    @Test
    public void test_getVanityUrl() {
        systemUnderTest.getVanityUrl();
        verify(mocked, times(1)).getVanityUrl();
    }

    @Test
    public void test_getTags() {
        systemUnderTest.getTags();
        verify(mocked, times(1)).getTags();
    }

    @Test
    public void test_lock() throws WCMException {
        systemUnderTest.lock();
        verify(mocked, times(1)).lock();
    }

    @Test
    public void test_isLocked() {
        systemUnderTest.isLocked();
        verify(mocked, times(1)).isLocked();
    }

    @Test
    public void test_getLockOwner() {
        systemUnderTest.getLockOwner();
        verify(mocked, times(1)).getLockOwner();
    }

    @Test
    public void test_canUnlock() {
        systemUnderTest.canUnlock();
        verify(mocked, times(1)).canUnlock();
    }

    @Test
    public void test_unlock() throws WCMException {
        systemUnderTest.unlock();
        verify(mocked, times(1)).unlock();
    }

    @Test
    public void test_getTemplate() {
        systemUnderTest.getTemplate();
        verify(mocked, times(1)).getTemplate();
    }

    @Test
    public void test_getLanguage() {
        systemUnderTest.getLanguage();
        verify(mocked, times(1)).getLanguage();
    }

    @Test
    public void test_testGetLanguage() {
        systemUnderTest.getLanguage(true);
        verify(mocked, times(1)).getLanguage(true);
    }

    @Test
    public void test_adaptTo() {
        systemUnderTest.adaptTo(Resource.class);
        verify(mocked, times(1)).adaptTo(Resource.class);
    }
}