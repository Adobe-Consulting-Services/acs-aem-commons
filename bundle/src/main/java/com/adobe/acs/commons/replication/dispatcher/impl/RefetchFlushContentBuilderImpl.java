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
package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

/**
 * Custom dispatcher flush content builder that sends a list of URIs to be re-fetched immediately upon flushing a page.
 */
@Designate(ocd = RefetchFlushContentBuilderImpl.Config.class)
@Component(
        service = { ContentBuilder.class },
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {
            Constants.SERVICE_DESCRIPTION + "=ACS Commons Re-fetch Flush Content Builder",
            "webconsole.configurationFactory.nameHint=Extension Mapping: [{extension.pairs}] Match: [{match.paths}]",
            "name=" + RefetchFlushContentBuilderImpl.SERVICE_NAME
        },
        immediate = true
)
public class RefetchFlushContentBuilderImpl implements ContentBuilder {
    private static final Logger log = LoggerFactory.getLogger(RefetchFlushContentBuilderImpl.class);

    private ReplicationLog replicationLog;

    private static final String CONTENT_BUILDER_NAME = "flush_refetch";
    public static final String TITLE = "Dispatcher Flush Re-fetch";
    public static final String SERVICE_NAME = CONTENT_BUILDER_NAME;
    private static final String[] DEFAULT_MATCH_PATH = {"*"};
    private String[] pathMatches = DEFAULT_MATCH_PATH;
    private Map<String, String[]> extensionPairs = new LinkedHashMap<>();

    @ObjectClassDefinition(
        name = "ACS AEM Commons - Dispatcher Flush with Re-fetch",
        description = "Instead of deleting pages from the Dispatcher cache, update the last modified time (.stat) of the targeted file, and trigger an immediate request of the page."
    )
    @interface Config {
        @AttributeDefinition(
                name = "Path Pattern",
                description = "Specify a regex to match paths to be included in the re-fetch flush (i.e. * for all paths, /content/.* for all paths under /content, .*.html for all paths with html as its extension)",
                cardinality = Integer.MAX_VALUE
        )
        String[] match_paths() default {"*"}; // DEFAULT_MATCH_PATH

        @AttributeDefinition(
            name = "Extension Pairs",
            description = "To activate paired pages with re-fetch, specify the original extension (i.e. html) and map it to any other extensions (i.e. header_include.html)",
            cardinality = Integer.MAX_VALUE
        )
        String[] extension_pairs() default {}; // DEFAULT_EXTENSION_PAIRS
    }

    @Activate
    protected void activate(Config config) {
        this.extensionPairs = this.formatExtensions(ParameterUtil.toMap(config.extension_pairs(), "=", false, null, false));

        logInfoMessage("Extension Pairs [" +  mapToString(this.extensionPairs) + "]");

        ArrayList<String> validMatches = new ArrayList<>();

        String[] matchProps = config.match_paths();

        if (matchProps.length > 0) {
            for (String match : matchProps) {
                if (StringUtils.isNotEmpty(match)) {
                    validMatches.add(match);
                }
            }
        }

        this.pathMatches = validMatches.toArray(new String[0]);

        logInfoMessage("Match Path Patterns [" +  String.join(",", this.pathMatches) + "]");
    }

    /**
     * Take the mapped extensions and organize them by individual extension.
     * @param configuredExtensions Map of extension mappings
     * @return Map with extension as keys
     */
    private Map<String, String[]> formatExtensions(final Map<String, String> configuredExtensions) {
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

    /**
     * {@inheritDoc}
     */
    public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory factory)
            throws ReplicationException {
        String path = action.getPath();
        replicationLog = action.getLog();
        if (replicationLog == null) {
            logWarnMessage("No replication log found on agent " + CONTENT_BUILDER_NAME);
        }

        /* Check if the action is valid, and whether it should be ignored. */
        checkValidity(action, path);
        if (shouldIgnore(action, path)) {
            return ReplicationContent.VOID;
        }

        logInfoMessage("Content builder invoked for path " + path + ", with replication action "
                + action.getType() + " and serialization type " +  action.getConfig().getSerializationType());

        String[] uris = new String[]{path};
        int extSep = path.indexOf('.', path.lastIndexOf('/'));
        if (extSep > 0 && MapUtils.isNotEmpty(extensionPairs)) {
            try {
                ArrayList<String> paths = new ArrayList<>();
                paths.add(path);
                String[] values = getExtensionPairs(path);
                if (ArrayUtils.isNotEmpty(values)) {
                    String withoutExt = FilenameUtils.removeExtension(path) + ".";
                    for (String next: values) {
                        paths.add(withoutExt + next);
                    }
                }

                uris = paths.toArray(new String[0]);
            } catch (Exception e) {
                logErrorMessage("Replicated cancelled: " + e.getMessage());
                return ReplicationContent.VOID;
            }
        }

        logInfoMessage("Replicating with Re-Fetch: " + Arrays.toString(uris));
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
        Path tmpFile = null;
        BufferedWriter out = null;

        try {
            tmpFile = Files.createTempFile("cq5", ".post");
            out = Files.newBufferedWriter(tmpFile);
            for (String nextUri: uris) {
                out.write(nextUri);
                out.newLine();
                logDebugMessage("TempFile: adding " + nextUri);
            }
            out.close();
            return factory.create("text/plain", tmpFile.toFile(), true);
        } catch (IOException e) {
            try {
                if (tmpFile != null) {
                    Files.delete(tmpFile);
                }
            } catch(Exception exception) {
                logInfoMessage("Could not delete repository content temporary file: " + tmpFile.toString());
            }
            throw new ReplicationException("Error with temporary repository content", e);
        } finally {
            if (out != null) {
                IOUtils.closeQuietly(out);
            }
        }
    }

    /**
     * Log methods to use Replication Log if available.  If not, use this class's logger.  Do not duplicate
     * log entries. (ReplicationLog does not extend Logger.)
     * @param message A simple string to use as the log entry.
     */
    private void logErrorMessage(String message){
        if (replicationLog != null) {
            replicationLog.error(message);
        } else if (log != null) {
            log.error(message);
        }
    }

    private void logWarnMessage(String message) {
        if (replicationLog != null) {
            replicationLog.warn(message);
        } else if (log != null) {
            log.warn(message);
        }
    }

    private void logInfoMessage(String message) {
        if (replicationLog != null) {
            replicationLog.info(message);
        } else if (log != null) {
            log.info(message);
        }
    }

    private void logDebugMessage(String message) {
        if (replicationLog != null) {
            replicationLog.debug(message);
        } else if (log != null) {
            log.debug(message);
        }
    }

    /**
     * Check the validity of the parameters received for this activation.
     *
     * @param action The replication action specifying properties of the activation.
     * @param path The path to the item to be activated.
     * @throws ReplicationException Throws a replication exception if invalid.
     */
    private void checkValidity(ReplicationAction action, String path) throws ReplicationException {
        if (action.getType() != ReplicationActionType.ACTIVATE && action.getType() != ReplicationActionType.TEST) {
            logErrorMessage("No re-fetch handling for replication action " + action.getType().getName());
            throw new ReplicationException("No re-fetch handling for replication action "
                    + action.getType().getName());
        }

        if (StringUtils.isEmpty(path)) {
            logErrorMessage("No path found for re-fetch replication.");
            throw new ReplicationException("No path found for re-fetch replication.");
        }

        if (!CONTENT_BUILDER_NAME.equals(action.getConfig().getSerializationType())) {
            String message = "Serialization type '" + action.getConfig().getSerializationType()
                    + "' not supported by Flush Re-Fetch Content Builder.";
            logErrorMessage(message);
            throw new ReplicationException(message);
        }
    }

    private boolean shouldIgnore(ReplicationAction action, String path) {
        if (!pathMatchesFilter(this.pathMatches, path)) {
            logInfoMessage("Path does not match filters provided. IGNORING " + path);
            return true;
        }

        if (action.getType() == ReplicationActionType.TEST) {
            return true;
        }

        int pathSep = path.lastIndexOf('/');
        if (pathSep < 0) {
            logWarnMessage("Activation on a non-path value.  IGNORING: " + path);
            return true;
        }

        return false;
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
     *
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
                logErrorMessage("Ignoring invalid regex filter: [" + filter + "].  Reason: " + ex.getMessage());
            }
        }

        return matches;
    }

    /**
     * Get extension values for the provided path.
     *
     * @param path The path to extract the extension from.  The extension will be the key.
     * @return The extension pairs, if any.
     */
    private String[] getExtensionPairs(String path) {
        String extension = FilenameUtils.getExtension(path);
        return extensionPairs.get(extension);
    }

    final Map<String, String[]> getExtensionPairs() {
        return this.extensionPairs;
    }

    final String[] getPathMatches() {
        return Optional.ofNullable(this.pathMatches)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(DEFAULT_MATCH_PATH);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #CONTENT_BUILDER_NAME}
     */
    public String getName() {
        return CONTENT_BUILDER_NAME;
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