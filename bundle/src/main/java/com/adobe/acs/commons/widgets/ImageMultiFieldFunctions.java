/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.widgets;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;

import tldgen.Function;

import com.day.cq.wcm.foundation.Image;

/**
 * JSP functions for working with ImageMultiField widget.
 */
public class ImageMultiFieldFunctions {
    @Function
    public static List<Image> getImagesFromImageMultiField(Resource resource, String name) {
        List<Image> images = new ArrayList<Image>();
        Resource imagesResource = resource.getChild(name);
        if (imagesResource != null) {
            ValueMap map = imagesResource.adaptTo(ValueMap.class);
            String order = map.get("order", String.class);
            if (order != null) {
                JSONArray array;
                try {
                    array = new JSONArray(order);
                } catch (JSONException e) {
                    // order can't be parsed, return empty array
                    array = new JSONArray();
                }

                for (int i = 0; i < array.length(); i++) {
                    String imageResourceName;
                    try {
                        imageResourceName = array.getString(i);
                    } catch (JSONException e) {
                        // array item isn't readable; skip
                        imageResourceName = null;
                    }

                    if (imageResourceName != null) {
                        Resource imageResource = imagesResource.getChild(imageResourceName);
                        if (imageResource != null) {

                            Image img = new Image(imageResource);
                            img.setItemName(Image.PN_REFERENCE, "imageReference");
                            img.setSelector("img");
                            img.setAlt(imageResource.getName());

                            images.add(img);
                        }
                    }
                }
            }
        }

        return images;
    }
}
