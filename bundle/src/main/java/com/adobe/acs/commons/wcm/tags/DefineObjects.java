package com.adobe.acs.commons.wcm.tags;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.scripting.SlingBindings;
import tldgen.BodyContentType;
import tldgen.Tag;

import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * This tag is similar to the OOTB cq:defineObjects tag that adds
 * component instance-level properties to the pageContext of a JSP,
 * but it instead sets sharedProperties and mergedProperties maps
 * which come from
 * com.adobe.acs.commons.wcm.impl.SharedComponentPropertiesBindingsValuesProvider
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
