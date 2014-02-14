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
package com.adobe.acs.commons.multipanelfield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tldgen.Function;

public class MultiPanelFieldFunctions {
	 private static final Logger log = LoggerFactory.getLogger(MultiPanelFieldFunctions.class);
	@Function
public static List<Map<String,String>> getMultiPanelFieldValues(Resource resource , String name ){
		ValueMap map = resource.adaptTo(ValueMap.class);
		List<Map<String,String>> columnsList =  null;
		try {
		if(map.containsKey(name)){
			columnsList = new ArrayList<Map<String,String>>();
			String[] columns =  map.get(name,new String[0]);
			for(String column : columns){
				
					JSONObject columnJSON =  new JSONObject(column);
					Map<String,String> columnMap = new HashMap<String, String>();
					for(Iterator<String> iter = columnJSON.keys();iter.hasNext();){
						String key = iter.next();
						String value = columnJSON.getString(key);
						columnMap.put(key, value);
					}
					
					columnsList.add(columnMap);
				
			}
		}
		} catch (JSONException e) {
			log.error("error",e);
		return null;
		}
	return columnsList;
}
	   @Function
	   public static List<String> getMultiFieldValues(Resource resource , String name ){
	           ValueMap map = resource.adaptTo(ValueMap.class);
	              if(map.containsKey(name)){
	               String[] columns =  map.get(name,new String[0]);
	              return Arrays.asList(columns);
	           }
	          
	       return Collections.EMPTY_LIST;
	   }
}
