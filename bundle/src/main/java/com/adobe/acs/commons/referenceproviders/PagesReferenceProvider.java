/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.referenceproviders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;
@Component
@Service(ReferenceProvider.class)
public class PagesReferenceProvider implements ReferenceProvider {

private static final String TYPE_PAGE = "page";
private static final String pageRootPath = "/content/";
private static final String jcrContentRelativePath ="/"+JcrConstants.JCR_CONTENT;
private static final String urlExtension =".html";
//any text containing /content/
private final Pattern pattern = Pattern.compile("([\"']|^)(" + Pattern.quote(pageRootPath) + ")(\\S|$)");
    @Override
    public List<Reference> findReferences(Resource resource) {
        List<Reference> references = new ArrayList<Reference>();

        Set<String> paths = new HashSet<String>(); 
        ResourceResolver resolver = resource.getResourceResolver();
        search(resource, paths , resolver);
        for(String path : paths){
            references.add(getReference(resolver.getResource(path)));
        }
       
        return references;
    }

    private void search(Resource resource, Set<String> references,ResourceResolver resolver){
        findReferencesInResource(resource, references, resolver);
        for(Iterator<Resource> iter = resource.listChildren();iter.hasNext();){
            search(iter.next(), references, resolver);
        }
    }
    private void findReferencesInResource(Resource resource, Set<String> references, ResourceResolver resolver){
        ValueMap map = resource.adaptTo(ValueMap.class);
        for(String key :map.keySet()){
            String[] values = map.get(key, new String[0]);
            for(String value :values){
                if (pattern.matcher(value).find()) {
                   for(String path:getAllPathsInAProperty( value)){
                       if(isResourcePartOfAnotherPage(resolver , path)){
                           references.add(getPagePathOfGivenResourcePath(path));
                       }
                   }
                }
            }
        }
    }
    private Reference getReference(Resource res){
        Page page = res.adaptTo(Page.class);
        return new Reference(TYPE_PAGE, page.getName(), res, getLastModifiedTimeOfResource(page));
    }
    private long getLastModifiedTimeOfResource(Page page) {
        final Calendar mod = page.getLastModified();
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        return lastModified;
    }
    private boolean isResourcePartOfAnotherPage(ResourceResolver resolver, String ref){
        String path = ref;
        int jcrcontentIndex = path.indexOf("/jcr:content");
        if(jcrcontentIndex>=0){
            path = path.substring(0,jcrcontentIndex);
        }
      int ext = path.indexOf(".html");
      if(ext>0){
          path = path.substring(0,ext);
      }
        Resource page = resolver.getResource(path);
        if(page.adaptTo(Page.class)!=null){
            return true;
        }
        return false;
    }

private String getPagePathOfGivenResourcePath(String ref){
    String path = ref;
    int jcrcontentIndex = path.indexOf(jcrContentRelativePath);
    if(jcrcontentIndex>=0){
        path = path.substring(0,jcrcontentIndex);
    }
    int ext = path.indexOf(urlExtension);
    if(ext>0){
        path = path.substring(0,ext);
    }
   return path;
}
    private Set<String> getAllPathsInAProperty(String value){
       
        if(isSinglePathInValue(value)){
           return getSinglePath(value);
        }
        else{
            return getMultiplePaths(value);
        }
    }
    private boolean isSinglePathInValue(String value){
        return value.startsWith("/");
    }
    private Set<String> getSinglePath(String value){
        Set<String> paths = new HashSet<String>();
         paths.add(decode(value));
         return paths;
    }
    private Set<String> getMultiplePaths(String value){
        Set<String> paths = new HashSet<String>();
        int startPos = value.indexOf(pageRootPath, 1);
        while (startPos != -1) {
            char charBeforeStartPos = value.charAt(startPos - 1);
            if (charBeforeStartPos == '\'' || charBeforeStartPos == '"') {
                int endPos = value.indexOf(charBeforeStartPos, startPos);
                if (endPos > startPos) {
                    String ref = value.substring(startPos, endPos);
                    paths.add(decode(ref) );
                    startPos = endPos;
                }
            }
            startPos = value.indexOf(pageRootPath, startPos + 1);
        }
        return paths;
    }
    private String decode(String url) {
        try {
            return new URI(url).getPath();
        } catch(URISyntaxException e) {
            return url;
        }
    }
}
