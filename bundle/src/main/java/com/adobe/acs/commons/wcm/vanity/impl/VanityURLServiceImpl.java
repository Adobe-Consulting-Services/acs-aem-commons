package com.adobe.acs.commons.wcm.vanity.impl;

import com.adobe.acs.commons.wcm.vanity.VanityURLService;
import com.day.cq.commons.PathInfo;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Service
public class VanityURLServiceImpl implements VanityURLService {

	private static final Logger log = LoggerFactory.getLogger(VanityURLServiceImpl.class);

    private static final String VANITY_DISPATCH_CHECK_ATTR = "acs-aem-commons__vanity-check-loop-detection";
    private static final String DEFAULT_PATH_SCOPE = "/content";

	@Reference
	QueryBuilder queryBuilder;

	public boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException, RepositoryException {
		if (request.getAttribute(VANITY_DISPATCH_CHECK_ATTR) != null) {
			log.trace("Processing a previously vanity dispatched request. Skipping...");
			return false;
		}

		request.setAttribute(VANITY_DISPATCH_CHECK_ATTR, true);

		final String requestURI = request.getRequestURI();
		final RequestPathInfo requestPathInfo = new PathInfo(request.getResourceResolver(), requestURI);

		// Manually strip off any selectors or extensions from the URL
		final String resourcePath = StringUtils.substringBefore(requestPathInfo.getResourcePath(), ".");
		// Map the incoming URL to remove any prefix
		final String candidateVanity = request.getResourceResolver().map(resourcePath);
		final String pathScope = StringUtils.removeEnd(resourcePath, candidateVanity);

		log.debug("Candidate vanity URL to check and dispatch: [ {} ]", candidateVanity);

		// Check if...
		// 1) the candidateVanity and the requestURI are the same; If they are it means the request has already
		// gone through resource resolution and failed so there is no sense in sending it through again.
		// 2) the candidate is in at least 1 sling:vanityPath under /content
		if (!StringUtils.equals(candidateVanity, requestURI) && isVanityPath(pathScope, candidateVanity, request)) {
			log.debug("Forwarding request to vanity resource [ {} ]", candidateVanity);

			final RequestDispatcher requestDispatcher = request.getRequestDispatcher(candidateVanity);
			requestDispatcher.forward(new ExtensionlessRequestWrapper(request), response);
			return true;
		}

		return false;
	}

	/**
	 * Checks if the provided vanity path is sling:vanityPath under the mapped path prefix
	 *
	 * @param pathScope The content path to scope the vanity search too.
	 * @param vanityPath Vanity path that needs to be validated.
	 * @param request SlingHttpServletRequest object used for performing query/lookup
	 * @return return true if the vanityPath is a registered sling:vanityPath under /content
	 */
	protected boolean isVanityPath(String pathScope, String vanityPath, SlingHttpServletRequest request) throws RepositoryException {
		final long start = System.currentTimeMillis();

		final Map<String, String> params = new HashMap<>();

		// Limit to <pathScope> to get 1/2 to multi-tenant support
		params.put("path", StringUtils.defaultIfEmpty(pathScope, DEFAULT_PATH_SCOPE));
		params.put("property", NameConstants.PN_SLING_VANITY_PATH);
		params.put("property.value", vanityPath);
		params.put("p.limit", "1");
		params.put("p.guessTotal", "true");

		log.debug("Searching for vanity path [ {} ] under [ {} ]", vanityPath, pathScope);

		final Query query = queryBuilder.createQuery(PredicateGroup.create(params), request.getResourceResolver().adaptTo(Session.class));
		final SearchResult result = query.getResult();

		if (result.getTotalMatches() > 0) {
			if (log.isDebugEnabled()) {
				log.debug("Found matching Sling vanity path [ {} ] on [ {} ]", vanityPath, result.getHits().get(0).getPath());
			}

			if (result.hasMore()) {
				log.warn("Found more than 1 resources that match the Sling vanity path [ {} ]", vanityPath);
			}

			// Found at least one matching vanity path; returning it!
			return true;
		} else {
			log.debug("Could not find a resource with the Sling vanity path [ {} ]", vanityPath);
		}

		log.debug("Look-up of Sling vanity path [ {} ] took [ {} ] ms", vanityPath, System.currentTimeMillis() - start);

		return false;
	}
}