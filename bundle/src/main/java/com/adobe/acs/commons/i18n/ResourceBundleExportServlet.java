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
package com.adobe.acs.commons.i18n;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.WeakHashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;

import com.adobe.acs.commons.i18n.impl.ETagHelper;
import com.adobe.acs.commons.i18n.impl.JsonExporter;
import com.adobe.acs.commons.i18n.impl.LocaleUtil;

/**
 * The <code>ResourceBundleExportServlet</code> exports all translations found
 * in the repository via the i18n ResourceBundle from the request. This does not
 * support import because it is not possible to "globally" import into the
 * ResourceBundle (which is an abstraction). For import, one has to specifically
 * address eg. a path in the JCR and use the {@link DictImportExportServlet}
 * instead.
 * The ResourceBundlerExportServlet supports the sling:basename property
 */

@SlingServlet(paths = "/bin/acs-commons/components/utilities/i18n/dict", extensions="json", methods = "GET")
public class ResourceBundleExportServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = -7329439205765482666L;
	
	private static final String DEFAULT_LOCALE = "en";

	private static final String DEFAULT_BASENAME = "";


	private ResourceBundleExporter defaultExporter = new JsonExporter();

	private WeakHashMap<ResourceBundle, String> bundleChecksumCache = new WeakHashMap<ResourceBundle, String>();

	@Override
	protected void doGet(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws IOException {

		String format = request.getRequestPathInfo().getExtension();
		ResourceBundle bundle;

		if (defaultExporter != null) {
			bundle = getResourceBundle(request);

			response.setHeader("Cache-Control", "public");

			// it's ok to calculate the checksum on the "resource" directly (the
			// ResourceBundle in memory)
			// and not the "representation" entities (such as defined by the
			// exporters, e.g. json output)
			// as the different entities are behind different URLs with
			// different extensions
			if (ETagHelper.handleETag(request, response, getChecksum(bundle))) {
				return;
			}

			defaultExporter.export(bundle, response);

		} else {
			// unknown format
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
					"Format " + format + " not supported");
		}
	}

	/**
	 * Retrieve the resourceBundle based on the request selectors:
	 * The locale is the first selector
	 * The optional basename is retrieved as the second selector
	 * 
	 * @return the selected ResourceBunlde
	 */
	private ResourceBundle getResourceBundle(SlingHttpServletRequest request) {
		// get the required resource bundle
		String[] selectorsArray = request.getRequestPathInfo().getSelectors();
		String localeString = DEFAULT_LOCALE;
		String basenameString = DEFAULT_BASENAME;
		if (selectorsArray != null) {
			if (selectorsArray.length >= 1)
				localeString = selectorsArray[0];
			if (selectorsArray.length >= 2)
				basenameString = selectorsArray[1];
		}
		Locale locale = (localeString != null) ? LocaleUtil
				.parseLocale(localeString) : null;
		if (StringUtils.isNotEmpty(basenameString))
			return request.getResourceBundle(basenameString, locale);
		return request.getResourceBundle(locale);
	}

	private String getChecksum(ResourceBundle bundle) {
		synchronized (bundleChecksumCache) {
			final String checksum = bundleChecksumCache.get(bundle);
			if (checksum != null) {
				return checksum;
			}
		}

		final String checksum = calculateChecksum(bundle);
		if (checksum != null) {
			synchronized (bundleChecksumCache) {
				bundleChecksumCache.put(bundle, checksum);
			}
		}
		return checksum;
	}

	/**
	 * Calculates a MD5 checksum of a ResourceBundle by going through all keys
	 * and values in the resource bundle (expecting Strings) in the order as
	 * returned by getKeys() and taking an MD5 of the (virtually) concatenated
	 * string.
	 */
	private String calculateChecksum(ResourceBundle bundle) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			Enumeration<String> keys = bundle.getKeys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				md5.update(key.getBytes("utf-8"));
				md5.update(bundle.getObject(key).toString().getBytes("utf-8"));
			}
			// return md5 as hex string
			return (new BigInteger(1, md5.digest())).toString(16);

		} catch (NoSuchAlgorithmException e) {
			throw new SlingException(
					"MessageDigest does not support MD5 algorithm", e);
		} catch (UnsupportedEncodingException e) {
			throw new SlingException(
					"String.getBytes() does not support utf-8 charset", e);
		}
	}

	// ---------- SCR integration ----------------------------------------------

	@Activate
	protected void activate(final BundleContext context) {
	}

	@Deactivate
	protected void deactivate() {
		// remove all helpers
	}
}
