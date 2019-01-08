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
package com.adobe.acs.commons.wcm.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.scripting.jsp.util.TagUtil;

import tldgen.BodyContentType;
import tldgen.Tag;
import tldgen.TagAttribute;
import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.util.ModeUtil;

import com.adobe.acs.commons.wcm.ComponentHelper;
import com.adobe.acs.commons.wcm.impl.ComponentHelperImpl;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.DropTarget;
import com.day.cq.wcm.commons.WCMUtils;
import com.day.cq.wcm.foundation.Placeholder;

/**
 * JSP tag which outputs placeholders for components
 * for both Classic UI and Touch UI.
 *
 */
@ProviderType
@Tag(bodyContentType = BodyContentType.JSP, value = "placeholder")
public final class PlaceholderTag extends BodyTagSupport {

    private static final long serialVersionUID = -2497240151981056169L;

    private static final String DEFAULT_CLASS_NAME = "cq-text-placeholder";

    // NOTE - not a service lookup because (right now) ComponentHelperImpl is
    // not configured.
    private transient ComponentHelper componentHelper = new ComponentHelperImpl();
    
    private String classNames;

    private String ddType;

    private String getAllClassNames() {
        StringBuilder allClassNames = new StringBuilder();
        allClassNames.append(getDdClass());
        allClassNames.append(" ");
        if (classNames != null) {
            allClassNames.append(classNames);
        } else {
            allClassNames.append(DEFAULT_CLASS_NAME);
        }
        return allClassNames.toString().trim();
    }

    private String getDdClass() {
        if (ddType != null) {
            StringBuilder bld = new StringBuilder();
            bld.append(DropTarget.CSS_CLASS_PREFIX);
            bld.append(ddType);
            return bld.toString();
        } else {
            return "";
        }
    }

    @TagAttribute(required = false)
    public void setClassNames(String classNames) {
        this.classNames = classNames;
    }

    @TagAttribute(required = false)
    public void setDdType(String type) {
        this.ddType = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int doStartTag() throws JspException {
        SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        if (ModeUtil.isEdit(request)) {
            return EVAL_BODY_BUFFERED;
        } else {
            return SKIP_BODY;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int doEndTag() throws JspException {
        SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        Component component = WCMUtils.getComponent(request.getResource());
        if (ModeUtil.isEdit(request)) {
            JspWriter writer = pageContext.getOut();
            String placeholder;

            String bodyContentString = bodyContent != null ? bodyContent.getString() : null;
            if (StringUtils.isNotBlank(bodyContentString)) {
                // use the body content as the default placeholder
                placeholder = Placeholder.getDefaultPlaceholder(request, component, bodyContentString, getDdClass());
            } else {
                String classicUIPlaceholder = componentHelper.generateClassicUIPlaceholder(getAllClassNames(), null);
                placeholder = Placeholder.getDefaultPlaceholder(request, component, classicUIPlaceholder, getDdClass());
            }

            try {
                writer.print(placeholder);
            } catch (IOException e) {
                throw new JspException(e);
            }

        }
        reset();
        return EVAL_PAGE;
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
        this.classNames = null;
        this.ddType = null;
    }
}
