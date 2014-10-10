/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.designer;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.jsp.util.TagUtil;

import tldgen.BodyContentType;
import tldgen.Tag;
import tldgen.TagAttribute;
import aQute.bnd.annotation.ProviderType;

import com.day.cq.wcm.api.designer.Design;

/**
 * JSP Tag which includes client libraries based on the design.
 *
 */
@ProviderType
@SuppressWarnings("serial")
@Tag(bodyContentType = BodyContentType.EMPTY, value = "includeClientLibraries")
public final class IncludeDesignLibrariesTag extends TagSupport {

    private PageRegion region;

    private boolean js;

    private boolean css;

    private Design design;

    /**
     * {@inheritDoc}
     */
    @Override
    public int doEndTag() throws JspException {
        final SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        final SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        final DesignHtmlLibraryManager manager = bindings.getSling().getService(DesignHtmlLibraryManager.class);
        if (manager != null) {
            try {
                if (js && css) {
                    manager.writeIncludes(request, getDesign(), region, pageContext.getOut());
                } else if (js) {
                    manager.writeJsInclude(request, getDesign(), region, pageContext.getOut());
                } else if (css) {
                    manager.writeCssInclude(request, getDesign(), region, pageContext.getOut());
                }
            } catch (IOException e) {
                throw new JspException("Unable to write client library includes", e);
            }
        }
        reset();
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        reset();
        super.release();
    }

    @TagAttribute(runtimeValueAllowed = true)
    public void setCss(boolean css) {
        this.css = css;
    }

    @TagAttribute(runtimeValueAllowed = true)
    public void setDesign(Design design) {
        this.design = design;
    }

    @TagAttribute(runtimeValueAllowed = true)
    public void setJs(boolean js) {
        this.js = js;
    }

    @TagAttribute(required = true, runtimeValueAllowed = true)
    public void setRegion(String region) {
        this.region = PageRegion.valueOf(region.toUpperCase());
    }

    private Design getDesign() {
        if (design == null) {
            return (Design) pageContext.getAttribute("currentDesign");
        } else {
            return design;
        }
    }

    private void reset() {
        this.region = null;
        this.js = false;
        this.css = false;
    }
}
