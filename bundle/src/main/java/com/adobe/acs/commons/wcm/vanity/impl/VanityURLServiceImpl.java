package com.adobe.acs.commons.wcm.vanity.impl;

import java.util.Iterator;

import javax.jcr.query.Query;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.wcm.vanity.VanityURLService;
import com.day.cq.wcm.api.NameConstants;

@Component(
        label = "ACS AEM Commons - Vanity URL Service",
        description = "This service provides business methods around AEM Vanity URL functionality.",
        immediate = false, metatype = false)
@Service
public class VanityURLServiceImpl implements VanityURLService {

	private static final Logger log = LoggerFactory.getLogger(VanityURLServiceImpl.class);

	private static final String VANITY_DISPATCH_CHECK_ATTR = "vanity-dispatch-check";

	@Override
	public boolean isValidVanityURL(String vanityPath, SlingHttpServletRequest request) {

		boolean result = false;

		final ResourceResolver resolver = request.getResourceResolver();

		if (StringUtils.isNotBlank(vanityPath)) {
			String xpath = "//element(*)[" + NameConstants.PN_SLING_VANITY_PATH + "='" + vanityPath + "']";
			@SuppressWarnings("deprecation")
			Iterator<Resource> resources = resolver.findResources(xpath, Query.XPATH);
			while (resources.hasNext()) {
				result = true;
			}
		}

		log.debug("Is given URI {} a valid Vanity Path? --> {}", vanityPath, result);

		return result;
	}

	@Override
	public boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response) {

		boolean hasDispatched = false;

		ResourceResolver resolver = request.getResourceResolver();

		try {

			if (request.getAttribute(VANITY_DISPATCH_CHECK_ATTR) == null) {

				request.setAttribute(VANITY_DISPATCH_CHECK_ATTR, true);

				String requestURI = ((HttpServletRequest) request).getRequestURI();

				String candidateVanity = resolver.map(request, requestURI);
				candidateVanity = FilenameUtils.removeExtension(candidateVanity);

				if (!StringUtils.equals(candidateVanity, requestURI) && isValidVanityURL(candidateVanity, request)) {

					RequestDispatcher requestDispathcher = request.getRequestDispatcher(candidateVanity);
					log.debug("Forwarding current request to {} using RequestDispatcher, as it's a valid vanity:"
							+ candidateVanity);

					requestDispathcher.forward(request, response);

					hasDispatched = true;
				}
			}

		} catch (Exception e) {
			log.error("Error while dispatching request to Vanity URL, details are:", e);
		}

		return hasDispatched;
	}
}
