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

import tldgen.BodyContentType;
import tldgen.Tag;
import tldgen.TagAttribute;

import com.day.cq.wcm.api.WCMMode;

/**
 * Implementation of the &lt;wcmmode:setMode&gt; tag, this sets the <code>WCMMode</code> to
 * the specified mode and restores it to the original mode.<br/>
 * The following attributes can be specified:
 * <ul>
 * <li>mode: to mode to be set</li>
 * <li>restore: must the original mode be restored (default true)</li>
 * </ul>
 * Example:<br/>
 * &lt;wcmmode:setMode mode="disabled"&gt;
 * ...
 * &lt;/wcmmode:setMode&gt;
 *
 * @see <a href="http://dev.day.com/docs/en/cq/current/javadoc/com/day/cq/wcm/api/WCMMode.html">WCMMode</a>
 */
@Tag(value = "setMode", bodyContentType = BodyContentType.JSP)
public final class SetWCMMode extends TagSupport {

    private static final long serialVersionUID = 1247938294323013878L;

    private String mode;

    private boolean restore = true;

    private WCMMode oldMode = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public int doStartTag() throws JspException {
        final WCMMode toSet = WCMMode.valueOf(mode);
        final ServletRequest request = pageContext.getRequest();
        this.oldMode = WCMMode.fromRequest(request);
        toSet.toRequest(request);
        return EVAL_BODY_INCLUDE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int doEndTag() throws JspException {
        if (restore) {
            if (oldMode != null) {
                oldMode.toRequest(pageContext.getRequest());
            } else {
                WCMMode.DISABLED.toRequest(pageContext.getRequest());
            }
        }
        reset();
        return EVAL_PAGE;
    }

    @TagAttribute(required = true, runtimeValueAllowed = true)
    public void setMode(final String mode) {
        this.mode = mode.toUpperCase();
    }

    @TagAttribute(required = false, runtimeValueAllowed = true)
    public void setRestore(final boolean restore) {
        this.restore = restore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        super.release();
        reset();
    }

    private void reset() {
        this.mode = null;
        this.restore = true;
        this.oldMode = null;
    }

}
