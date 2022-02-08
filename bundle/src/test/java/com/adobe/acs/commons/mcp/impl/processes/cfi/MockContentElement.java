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
package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.adobe.cq.dam.cfm.VersionDef;
import com.adobe.cq.dam.cfm.VersionedContent;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Incomplete mock that provides just enough for basic testing
 */
public class MockContentElement implements ContentElement {

    Map.Entry<String, String> entry;

    public MockContentElement(Map.Entry<String, String> e) {
        entry = e;
    }

    @Override
    public Iterator<ContentVariation> getVariations() {
        return null;
    }

    @Override
    public ContentVariation getVariation(String s) {
        return null;
    }

    @Override
    public ContentVariation createVariation(VariationTemplate variationTemplate) throws ContentFragmentException {
        return null;
    }

    @Override
    public void removeVariation(ContentVariation contentVariation) throws ContentFragmentException {
    }

    @Override
    public ContentVariation getResolvedVariation(String s) {
        return null;
    }

    @Override
    public String getName() {
        return entry.getKey();
    }

    @Override
    public String getTitle() {
        return entry.getKey();
    }

    @Override
    public String getContent() {
        return entry.getValue();
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public void setContent(String s, String s1) throws ContentFragmentException {
        entry.setValue(s);
    }

    @Override
    public VersionDef createVersion(String s, String s1) throws ContentFragmentException {
        return null;
    }

    @Override
    public Iterator<VersionDef> listVersions() throws ContentFragmentException {
        return null;
    }

    @Override
    public VersionedContent getVersionedContent(VersionDef versionDef) throws ContentFragmentException {
        return null;
    }

    @CheckForNull
    @Override
    public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> aClass) {
        return null;
    }

    @Override
    public FragmentData getValue() {
        return null;
    }

    @Override
    public void setValue(FragmentData object) throws ContentFragmentException {
        
    }

}
