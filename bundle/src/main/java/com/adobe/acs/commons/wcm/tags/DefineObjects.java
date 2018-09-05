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
package com.adobe.acs.commons.wcm.tags;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.scripting.SlingBindings;
import tldgen.BodyContentType;
import tldgen.Tag;

import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * This tag is similar to the OOTB cq:defineObjects tag that adds
 * component instance-level properties to the pageContext of a JSP,
 * but it instead adds globalProperties, sharedProperties, and
 * mergedProperties maps that come from
 * com.adobe.acs.commons.wcm.properties.shared.impl.SharedComponentPropertiesBindingsValuesProvider
 */
@ProviderType
@Tag(bodyContentType = BodyContentType.JSP, value = "defineObjects")
public class DefineObjects extends BodyTagSupport {

    @Override
    public int doEndTag() {
        SlingBindings bindings = (SlingBindings)this.pageContext.getRequest().getAttribute(SlingBindings.class.getName());
        pageContext.setAttribute("globalProperties", bindings.get("globalProperties"));
        pageContext.setAttribute("sharedProperties", bindings.get("sharedProperties"));
        pageContext.setAttribute("mergedProperties", bindings.get("mergedProperties"));

        return EVAL_PAGE;
    }
}
