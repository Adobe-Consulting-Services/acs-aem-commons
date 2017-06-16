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

    private static final String VANITY_DISPATCH_CHECK_ATTR = "acs-aem-commons__vanity-dispatch-check";
    private static final String HTML_EXTENSION = ".html";

	@Reference
	QueryBuilder queryBuilder;

	public boolean isVanityPath(String vanityPath, SlingHttpServletRequest request) throws RepositoryException {
		final long start = System.currentTimeMillis();

		final Map<String, String> params = new HashMap<String, String>();

		// Limit to /content as this could show up in /jcr:system version nodes, etc.
		params.put("path", "/content");
		params.put("property", NameConstants.PN_SLING_VANITY_PATH);
		params.put("property.value", vanityPath);
		params.put("p.limit", "1");
		params.put("p.guessTotal", "true");

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
			log.debug("Could not find a resource with the Sling vanity path [ {}  ]", vanityPath);
		}

		log.debug("Look-up of Sling vanity path [ {} ] took [ {} ] ms", vanityPath, System.currentTimeMillis() - start);

		return false;
	}

	public boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException, RepositoryException {
		if (request.getAttribute(VANITY_DISPATCH_CHECK_ATTR) != null) {
			log.trace("Processing a previously vanity dispatched request. Skipping...");
			return false;
		}

		request.setAttribute(VANITY_DISPATCH_CHECK_ATTR, true);

		final String requestURI = request.getRequestURI();
		final RequestPathInfo requestPathInfo = new PathInfo(request.getResourceResolver(), requestURI);
		final String candidateVanity = StringUtils.removeEnd(requestPathInfo.getResourcePath(), HTML_EXTENSION);

		if (!StringUtils.equals(candidateVanity, requestURI) && isVanityPath(candidateVanity, request)) {
			log.debug("Forwarding request to vanity resource [ {} ]", candidateVanity);

			final RequestDispatcher requestDispatcher = request.getRequestDispatcher(candidateVanity);
			requestDispatcher.forward(new ExtensionlessRequestWrapper(request), response);
			return true;
		}

		return false;
	}
}