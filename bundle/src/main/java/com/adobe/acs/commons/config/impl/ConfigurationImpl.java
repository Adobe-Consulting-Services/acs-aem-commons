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
package com.adobe.acs.commons.config.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.acs.commons.config.Configuration;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

public class ConfigurationImpl implements Configuration {

    private final Resource resource;
    private final String relPath ;
    private final Page page;
    public ConfigurationImpl(Page page){
        this.resource = page.getContentResource().getChild("grid");
        this.relPath = getRelPath(resource.getPath());
        this.page = page;
    }
    @Override
    public Map<String, String> getRowByKey(String key) {
        List<Map<String,String>> rows = getList("key", key);
        if(rows.size()==0)return Collections.emptyMap();
        return rows.get(0);
    }

    @Override
    public List<Map<String, String>> getRowsByKey(String key) {
        List<Map<String,String>> rows = getList("key", key);
        return rows;
    }

    @Override
    public Map<String, String> getRowByColumnValue(String columnName,
            String columnValue) {
        List<Map<String,String>> rows = getList(columnName, columnValue);
        if(rows.size()==0)return Collections.emptyMap();
       return rows.get(0);
    }

    @Override
    public List<Map<String, String>> getRowsByColumnValue(String columnName,
            String columnValue) {
        List<Map<String,String>> rows = getList(columnName, columnValue);
        return rows;
    }
    
    private List<Map<String,String>> getList(String columnName, String keyWord){
        
        Resource currRes = this.resource;
        Page currPage = this.page;
        List<Map<String,String>> list = new ArrayList<Map<String,String>>();
        do{
        for(Iterator<Resource> iter = currRes.listChildren();iter.hasNext();){
            Resource res = iter.next();
            ValueMap map = res.adaptTo(ValueMap.class);
            if(keyWord.equals(map.get(columnName, ""))){
                list.add(adaptValueMapToMap(map));
            }
        }
        if(list.size()==0){
            currPage = currPage.getParent();
        currRes = getResourceFromParentPage(currPage);
        }
        }while(list.size()==0 && currRes!=null);
        
         return list;
     }
    private String getRelPath(String resPath){
        int pos = resPath.indexOf(JcrConstants.JCR_CONTENT + "/");
        if (pos <= 0) {
            return ".";
        }
        return resPath.substring(pos + JcrConstants.JCR_CONTENT.length() + 1);
    }
    private Resource getResourceFromParentPage(Page parentPage){
     
        if(parentPage==null)
        {
            return null;
        }
        return parentPage.getContentResource().getChild(this.relPath);
        
      }
    private Map<String , String> adaptValueMapToMap(ValueMap map){
        Map<String,String> result = new HashMap<String, String>();
        for(String key:map.keySet()){
            result.put(key, map.get(key, ""));
        }
        return result;
    }
}
