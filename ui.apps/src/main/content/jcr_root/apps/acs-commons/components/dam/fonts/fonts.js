/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
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

/*global use: false, request: false, resolver: false, Packages: false, java: false */
use(function() {
    function parseFontResource(res) {
        var properties = res.adaptTo(Packages.org.apache.sling.api.resource.ValueMap),
            fontFamily = properties.get("stFNT:fontFamily", java.lang.String),
            fontFace = properties.get("stFNT:fontFace", java.lang.String);

        if (fontFace && fontFamily) {
            if (result.fonts[fontFamily]) {
                result.fonts[fontFamily].faces.push(fontFace);
                result.fonts[fontFamily].hasMultiple = true;
            } else {
                result.fonts[fontFamily] = {
                    faces : [ fontFace ]
                };
            }
        }
    }

    var result = {
            fonts : {}
        },
        itemPath = request.getParameter("item"),
        fontsResource,
        childrenIterator;

    if (itemPath) {
        fontsResource = resolver.getResource(itemPath + "/jcr:content/metadata/xmpTPg:Fonts");
        if (fontsResource) {
            childrenIterator = resolver.listChildren(fontsResource);
            while (childrenIterator.hasNext()) {
                parseFontResource(childrenIterator.next());
            }
        }
    }

    result.hasContent = Object.keys(result.fonts).length > 0;

    return result;
});