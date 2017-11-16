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
package com.adobe.acs.commons.util;

import aQute.bnd.annotation.ProviderType;

import com.day.cq.commons.jcr.JcrConstants;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;

import java.io.IOException;
import java.io.InputStream;

@ProviderType
@SuppressWarnings({"checkstyle:abbreviationaswordinname", "squid:S1118"})
public class ResourceDataUtil {
    public static final String ENCODING_UTF_8 = "UTF-8";
    private static final Logger log = LoggerFactory.getLogger(ResourceDataUtil.class);

    public static String getIncludeAsString(final String path, final SlingHttpServletRequest slingRequest,
                                            final SlingHttpServletResponse slingResponse) {
        StringWriterResponse responseWrapper = null;

        try {
            responseWrapper = new StringWriterResponse(slingResponse);
            final RequestDispatcher requestDispatcher = slingRequest.getRequestDispatcher(path);

            requestDispatcher.include(slingRequest, responseWrapper);

            return StringUtils.stripToNull(responseWrapper.getString());
        } catch (Exception ex) {
            log.error("Error creating the String representation for: " + path, ex);
        } finally {
            if (responseWrapper != null) {
                responseWrapper.clearWriter();
            }
        }

        return null;
    }

    public static InputStream getNTFileAsInputStream(final String path, final ResourceResolver resourceResolver) throws RepositoryException {
        return getNTFileAsInputStream(resourceResolver.resolve(path));
    }

    public static InputStream getNTFileAsInputStream(final Resource resource) throws RepositoryException {
        final Node node = resource.adaptTo(Node.class);
        final Node jcrContent = node.getNode(JcrConstants.JCR_CONTENT);
        return jcrContent.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
    }

    public static String getNTFileAsString(final String path, final ResourceResolver resourceResolver) throws RepositoryException, IOException {
        return getNTFileAsString(path, resourceResolver, ENCODING_UTF_8);
    }

    public static String getNTFileAsString(final String path, final ResourceResolver resourceResolver, final String encoding) throws RepositoryException, IOException {
        return getNTFileAsString(resourceResolver.resolve(path), encoding);
    }

    public static String getNTFileAsString(final Resource resource) throws RepositoryException, IOException {
        return getNTFileAsString(resource, ENCODING_UTF_8);
    }

    public static String getNTFileAsString(final Resource resource, final String encoding) throws RepositoryException, IOException {
        final InputStream inputStream = getNTFileAsInputStream(resource);
        return IOUtils.toString(inputStream, encoding);
    }
}