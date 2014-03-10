package com.adobe.acs.commons.configpage.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.acs.commons.configpage.Configurations;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class ConfigurationsImpl implements Configurations {

    private final Resource resource;
    private final String relPath ;
    private final PageManager manager;
    private final Page page;
    public ConfigurationsImpl(Resource resource){
        this.resource = resource;
        this.relPath = getInnerPath(resource);
        this.manager = resource.getResourceResolver().adaptTo(PageManager.class);
        this.page = manager.getContainingPage(resource);
    }
    /**will work only if the node name is key and user inputted value. currently for ease, we are random generating the uid. this will allow us to have lists and not just unique values
     */
//    public Map<String,String> lookup(String keyWord){
//        Resource res = getInherited(keyWord);
//        if(res==null)return Collections.emptyMap();
//        ValueMap map = res.adaptTo(ValueMap.class);
//       return adaptValueMapToMap(map);
//    }
    @Override
    public Map<String,String> lookup(String columnName,String keyWord){
        List<Map<String,String>> list = getList(columnName, keyWord);
        if(list.size()==0)return Collections.emptyMap();
       return list.get(0);
    }
    @Override
    public List<Map<String,String>> getList(String columnName, String keyWord){
      return getList(resource, columnName, keyWord);
    }
    private List<Map<String,String>> getList(Resource currentResource,String columnName, String keyWord){
      
        Resource currRes = currentResource;
        List<Map<String,String>> list = new ArrayList<Map<String,String>>();
        do{
        for(Iterator<Resource> iter = currRes.listChildren();iter.hasNext();){
            Resource res = iter.next();
            ValueMap map = res.adaptTo(ValueMap.class);
            if(keyWord.equals(map.get(columnName, ""))){
                list.add(adaptValueMapToMap(map));
            }
        }
        currRes = getResourceFromParentPage(manager.getContainingPage(currRes));
        }while(list.size()==0 && currRes!=null);
        
         return list;
     }
    private Map<String , String> adaptValueMapToMap(ValueMap map){
        Map<String,String> result = new HashMap<String, String>();
        for(String key:map.keySet()){
            result.put(key, map.get(key, ""));
        }
        return result;
    }
    private  Resource getInherited(String keyWord) {
        String finalPath = relPath+"/"+keyWord;
        
        Page currPage = page;
        Resource contentResource  =  currPage.getContentResource();
     while(currPage!=null && contentResource!=null){
         Resource res = contentResource.getChild(finalPath);
         if(res==null){
             currPage = page.getParent();
         }
         else{
             return res;
         }
         contentResource = page.getContentResource();
     }
      return null;   
    }
    private String getInnerPath(Resource resource) {
        if (resource == null) {
            return ".";
        }
        final String resPath = resource.getPath();
        int pos = resPath.indexOf(JcrConstants.JCR_CONTENT + "/");
        if (pos <= 0) {
            return ".";
        }
        return resPath.substring(pos + JcrConstants.JCR_CONTENT.length() + 1);
    }
    
    private Resource getResourceFromParentPage(Page currPage){
      Page parentPage = currPage.getParent();
      if(parentPage==null)
      {
          return null;
      }
      return parentPage.getContentResource().getChild(relPath);
      
    }
}
