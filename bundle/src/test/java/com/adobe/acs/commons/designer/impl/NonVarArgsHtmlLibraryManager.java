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
package com.adobe.acs.commons.designer.impl;

import java.io.Writer;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Mockito doesn't like varargs yet.
 * @see https://code.google.com/p/mockito/issues/detail?id=372
 *
 */
public interface NonVarArgsHtmlLibraryManager {

    void writeJsInclude(SlingHttpServletRequest request, Writer out, String[] categories);

    void writeCssInclude(SlingHttpServletRequest request, Writer out, String[] categories);
    
}