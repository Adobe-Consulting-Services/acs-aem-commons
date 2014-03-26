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
package com.adobe.acs.commons.configpage.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.configpage.GridOperationFailedException;
import com.adobe.acs.commons.configpage.GridStoreService;

@SuppressWarnings("serial")
@SlingServlet(
        label = "ACS AEM Commons - Config Page Servlet",
        description = "Servlet end-point used to the json store in config page",
        resourceTypes = {"acs-commons/components/utilities/editablegrid"},
        selectors = {"store"},
        extensions = {"json"},
        methods = {"GET","POST"},
        generateComponent = true)
public class EditGridJSONStoreServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory
            .getLogger(EditGridJSONStoreServlet.class);


    @Reference
    private GridStoreService gridStoreService;
    
    private static enum Operation {
        UPDATE("update"), DELETE("delete"),NOOP("noop");
        private String type;
        private Operation(String type) {
            this.type = type;
        }
       String type(){
            return type;
        }
    };
  
    
    @Override
    public final void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        final JSONWriter writer = new JSONWriter(response.getWriter());

      
        ResourceResolver resolver = request.getResourceResolver();

        try {
            Resource gridResource = getGridResource(request.getResource());
            int iLen =0;
            writer.object();
            
            writer.key("grid");
            writer.array();
            if (gridResource != null) {
                for (Iterator<Resource> iter = resolver
                        .listChildren(gridResource); iter.hasNext();) {
                    iLen++;
                    Resource record = iter.next();
                    writer.object();
                    ValueMap map = record.adaptTo(ValueMap.class);
                    for (Iterator<String> keyIter = map.keySet().iterator(); keyIter
                            .hasNext();) {
                        String key = keyIter.next();
                        if(!JcrConstants.JCR_PRIMARYTYPE.equals(key)){
                        writer.key(key);
                        writer.value(map.get(key, ""));
                        }
                    }
                    writer.endObject();

                }
            
            }
            writer.endArray();
           
            writer.key("results");
            writer.value(iLen);
            writer.endObject();
         
        } catch (JSONException e) {
            response.reset();
            log.error(e.getMessage(), e);
            throw new ServletException("Unable to produce JSON", e);
        } catch (GridOperationFailedException e) {
            log.error(e.getMessage(), e);
            throw new ServletException("Unable to produce JSON", e);
        } 

    }
    @Override
    public  void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {

      
        boolean success = false;
        try {
            success = update(request);
        } finally {

            writeJsonToResponse(response, success);

        }
    }
    protected void writeJsonToResponse(SlingHttpServletResponse response, boolean success) throws IOException{
        JSONWriter w = new JSONWriter(response.getWriter());
        try {
            w.object();
            w.key("success").value(success);
            w.endObject();
        } catch (JSONException e) {
            log.error("error writing json response", e);
        }
    }
    protected boolean update(SlingHttpServletRequest request) {
        boolean success = false;
        try{
        Operation operation = getOperationFromRequest(request);
        switch(operation){
        case UPDATE:{
        return   gridStoreService.addOrUpdateRows(request.getResourceResolver(), getModifiedRowsFromRequestForUpdate(request), request.getResource().getChild("grid")); 
        }
        case DELETE:{
          return  gridStoreService.deleteRows(request.getResourceResolver(), getModifiedRowsFromRequestForDelete(request), request.getResource().getChild("grid")); 
        }
        case NOOP:{
            
        }
        }
        }catch(GridOperationFailedException e){
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
       return success;
    }
    
   private Operation getOperationFromRequest(SlingHttpServletRequest request){
       String selectorstring = request.getRequestPathInfo().getSelectorString();
       for(Operation operation :Operation.values()){
          if(selectorstring.contains(operation.type())){
              return operation;
          }
       }
       return Operation.NOOP;
   }
    private Resource getGridResource(Resource contentResource) throws GridOperationFailedException {

        return contentResource.getChild("grid");
    }
    private List<Map<String,String>> getModifiedRowsFromRequestForUpdate(SlingHttpServletRequest request) throws IOException, GridOperationFailedException{
        JSONObject grid = getJsonFromRequest(request);
        try {
            JSONArray rows = grid.getJSONArray("grid");
            List<Map<String, String>> modifiedRows = new ArrayList<Map<String,String>>();
            if(rows!=null){
                for(int i =0 ; i <rows.length();i++){
                    JSONObject row = rows.getJSONObject(i);
                   
                    Map<String, String> map = new HashMap<String, String>();
                    for(Iterator<String> iter = row.keys();iter.hasNext();){
                        String key = iter.next();
                        map.put(key, row.getString(key));
                    }
                    modifiedRows.add(map);
                }
                return modifiedRows;
            }
        } catch (JSONException e) {
            throw new GridOperationFailedException(e.getMessage());
        }
        return Collections.emptyList();
    }
    private List<String> getModifiedRowsFromRequestForDelete(SlingHttpServletRequest request) throws IOException, GridOperationFailedException{
        JSONObject grid = getJsonFromRequest(request);
        try {
            JSONArray rows = grid.getJSONArray("grid");
            List<String> modifiedRows = new ArrayList<String>();
            if(rows!=null){
                for(int i =0 ; i <rows.length();i++){
                   
                    modifiedRows.add(  rows.getString(i));
                }
                return modifiedRows;
            }
        } catch (JSONException e) {
            throw new GridOperationFailedException(e.getMessage());
        }
        return Collections.emptyList();
    }
    private JSONObject getJsonFromRequest(SlingHttpServletRequest request)
            throws IOException {

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder("");
        String line = "";
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return getJsonFromString(sb.toString());
    }
    private JSONObject getJsonFromString(String str) {
        try {
            JSONObject obj = new JSONObject(str);
            return obj;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

}
