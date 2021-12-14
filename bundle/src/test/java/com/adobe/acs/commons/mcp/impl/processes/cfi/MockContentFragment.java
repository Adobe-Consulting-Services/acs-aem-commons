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
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.VariationDef;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.adobe.cq.dam.cfm.VersionDef;
import com.adobe.cq.dam.cfm.VersionedContent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.sling.api.resource.Resource;

/**
 * Incomplete mock that provides just enough for basic testing
 */
public class MockContentFragment implements ContentFragment {
    
    String name;
    String title;
    String path;
    HashMap<String, String> elements = new HashMap<>();
    HashMap<String, Object> metadata = new HashMap<>();

    @Override
    public Iterator<ContentElement> getElements() {
        return elements.entrySet().stream().map(MockContentElement::new).map((e) -> (ContentElement) e).collect(Collectors.toList()).iterator();
    }

    @Override
    public boolean hasElement(String s) {
        return elements.containsKey(s);
    }

    @Override
    public ContentElement createElement(ElementTemplate elementTemplate) throws ContentFragmentException {
        return null;
    }

    @Override
    public ContentElement getElement(String s) {
        for (Map.Entry<String, String> elems : elements.entrySet()) {
            if (elems.getKey().equals(s)) {
                return new MockContentElement(elems);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String s) throws ContentFragmentException {
        title = s;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String s) throws ContentFragmentException {
    }

    @Override
    public Map<String, Object> getMetaData() {
        return metadata;
    }

    @Override
    public void setMetaData(String s, Object o) throws ContentFragmentException {
        metadata.put(s, o);
    }

    @Override
    public Iterator<VariationDef> listAllVariations() {
        return null;
    }

    @Override
    public FragmentTemplate getTemplate() {
        return null;
    }

    @Override
    public VariationTemplate createVariation(String s, String s1, String s2) throws ContentFragmentException {
        return null;
    }

    @Override
    public Iterator<Resource> getAssociatedContent() {
        return null;
    }

    @Override
    public void addAssociatedContent(Resource resource) throws ContentFragmentException {
    }

    @Override
    public void removeAssociatedContent(Resource resource) throws ContentFragmentException {
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
    public void removeVariation(String name) throws ContentFragmentException {
    }
    
}
