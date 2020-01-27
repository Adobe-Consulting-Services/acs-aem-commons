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
import com.day.cq.tagging.Tag;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.WCMException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

/**
 * PageWrapper
 * <p>
 * Wraps the CQ Page interface to override methods at will without having to declare them all.
 * </p>
 */
public abstract class PageWrapper implements Page {

    private final Page wrapped;

    public PageWrapper(Page wrapped){
        this.wrapped = wrapped;
    }

    @Override
    public String getPath() {
        return wrapped.getPath();
    }

    @Override
    public PageManager getPageManager() {
        return wrapped.getPageManager();
    }

    @Override
    public Resource getContentResource() {
        return wrapped.getContentResource();
    }

    @Override
    public Resource getContentResource(String s) {
        return wrapped.getContentResource(s);
    }

    @Override
    public Iterator<Page> listChildren() {
        return wrapped.listChildren();
    }

    @Override
    public Iterator<Page> listChildren(Filter<Page> filter) {
        return wrapped.listChildren(filter);
    }

    @Override
    public Iterator<Page> listChildren(Filter<Page> filter, boolean b) {
        return wrapped.listChildren(filter,b);
    }

    @Override
    public boolean hasChild(String s) {
        return wrapped.hasChild(s);
    }

    @Override
    public int getDepth() {
        return wrapped.getDepth();
    }

    @Override
    public Page getParent() {
        return wrapped.getParent();
    }

    @Override
    public Page getParent(int i) {
        return wrapped.getParent(i);
    }

    @Override
    public Page getAbsoluteParent(int i) {
        return wrapped.getAbsoluteParent(i);
    }

    @Override
    public ValueMap getProperties() {
        return wrapped.getProperties();
    }

    @Override
    public ValueMap getProperties(String s) {
        return wrapped.getProperties(s);
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public String getTitle() {
        return wrapped.getTitle();
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription();
    }

    @Override
    public String getPageTitle() {
        return wrapped.getPageTitle();
    }

    @Override
    public String getNavigationTitle() {
        return wrapped.getNavigationTitle();
    }

    @Override
    public boolean isHideInNav() {
        return wrapped.isHideInNav();
    }

    @Override
    public boolean hasContent() {
        return wrapped.hasContent();
    }

    @Override
    public boolean isValid() {
        return wrapped.isValid();
    }

    @Override
    public long timeUntilValid() {
        return wrapped.timeUntilValid();
    }

    @Override
    public Calendar getOnTime() {
        return wrapped.getOnTime();
    }

    @Override
    public Calendar getOffTime() {
        return wrapped.getOffTime();
    }

    @Override
    public Calendar getDeleted() {
        return wrapped.getDeleted();
    }

    @Override
    public String getDeletedBy() {
        return wrapped.getDeletedBy();
    }

    @Override
    public String getLastModifiedBy() {
        return wrapped.getLastModifiedBy();
    }

    @Override
    public Calendar getLastModified() {
        return wrapped.getLastModified();
    }

    @Override
    public String getVanityUrl() {
        return wrapped.getVanityUrl();
    }

    @Override
    public Tag[] getTags() {
        return wrapped.getTags();
    }

    @Override
    public void lock() throws WCMException {
        wrapped.lock();
    }

    @Override
    public boolean isLocked() {
        return wrapped.isLocked();
    }

    @Override
    public String getLockOwner() {
        return wrapped.getLockOwner();
    }

    @Override
    public boolean canUnlock() {
        return wrapped.canUnlock();
    }

    @Override
    public void unlock() throws WCMException {
        wrapped.unlock();
    }

    @Override
    public Template getTemplate() {
        return wrapped.getTemplate();
    }

    @Override
    public Locale getLanguage(boolean b) {
        return wrapped.getLanguage(b);
    }

    @Override
    public Locale getLanguage() {
        return wrapped.getLanguage();
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> aClass) {
        return wrapped.adaptTo(aClass);
    }
}