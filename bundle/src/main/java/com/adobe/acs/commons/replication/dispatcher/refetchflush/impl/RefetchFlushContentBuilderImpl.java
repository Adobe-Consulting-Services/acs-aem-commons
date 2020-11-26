/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.replication.dispatcher.refetchflush.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.granite.logging.LogLevel;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

/**
 * Custom dispatcher flush content builder that sends a list of URIs to be re-fetched immediately upon flushing a page.
 */
@Component(
		label = "ACS AEM Commons - Dispatcher Flush with Re-Fetch",
		description = "Instead of deleting pages from the Dispatcher cache, update the last modified time (.stat) "
				+ "of the targeted file, and trigger an immediate request of the page.",
		configurationFactory = true,
		metatype = true,
		policy = ConfigurationPolicy.REQUIRE
)
@Properties({
		@Property(name = Constants.SERVICE_DESCRIPTION, value="ACS Commons Re-Fetch Flush Content Builder"),
		@Property(name = ContentBuilder.PROPERTY_NAME, value= RefetchFlushContentBuilderImpl.NAME,
				label = "Read Only Name", description = "Service Name for Dispatcher Flush Re-Fetch"),
		@Property(
				name = "webconsole.configurationFactory.nameHint",
				value = "Extension Mapping: [{prop.extension-pairs}] Match: [{prop.match-paths}]"
		)
})
@Service(ContentBuilder.class)
public class RefetchFlushContentBuilderImpl implements ContentBuilder {

	private final Logger logger = LoggerFactory.getLogger(RefetchFlushContentBuilderImpl.class);
	private ReplicationLog replicationLog;

	public static final String NAME = "flush_refetch";
	public static final String TITLE = "Dispatcher Flush Re-fetch";

	private Map<String, String[]> extensionPairs = new LinkedHashMap<>();
	private String[] pathMatches = DEFAULT_MATCH_PATH;

	/* Regex to indicate which paths to re-fetch. */
	private static final String[] DEFAULT_MATCH_PATH = {"*"};
	@Property(
			label = "Path Pattern",
			description = "Specify a regex to match paths to be included in the re-fetch flush "
					+ "(i.e. * for all paths, /content/.* for all paths under /content, .*.html for all paths "
					+ "with html as its extension)",
			cardinality = Integer.MAX_VALUE,
			value = {"*"})
	private static final String PROP_MATCH_PATH = "prop.match-paths";

	/* Extension Pairs */
	private static final String[] DEFAULT_EXTENSION_PAIRS = {};
	@Property(
			label = "Extension Pairs",
			description = "To activate paired pages with re-fetch, specify the original extension (i.e. html) and "
					+ "map it to any other extensions (i.e. header_include.html)",
			cardinality = Integer.MAX_VALUE)
	private static final String PROP_EXTENSION_PAIRS = "prop.extension-pairs";

	private static final String SERVICE_NAME = "flush_refetch";
	protected static final Map<String, Object> AUTH_INFO;
	static {
		AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
	}

	@Activate
	protected void activate(final Map<String, Object> properties) {
		this.extensionPairs = this.configureExtensions(ParameterUtil.toMap(
				PropertiesUtil.toStringArray(properties.get(PROP_EXTENSION_PAIRS),
						DEFAULT_EXTENSION_PAIRS), "=", false, null, false));

		logMessage("Extension Pairs [" +  mapToString(this.extensionPairs) + "]", LogLevel.INFO);

		ArrayList<String> validMatches = new ArrayList<>();
		String[] matchProps = PropertiesUtil.toStringArray(properties.get(PROP_MATCH_PATH), DEFAULT_MATCH_PATH);
		if (matchProps.length > 0) {
			for (String match : matchProps) {
				if (StringUtils.isNotEmpty(match)) {
					validMatches.add(match);
				}
			}
		}

		this.pathMatches = validMatches.toArray(new String[0]);

		logMessage("Match Path Patterns [" +  String.join(",", this.pathMatches) + "]", LogLevel.INFO);
	}

	@SuppressWarnings("unused")
	@Deactivate
	protected final void deactivate(final Map<String, Object> properties) {
		this.extensionPairs = new HashMap<>();
		this.pathMatches = DEFAULT_MATCH_PATH;
	}

	private Map<String, String[]> configureExtensions(final Map<String, String> configuredExtensions) {
		final Map<String, String[]> extensions = new LinkedHashMap<>();

		for (final Map.Entry<String, String> entry : configuredExtensions.entrySet()) {
			final String ext = entry.getKey().trim();
			extensions.put(ext, entry.getValue().trim().split("&"));
		}

		return extensions;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReplicationContent create(Session session, ReplicationAction replicationAction,
									 ReplicationContentFactory factory, Map<String, Object> options)
			throws ReplicationException {
		return create(session, replicationAction, factory);
	}

	public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory factory)
			throws ReplicationException {
		replicationLog = action.getLog();
		if (replicationLog == null) {
			AgentConfig config = action.getConfig();
			logMessage("No replication log found on agent " +
							(config == null || config.getAgentId() == null ? "." : action.getConfig().getAgentId()),
					LogLevel.WARN);
		}

		if (action.getType() != ReplicationActionType.ACTIVATE &&
				action.getType() != ReplicationActionType.TEST) {
			logMessage("No re-fetch handling for replication action " + action.getType().getName(),
					LogLevel.ERROR);
			throw new ReplicationException("No re-fetch handling for replication action " +
					action.getType().getName());
		}

		String path = action.getPath();
		if (StringUtils.isEmpty(path)) {
			logMessage("No path found for re-fetch replication.", LogLevel.ERROR);
			throw new ReplicationException("No path found for re-fetch replication.");
		}
		String[] uris;

		if (!pathMatchesFilter(this.pathMatches, path)) {
			logMessage("Path does not match filters provided. IGNORING " + path, LogLevel.INFO);
			return ReplicationContent.VOID;
		}

		if (!NAME.equals(action.getConfig().getSerializationType())) {
			String message = "Serialization type " + action.getConfig().getSerializationType()
					+ " not supported by Flush Re-Fetch Content Builder.";
			logMessage(message, LogLevel.ERROR);
			throw new ReplicationException(message);
		}

		if (action.getType() == ReplicationActionType.TEST) {
			return ReplicationContent.VOID;
		}

		logMessage("Content builder invoked for path " + path + ", with replication action "
						+ action.getType() + " and serialization type " +  action.getConfig().getSerializationType(),
				LogLevel.INFO);

		int pathSep = path.lastIndexOf('/');
		if (pathSep < 0) {
			logMessage("Activation on a non-path value.  IGNORING: " + path, LogLevel.WARN);
			return ReplicationContent.VOID;
		}

		int extSep = path.indexOf('.', pathSep);
		if (extSep == -1) {
			// If no extension, let the path be activated.
			uris = new String[]{path};
		} else {
			try {
/*						Resource res = rr.getResource(path);
				if (res != null) {
					Node node = res.adaptTo(Node.class);
					if (node != null && NT_DAM_ASSET.equals(node.getPrimaryNodeType().getName())) {
						logMessage("Skipped request to replicate " + path + " as it was a dam:Asset.",
						LogLevel.WARN);
						return ReplicationContent.VOID;
					}
				}*/
				ArrayList<String> paths = new ArrayList<>();
				paths.add(path);
				if (extensionPairs != null && extensionPairs.size() != 0) {
					String extension = FilenameUtils.getExtension(path);
					String[] values = extensionPairs.get(extension);
					if (values != null && values.length != 0) {
						String withoutExt = FilenameUtils.removeExtension(path) + ".";
						for (String next: values) {
							paths.add(withoutExt + next);
						}
					}
				}

				uris = paths.toArray(new String[0]);
			} catch (Exception e) {
				logMessage("Replicated cancelled: " + e.getMessage(), LogLevel.ERROR);
				return ReplicationContent.VOID;
			}
		}

		logMessage("Replicating with Re-Fetch: " + Arrays.toString(uris), LogLevel.INFO);
		return createContent(factory, uris);
	}

	/**
	 * Create the replication content, containing one or more URIs to be re-fetched
	 * immediately upon flushing a page.
	 *
	 * @param factory ReplicationContentFactory
	 * @param uris    URIs to re-fetch
	 * @return replication content
	 *
	 * @throws ReplicationException if an error occurs
	 */
	private ReplicationContent createContent(ReplicationContentFactory factory, String[] uris)
			throws ReplicationException {

		File tmpFile;
		BufferedWriter out = null;

		try {
			tmpFile = File.createTempFile("cq5", ".post");
		} catch (IOException e) {
			throw new ReplicationException("Unable to create temp file", e);
		}

		try {
			out = new BufferedWriter(new FileWriter(tmpFile));
			for (String nextUri: uris) {
				out.write(nextUri);
				out.newLine();
				logMessage("TempFile: adding " + nextUri, LogLevel.DEBUG);
			}
			out.close();
			IOUtils.closeQuietly(out);
			out = null;
			return factory.create("text/plain", tmpFile, true);
		} catch (IOException e) {
			if (out != null) {
				IOUtils.closeQuietly(out);
			}
			if (!tmpFile.delete()) {
				logMessage("Could not delete repository content temporary file: " + tmpFile.getName(),
						LogLevel.DEBUG);
			}
			throw new ReplicationException("Unable to create (temporary) repository content", e);
		}
	}

	private void logMessage(String message, LogLevel level) {
		if (level == LogLevel.ERROR) {
			if (replicationLog != null) {
				replicationLog.error(message);
			} else if (logger != null) {
				logger.error(message);
			}
		} else if (level == LogLevel.WARN) {
			if (replicationLog != null) {
				replicationLog.warn(message);
			} else if (logger != null) {
				logger.warn(message);
			}
		} else if (level == LogLevel.DEBUG) {
			if (replicationLog != null) {
				replicationLog.debug(message);
			} else if (logger != null) {
				logger.debug(message);
			}
		} else {
			if (replicationLog != null) {
				replicationLog.info(message);
			} else if (logger != null) {
				logger.info(message);
			}
		}
	}

	private String mapToString(Map<String, String[]> map) {
		StringBuilder output = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			if (!first) {
				output.append(",");
			}
			first = false;

			output.append(entry.getKey());
			output.append("=[");
			boolean firstValue = true;
			String[] values = entry.getValue();
			for (String nextValue: values) {
				if (!firstValue) {
					output.append(",");
				}
				firstValue = false;

				output.append(nextValue);
			}
			output.append("]");
		}

		return output.toString();
	}

	/**
	 * See if any of the provided filter patterns match the current path.  TRUE, if no filters are received.
	 * @param filters An array of string making up the regex to match with the path.  They should never
	 *                be empty since there is a DEFAULT value.
	 * @param path The path to interrogate.
	 * @return true (match found) or false (no match)
	 */
	private boolean pathMatchesFilter(final String[] filters, String path) {
		boolean matches = false;
		for (String filter: filters) {
			try {
				if (filter.equals("*") || path.matches(filter)) {
					matches = true;
					break;
				}
			} catch(PatternSyntaxException ex) {
				logMessage("Ignoring invalid regex filter: [" + filter + "].  Reason: " + ex.getMessage(),
						LogLevel.ERROR);
			}
		}

		return matches;
	}

	final Map<String, String[]> getExtensionPairs() {
		return this.extensionPairs;
	}
	final String[] getPathMatches() {
		return this.pathMatches;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@value #NAME}
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@value #TITLE}
	 */
	public String getTitle() {
		return TITLE;
	}
}
