/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.components.longformtext;

import org.osgi.annotation.versioning.ProviderType;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface LongFormTextComponent {
    String LONG_FORM_TEXT_PAR = "long-form-text-par-";

    /**
     * Splits long form text into a series of "chunks" delimited.
     *
     * Note: this method does not support intelligence for managing nested &lt;p&gt;'s in &lt;p&gt;'s
     *
     * @param text The HTML text to split
     * @return A string array of HTML paragraphs including the surrounding &lt;p&gt;...&lt;/p&gt;
     */
    String[] getTextParagraphs(String text);

    /**
     * Housekeeping for the Long Form Text responsible for merging dangling par resource up when
     * the number of &lt;p&gt; decreases.
     *
     * @param resource the long form text resource
     * @param numParagraphCount the number of text paragraphs
     * @throws javax.jcr.RepositoryException
     */
    void mergeParagraphSystems(Resource resource, int numParagraphCount) throws RepositoryException;


    /**
     * Determines if the specified long form text parsys has content.
     *
     * @param resource the long form text resource
     * @param index the index of the long-form-text parsys to inspect
     * @return true to include the parsys
     */
    boolean hasContents(Resource resource, int index);
}
