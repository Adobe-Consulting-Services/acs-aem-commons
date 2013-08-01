package com.adobe.acs.commons.forms.helpers.impl;

import com.adobe.acs.commons.forms.Form;
import com.adobe.acs.commons.forms.helpers.PostFormHelper;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.granite.xss.XSSAPI;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(label = "ACS AEM Commons - Abstract Form Helper", description = "Abstract Form Helper. Do not use directly; instead use the PRGFormHelper or ForwardFormHelper.", enabled = true, metatype = false, immediate = false)
@Properties({ @Property(label = "Vendor", name = Constants.SERVICE_VENDOR, value = "ACS", propertyPrivate = true) })
@Service(value = PostFormHelper.class)
public class PostFormHelperImpl implements FormHelper {
	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Reference
    protected XSSAPI xssApi;

    /**
     * OSGi Properties *
     */
    private static final String DEFAULT_SUFFIX = "/acs/form";
    private String suffix = DEFAULT_SUFFIX;
    @Property(label = "Suffix", description = "Forward-as-GET Request Suffix used to identify Forward-as-GET POST Request", value = DEFAULT_SUFFIX)
    private static final String PROP_SUFFIX = "prop.suffix";

    @Override
    public Form getForm(String formName, SlingHttpServletRequest request) {
        throw new UnsupportedOperationException("Do not call AbstractFormHelper.getForm(..) direct. This is an abstract service.");
    }

    @Override
	public String getFormInputsHTML(final Form form, final String... keys)
			throws IOException {
        // The form obj's data and errors should be xssProtected before being passed into this method
		String html = "";

        html += "<input type=\"hidden\" name=\"" + FORM_NAME_INPUT + "\" value=\""
                + xssApi.encodeForHTMLAttr(form.getName()) + "\"/>\n";

        final String resourcePath = form.getResource().getPath();
        html += "<input type=\"hidden\" name=\"" + FORM_RESOURCE_INPUT + "\" value=\""
                + xssApi.encodeForHTMLAttr(resourcePath) + "\"/>\n";

		for (final String key : keys) {
			if (form.has(key)) {
				html += "<input type=\"hidden\" name=\"" + key + "\" value=\""
						+ form.get(key) + "\"/>\n";
			}
		}

		return html;
	}

    @Override
    public String getAction(final String path) {
        return path + FormHelper.EXTENSION + this.getSuffix();
    }

    @Override
    public String getAction(final Resource resource) {
        return getAction(resource.getPath());
    }

    @Override
    public String getAction(final Page page) {
        return getAction(page.getPath());
    }

    @Override
    public String getSuffix() {
        return this.suffix;
    }

    protected boolean doHandlePost(final String formName, final SlingHttpServletRequest request) {
        if(StringUtils.equalsIgnoreCase("POST", request.getMethod())) {
            // Form should have a hidden input with the name this.getLookupKey(..) and value formName
            return StringUtils.equals(formName, request.getParameter(this.getPostLookupKey(formName)));
        } else {
            return false;
        }
    }

    protected Form getPostForm(final String formName,
                            final SlingHttpServletRequest request) {
        final Map<String, String> map = new HashMap<String, String>();


        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final RequestParameterMap requestMap = slingRequest.getRequestParameterMap();

        for (final String key : requestMap.keySet()) {
            // POST LookupKey formName param does not matter
            if(StringUtils.equals(key, this.getPostLookupKey(null))) { continue; }

            final RequestParameter[] values = requestMap.getValues(key);

            if (values == null || values.length == 0) {
                log.debug("Value did not exist for key: {}", key);

                continue;
            } else if (values.length == 1) {
                log.debug("Adding to form data: {} ~> {}", key, values[0].toString());
                map.put(key, values[0].getString());
            } else {
                final List<String> list = new ArrayList<String>();
                for(final RequestParameter value : values) {
                    list.add(value.toString());                    
                }
                map.put(key, StringUtils.join(list, ","));
            }
        }

        return new Form(formName, request.getResource(), map);
    }

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
            } else if(StringUtils.equals(key, FORM_RESOURCE_INPUT)) {
                continue;
            } else if (StringUtils.isNotBlank(map.get(key))) {
                cleanedMap.put(key, map.get(key));
            }
        }
        return new Form(form.getName(), form.getResource(), cleanedMap, form.getErrors());
    }

    /**
     * Protect a Form in is entirety (data and errors)
     *
     * @param form
     * @return
     */
    protected Form getProtectedForm(final Form form) {
        return new Form(form.getName(),
                form.getResource(),
                this.getProtectedData(form.getData()),
                this.getProtectedErrors(form.getErrors()));
    }

    /**
     * Protect a Map representing Form Data
     *
     * @param data
     * @return
     */
    protected Map<String, String> getProtectedData(final Map<String, String> data) {
        final Map<String, String> protectedData = new HashMap<String, String>();

        // Protect data for HTML Attributes
        for (final String key : data.keySet()) {
            protectedData.put(key, xssApi.encodeForHTMLAttr(data.get(key)));
        }

        return protectedData;
    }

    /**
     * Protect a Map representing Form Errors
     *
     * @param errors
     * @return
     */
    protected Map<String, String> getProtectedErrors(final Map<String, String> errors) {
        final Map<String, String> protectedErrors = new HashMap<String, String>();

        // Protect data for HTML
        for (final String key : errors.keySet()) {
            protectedErrors.put(key, xssApi.encodeForHTML(errors.get(key)));
        }

        return protectedErrors;
    }

    /**
     * Internal method for encoding URL data
     *
     * @param unencoded
     * @return
     */
    protected String encode(String unencoded) {
        if(StringUtils.isBlank(unencoded)) {
            log.debug("Data to encode is blank.");
            return "";
        }

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
        if(StringUtils.isBlank(encoded)) {
            log.debug("Data to decode is blank.");
            return "";
        }

        try {
            return java.net.URLDecoder.decode((encoded), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return encoded;
        }
    }

    /**
     * OSGi Component Methods *
     */
    @Activate
    protected void activate(final Map<String, String> properties) throws Exception {
        this.suffix = PropertiesUtil.toString(properties.get(PROP_SUFFIX), DEFAULT_SUFFIX);
        if(StringUtils.isBlank(this.suffix)) {
            // No whitespace please
            this.suffix = DEFAULT_SUFFIX;
        }
    }
}