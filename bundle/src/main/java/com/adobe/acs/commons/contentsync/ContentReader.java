/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import org.apache.jackrabbit.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

public class ContentReader {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final DateTimeFormatter ECMA_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z");

    private static final Pattern ECMA_REGEX = Pattern.compile(
            "^(Mon|Tue|Wed|Thu|Fri|Sat|Sun) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) (\\d{2}) \\d{4} ([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d) GMT[+-]\\d{4}");

    static final String BINARY_DATA_PLACEHOLDER = "0";

    private final NodeTypeManager nodeTypeManager;
    private final Collection<String> knownPropertyPrefixes;

    public ContentReader(Session session) throws RepositoryException {
        Workspace workspace = session.getWorkspace();

        knownPropertyPrefixes = new HashSet<>(Arrays.asList(workspace.getNamespaceRegistry().getPrefixes()));
        nodeTypeManager = workspace.getNodeTypeManager();
    }

    /**
     * Recursive sanitize the give JCR node and remove protected properties
     *
     * @param node json node representing a JCR node
     * @return  sanitized json
     * @see #getProtectedProperties(JsonObject)
     */
    public JsonObject sanitize(JsonObject node) throws RepositoryException {
        JsonObjectBuilder out = Json.createObjectBuilder();
        sanitize(node, out);
        return out.build();
    }

    private void sanitize(JsonObject node, JsonObjectBuilder out) throws RepositoryException {
        Collection<String> sanitizedProperties = getProtectedProperties(node);

        for (Map.Entry<String, JsonValue> field : node.entrySet()) {
            String name = field.getKey();
            int colonIdx = name.indexOf(':');
            if (colonIdx > 0) {
                // sanitize unknown namespaces. These can come, for example, from asset metadata
                String prefix = name.substring(0, colonIdx);
                if (!knownPropertyPrefixes.contains(prefix)) {
                    log.trace("skipping protected property: {}", name);
                    continue;
                }
            }
            // sanitize protected properties
            if (sanitizedProperties.contains(name)) {
                log.trace("skipping unknown namespace: {}", name);
                continue;
            }

            JsonValue value = field.getValue();
            switch (value.getValueType()) {
                case OBJECT:
                    JsonObjectBuilder obj = Json.createObjectBuilder();
                    sanitize((JsonObject) value, obj);
                    out.add(name, obj);
                    break;
                case ARRAY:
                    JsonArray array = (JsonArray) value;
                    out.add(name, array);
                    break;
                case STRING:
                    String str = ((JsonString)value).getString();
                    if(ECMA_REGEX.matcher(str).matches()){
                        // convert to legacy ECMA dates to ISO8601
                        String isoDate = toISO8601(str);
                        value = Json.createValue(isoDate);
                    }
                default:
                    if (colonIdx == 0) {
                        // Leading colon in Sling GET Servlet JSON designates binary data, e.g. :jcr:data
                        // Put the real property instead (without a leading colon) and set a  dummy value
                        out.add(name.substring(1), BINARY_DATA_PLACEHOLDER);
                    } else {
                        out.add(name, value);
                    }
                    break;
            }
        }

    }

    /**
     * Collect protected properties of a given JCR node (non-recursively).
     * The list of protected properties consists of:
     * - properties protected by node's primary type
     * - properties protected by node's mixins
     * <p>
     * For example, if a cq:Page node does not have any mixins applied this method would return
     * <pre>
     *      ["jcr:created", "jcr:createdBy"]
     *  </pre>
     * <p>
     * If  cq:Page is versionable, i.e. has the "mix:versionable" mixin type, then this method would return
     * properties protected by the primary type (cq:Page ) and the mixin (mix:versionable) and the list would be
     * <pre>
     *      ["jcr:created", "jcr:createdBy", "jcr:versionHistory", "jcr:baseVersion", "jcr:predecessors",
     *      "jcr:mergeFailed", "jcr:activity", "jcr:configuration", "jcr:isCheckedOut", "jcr:uuid" ]
     *  </pre>
     *
     * @param node json representing a JCR node
     * @return the list of protected properties
     */
    public List<String> getProtectedProperties(JsonObject node) throws RepositoryException {
        Collection<String> ignored = new HashSet<>(Arrays.asList(JCR_PRIMARYTYPE, JCR_MIXINTYPES));

        List<String> props = new ArrayList<>();
        props.add("rep:policy"); // ACLs are not importable

        List<String> checkTypes = new ArrayList<>();
        String primaryType = node.getString(JCR_PRIMARYTYPE);
        checkTypes.add(primaryType);
        JsonArray mixins = node.getJsonArray(JCR_MIXINTYPES);
        if (mixins != null) {
            for (JsonValue item : mixins) {
                checkTypes.add(((JsonString) item).getString());
            }
        }
        for (String typeName : checkTypes) {
            NodeType nodeType = nodeTypeManager.getNodeType(typeName);
            for (PropertyDefinition definition : nodeType.getPropertyDefinitions()) {
                if (definition.isProtected() && !ignored.contains(definition.getName())) {
                    props.add(definition.getName());
                }
            }
        }

        return props;
    }

    private void collectBinaryProperties(JsonObject node, String parent, List<String> binaryProperties) {
        for (Map.Entry<String, JsonValue> field : node.entrySet()) {
            String name = field.getKey();
            JsonValue value = field.getValue();
            switch (value.getValueType()) {
                case OBJECT:
                    collectBinaryProperties((JsonObject) value, parent + "/" + name, binaryProperties);
                    break;
                case NUMBER:
                    // leading colon in Sling GET Servlet JSON and a numeric value designate binary data
                    if (name.startsWith(":")) {
                        String propPath = parent + "/" + name.substring(1);
                        binaryProperties.add(propPath);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Recursively collect binary properties from a given json node.
     * <p>
     * For example, if <code>node</code> represents a cq:Page object with inline images,
     * the output would look like
     *
     * <pre>
     *  [
     *    /jcr:content/image/file/jcr:content/jcr:data,
     *    /jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_480.png/jcr:content/jcr:data,
     *    /jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_60.png/jcr:content/jcr:data,
     *    /jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_300.png/jcr:content/jcr:data,
     *    /jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_48.png/jcr:content/jcr:data
     *  ]
     * </pre>
     *
     * @param node  json representing a JCR node
     * @return list of property paths relative to the json root
     */
    public List<String> collectBinaryProperties(JsonObject node) {
        List<String> binaryProperties = new ArrayList<>();
        collectBinaryProperties(node, "", binaryProperties);
        return binaryProperties;
    }

    public static boolean isECMADate(String str) {
        return ECMA_REGEX.matcher(str).matches();
    }

    public static Calendar parseEcmaDate(String ecmaDate){
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(ecmaDate, ECMA_DATE_FORMAT);
            return GregorianCalendar.from(zonedDateTime);
        } catch(Exception e){
            return null;
        }
    }

    public static String toISO8601(String ecmaDate){
        Calendar calendar = parseEcmaDate(ecmaDate);
        return calendar == null ? ecmaDate : ISO8601.format(calendar);
    }
}
