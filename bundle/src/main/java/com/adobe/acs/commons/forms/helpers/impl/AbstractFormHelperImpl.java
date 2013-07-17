package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFormHelperImpl implements FormHelper {
	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
	public String getFormInputsHTML(final Form form, final String... keys)
			throws IOException {
		String html = "";

        html += "<input type=\"hidden\" name=\"" + FORM_NAME_INPUT + "\" value=\""
                + form.getName() + "\"/>\n";

		for (final String key : keys) {
			if (form.has(key)) {
				html += "<input type=\"hidden\" name=\"" + key + "\" value=\""
						+ form.get(key) + "\"/>\n";
			}
		}

		return html;
	}

    @Override
    public String getAction(final Resource resource) {
        return resource.getPath() + ".post";
    }

    /**
     *
     * @param formName
     * @param request
     * @return
     */
    protected boolean doHandlePost(final String formName, final HttpServletRequest request) {
        if(StringUtils.equalsIgnoreCase("POST", request.getMethod())) {
            // Form should have a hidden input with the name this.getLookupKey(..) and value formName
            return StringUtils.equals(formName, request.getParameter(this.getPostLookupKey(formName)));
        } else {
            return false;
        }
    }

    /**
     *
     * @param formName
     * @param request
     * @return
     */
    protected Form getPostForm(final String formName,
                            final HttpServletRequest request) {
        final Map<String, String> map = new HashMap<String, String>();

        if(request instanceof SlingHttpServletRequest) {
            log.debug("Request is a SlingHttpServletRequest");

            final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            final RequestParameterMap requestMap = slingRequest.getRequestParameterMap();

            for (final String key : requestMap.keySet()) {
                if(StringUtils.equals(key, this.getPostLookupKey(formName))) { continue; }

                final RequestParameter[] values = requestMap.getValues(key);

                if (values == null || values.length == 0) {
                    continue;
                } else if (values.length == 1) {
                    map.put(key, values[0].getString());
                } else {
                    for (int i = 0; i < values.length; i++) {
                        map.put(key + "_" + i, values[i].getString());
                    }
                }
            }
        } else {
            log.debug("Request is NOT a SlingHttpServletRequest");

            for(final Object obj : request.getParameterMap().keySet()) {
                if(obj instanceof String) {
                    final String key = (String) obj;

                    if(StringUtils.equals(key, this.getPostLookupKey(formName))) { continue; }
                    map.put(key, request.getParameter(key));
                }
            }
        }

        return new Form(formName, map);
    }

    /**
     *
     * @param formName
     * @return
     */
    protected String getPostLookupKey(final String formName) {
        return FORM_NAME_INPUT;
    }

    /**
     * Removes unused Map entries from the provided map
     *
     * @param form
     * @return
     */
    protected Form clean(final Form form) {
        final Map<String, String> map = form.getData();
        final Map<String, String> cleanedMap = new HashMap<String, String>();
        for (final String key : map.keySet()) {
            if(StringUtils.equals(key, FORM_NAME_INPUT)) {
                continue;
            } else if (StringUtils.isNotBlank(map.get(key))) {
                cleanedMap.put(key, map.get(key));
            }
        }
        return new Form(form.getName(), cleanedMap, form.getErrors());
    }

    /**
     * Internal method for encoding URL data
     *
     * @param unencoded
     * @return
     */
    protected String encode(String unencoded) {
        try {
            return java.net.URLEncoder.encode(unencoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return unencoded;
        }
    }

    /**
     * Internal method for decoding URL data
     *
     * @param encoded
     * @return
     */
    protected String decode(String encoded) {
        try {
            return java.net.URLDecoder.decode((encoded), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return encoded;
        }
    }
}