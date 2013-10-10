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
package com.adobe.acs.commons.wcm.tags.wcmmode;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import tldgen.TagAttribute;

import com.day.cq.wcm.api.WCMMode;

/**
 * Abstract class used to implement the other tags for the WcmMode.
 * The WcmMode tags can be used in your JSP to show/hide particular bits
 * for a certain WcmMode.
 *
 */
abstract class AbstractMode extends TagSupport {

    private static final long serialVersionUID = -5908186805353457797L;

    private boolean not = false;

    @Override
    public final int doEndTag() throws JspException {
        reset();
        return EVAL_PAGE;
    }

    @Override
    public final int doStartTag() throws JspException {
        if (!isRequestInMode()) {
            return SKIP_BODY;
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public void release() {
        super.release();
        reset();
    }

    @TagAttribute(runtimeValueAllowed = true, required = false)
    public final void setNot(final boolean not) {
        this.not = not;
    }

    private boolean isRequestInMode() {
        final ServletRequest request = pageContext.getRequest();

        if (not) {
            return WCMMode.fromRequest(request) != getMode();
        } else {
            return WCMMode.fromRequest(request) == getMode();
        }
    }

    private void reset() {
        this.not = false;
    }

    abstract WCMMode getMode();

}
