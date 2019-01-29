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
package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.util.BufferingResponse;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

@Component(
        service=Filter.class
)
@Designate(
        ocd=AemEnvironmentIndicatorFilter.Config.class
)
public class AemEnvironmentIndicatorFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AemEnvironmentIndicatorFilter.class);

    private static final String DIV_ID = "acs-commons-env-indicator";

    private static final String BASE_DEFAULT_STYLE = ";background-image:url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA3NpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNS1jMDIxIDc5LjE1NDkxMSwgMjAxMy8xMC8yOS0xMTo0NzoxNiAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo5ZmViMDk1Ni00MTMwLTQ0NGMtYWM3Ny02MjU0NjY0OTczZWIiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6MDk4RTBGQkYzMjA5MTFFNDg5MDFGQzVCQkEyMjY0NDQiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6MDk4RTBGQkUzMjA5MTFFNDg5MDFGQzVCQkEyMjY0NDQiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIChNYWNpbnRvc2gpIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6Mjc5NmRkZmItZDVlYi00N2RlLWI1NDMtNDgxNzU2ZjIwZDc1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjlmZWIwOTU2LTQxMzAtNDQ0Yy1hYzc3LTYyNTQ2NjQ5NzNlYiIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Ps64/vsAAAAkSURBVHjaYvz//z8DGjBmAAkiYWOwInQBZEFjZB0YAiAMEGAAVBk/wkPTSYQAAAAASUVORK5CYII=');"
            + "border-bottom: 1px solid rgba(0, 0, 0, .25);"
            + "box-sizing: border-box;"
            + "-moz-box-sizing: border-box;"
            + "-webkit-box-sizing: border-box;"
            + "position: fixed;"
            + "left: 0;"
            + "top: 0;"
            + "right: 0;"
            + "height: 5px;"
            + "z-index: 100000000000000;";

    private static final String TITLE_UPDATE_SCRIPT = "<script>(function() { var c = 0; t = '%s' + ' | ' + document.title, "
            + "i = setInterval(function() { if (document.title === t && c++ > 10) { clearInterval(i); } else { document.title = t; } }, 1500); "
            + "document.title = t; })();</script>\n";

    @Reference
    private XSSAPI xss;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - AEM Environment Indicator",
            description = "Adds a visual cue to the AEM WebUI indicating which environment is being access (localdev, dev, qa, staging)"
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Color",
                description = "The color of the indicator bar; takes any valid value"
                        + " for CSS's 'background-color' attribute."
                        + " This is ignored if a Style Override is provided.",
                defaultValue = ""
        )
        String css$_$color();

        @AttributeDefinition(
                name = "CSS Override",
                description = "Accepts any valid CSS to style the AEM indicator div. All CSS rules must only be "
                        + "scoped to #" + DIV_ID + " { .. }",
                defaultValue = ""
        )
        String css$_$override();

        @AttributeDefinition(
                name = "Inner HTML",
                description = "Any additional HTML required; Will be injected into a div with"
                        + " id='" + DIV_ID + "'",
                defaultValue = "")
        String inner$_$html();

        @AttributeDefinition(name = "Browser Title",
                description = "A prefix to add to the browser tab/window title; <THIS VALUE> | <ORIGINAL DOC TITLE>",
                defaultValue = DEFAULT_TITLE_PREFIX
        )
        String browser$_$title$_$prefix();

        @AttributeDefinition(
                name = "Excluded WCM modes",
                description = "Do not display the indicator when these WCM modes",
                cardinality = Integer.MAX_VALUE
        )
        String[] excluded$_$wcm$_$modes();
    }

    /* Property: Default Color */


    private String color = "";

    public static final String PROP_COLOR = "css-color";

     /* Property: CSS Override */

    private String cssOverride = "";

    public static final String PROP_CSS_OVERRIDE = "css-override";

     /* Property: Inner HTML */

    private String innerHTML = "";

    public static final String PROP_INNER_HTML = "inner-html";


    /* Property: Browser Title Prefix */

    private static final String DEFAULT_TITLE_PREFIX = "";

    private String titlePrefix = DEFAULT_TITLE_PREFIX;

    public static final String PROP_TITLE_PREFIX = "browser-title-prefix";

    private static final String[] DEFAULT_EXCLUDED_WCMMODES = {"DISABLED"};

    public static final String PROP_EXCLUDED_WCMMODES = "excluded-wcm-modes";
    private String[] excludedWCMModes;

    private static final String[] REJECT_PATH_PREFIXES = new String[]{};

    private String css = "";

    private ServiceRegistration filterRegistration;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                               final FilterChain filterChain) throws IOException, ServletException {

        if (!(servletRequest instanceof HttpServletRequest)
                || !(servletResponse instanceof HttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!this.accepts(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final BufferingResponse capturedResponse = new BufferingResponse(response);

        filterChain.doFilter(request, capturedResponse);

        boolean doInclude = true;
        if (ArrayUtils.isNotEmpty(excludedWCMModes)) {
            // Test for configured WCM modes, where the indicators are not displayed
            WCMMode wcmmode = extractFromRequest(request);

            if (wcmmode != null) {
                for (String m : excludedWCMModes) {
                    if (StringUtils.equalsIgnoreCase(wcmmode.name(), m)) {
                        doInclude = false;
                        break;
                    }
                }
            } else {
                // No wcmmode could be extracted from the request
            }
        }

        // Get contents
        final String contents = capturedResponse.getContents();

        if (doInclude && contents != null && StringUtils.contains(response.getContentType(), "html")) {

            final int bodyIndex = contents.indexOf("</body>");

            if (bodyIndex != -1) {
                final PrintWriter printWriter = response.getWriter();

                printWriter.write(contents.substring(0, bodyIndex));

                if (StringUtils.isNotBlank(css)) {
                    printWriter.write("<style>" + css + " </style>");
                    printWriter.write("<div id=\"" + DIV_ID + "\">" + innerHTML + "</div>");
                }

                if (StringUtils.isNotBlank(titlePrefix)) {
                    printWriter.printf(TITLE_UPDATE_SCRIPT, titlePrefix);
                }

                printWriter.write(contents.substring(bodyIndex));
                return;
            }
        }

        if (contents != null) {
            response.getWriter().write(contents);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    @SuppressWarnings("squid:S3923")
    protected boolean accepts(final HttpServletRequest request) {
        if (StringUtils.isBlank(css) && StringUtils.isBlank(titlePrefix)) {
            // Only accept is properly configured
            log.warn("AEM Environment Indicator is not properly configured; If this feature is unwanted, "
                    + "remove the OSGi configuration and disable completely.");
            return false;
        } else if (!StringUtils.equalsIgnoreCase("get", request.getMethod())) {
            // Only inject on GET requests
            return false;
        } else if (StringUtils.startsWithAny(request.getRequestURI(), REJECT_PATH_PREFIXES)) {
            // Reject any request to well-known rejection-worthy path prefixes
            return false;
        } else if (StringUtils.equals(request.getHeader("X-Requested-With"), "XMLHttpRequest")) {
            // Do not inject into XHR requests
            return false;
        } else if (StringUtils.endsWith(request.getHeader("Referer"), "/editor.html" + request.getRequestURI())) {
            // Do not apply to pages loaded in the TouchUI editor.html
            return false;
        } else if (StringUtils.endsWith(request.getHeader("Referer"), "/cf")) {
            // Do not apply to pages loaded in the Classic Content Finder
            return false;
        }
        return true;
    }

    private String createCss(final String providedColor) {
        return "#" + DIV_ID + " { "
                + "background-color:" + providedColor + BASE_DEFAULT_STYLE
                + " }";
    }

    @Activate
    @SuppressWarnings("squid:S1149")
    protected final void activate(ComponentContext ctx) {
        Dictionary<?, ?> config = ctx.getProperties();

        color = PropertiesUtil.toString(config.get(PROP_COLOR), "");
        cssOverride = PropertiesUtil.toString(config.get(PROP_CSS_OVERRIDE), "");
        innerHTML = PropertiesUtil.toString(config.get(PROP_INNER_HTML), "");
        innerHTML = new StrSubstitutor(StrLookup.systemPropertiesLookup()).replace(innerHTML);

        // Only write CSS variable if cssOverride or color is provided
        if (StringUtils.isNotBlank(cssOverride)) {
            css = cssOverride;
        } else if (StringUtils.isNotBlank(color)) {
            css = createCss(color);
        }

        titlePrefix = xss.encodeForJSString(
                PropertiesUtil.toString(config.get(PROP_TITLE_PREFIX), "").toString());

        if (StringUtils.isNotBlank(css) || StringUtils.isNotBlank(titlePrefix)) {
            Dictionary<String, String> filterProps = new Hashtable<String, String>();
            filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/");
            filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)");
            filterRegistration = ctx.getBundleContext().registerService(Filter.class.getName(), this, filterProps);
        }

        excludedWCMModes = PropertiesUtil.toStringArray(config.get(PROP_EXCLUDED_WCMMODES),DEFAULT_EXCLUDED_WCMMODES);
    }


    @Deactivate
    protected final void deactivate(final Map<String, String> config) {
        if (filterRegistration != null) {
            filterRegistration.unregister();
            filterRegistration = null;
        }

        // Reset CSS variable
        css = "";
    }

    // extract the WCMMode from the request; we cannot use
    // WCMMode.fromRequest(), because this is not a SlingHttpServletRequest
    private WCMMode extractFromRequest(HttpServletRequest request) {

        return (WCMMode) request.getAttribute(
                WCMMode.REQUEST_ATTRIBUTE_NAME);
    }
}
