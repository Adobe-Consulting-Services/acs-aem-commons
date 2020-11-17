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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.functions.CheckedFunction;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an element in the asset tree, which is either an asset/file or a folder
 */
public interface HierarchicalElement {

    default boolean excludeBaseFolder() {
        return false;
    }

    boolean isFile();

    default boolean isFolder() {
        return !isFile();
    }

    HierarchicalElement getParent();

    Stream<HierarchicalElement> getChildren();

    String getName();

    String getItemName();

    String getSourcePath();

    Source getSource();

    String getJcrBasePath();

    default String getNodePath(boolean preserveName) {
        HierarchicalElement parent = getParent();
        if (excludeBaseFolder()) {
            return parent == null ? getJcrBasePath() : parent.getNodePath(preserveName) + "/" + getNodeName(preserveName);
        } else {
            return (parent == null ? getJcrBasePath() : parent.getNodePath(preserveName)) + "/" + getNodeName(preserveName);
        }
    }

    default String getNodeName(boolean preserveName) {
        String name = getName();
        if (name == null) {
            return null;
        }
        if (preserveName) {
            return getNodeName(name);
        }
        if (isFile() && name.contains(".")) {
            String baseName = org.apache.commons.lang3.StringUtils.substringBeforeLast(name, ".");
            String extension = org.apache.commons.lang3.StringUtils.substringAfterLast(name, ".");
            return NameUtil.createValidDamName(baseName) + "." + NameUtil.createValidDamName(extension);
        }
        return name.matches(NameUtil.VALID_NAME_REGEXP) ? name : NameUtil.createValidDamName(name);
    }

    default String getNodeName(String name) {
        if (isFile() && name.contains(".")) {
            return name;
        } else if (JcrUtil.isValidName(name)) {
            return name;
        } else {
            return JcrUtil.createValidName(name, JcrUtil.HYPHEN_LABEL_CHAR_MAPPING, "-");
        }
    }

    default Stream<HierarchicalElement> getFileChildren() {
        return getChildren().filter(HierarchicalElement::isFile);
    }

    default Stream<HierarchicalElement> getFolderChildren() {
        return getChildren().filter(HierarchicalElement::isFolder);
    }

    @SuppressWarnings("squid:S00112")
    default void visitAllFolders(CheckedConsumer<HierarchicalElement> visitor,
                                 CheckedFunction<HierarchicalElement, Stream<HierarchicalElement>> childFunction) throws Exception {
        LinkedList<HierarchicalElement> nodes = new LinkedList<>();
        nodes.add(this);
        while (!nodes.isEmpty()) {
            HierarchicalElement node = nodes.pop();
            childFunction.apply(node).forEach(nodes::add);
            visitor.accept(node);
        }
    }

    @SuppressWarnings("squid:S00112")
    default void visitAllFolders(CheckedConsumer<HierarchicalElement> visitor) throws Exception {
        visitAllFolders(visitor, HierarchicalElement::getFolderChildren);
    }

    @SuppressWarnings("squid:S00112")
    default void visitAllFiles(CheckedConsumer<HierarchicalElement> visitor,
                               CheckedFunction<HierarchicalElement, Stream<HierarchicalElement>> childFolderFunction,
                               CheckedFunction<HierarchicalElement, Stream<HierarchicalElement>> childFileFunction) throws Exception {
        LinkedList<HierarchicalElement> nodes = new LinkedList<>();
        nodes.add(this);
        while (!nodes.isEmpty()) {
            HierarchicalElement node = nodes.pop();
            childFolderFunction.apply(node).forEach(nodes::add);
            for (HierarchicalElement child : childFileFunction.apply(node).collect(Collectors.toList())) {
                visitor.accept(child);
            }
        }
    }

    @SuppressWarnings("squid:S00112")
    default void visitAllFiles(CheckedConsumer<HierarchicalElement> visitor) throws Exception {
        visitAllFiles(visitor, HierarchicalElement::getFolderChildren, HierarchicalElement::getFileChildren);
    }


    class UriHelper {
        private static final String SFTP_URL_ENCODING = "utf-8";
        private static final int FIRST_SUB_PATH_INDEX = 3;

        private UriHelper() {
        }

        static String encodeUriParts(final String uri) throws UnsupportedEncodingException {
            String[] uriParts = uri.split("/");

            //path parts started from index 3
            for (int i = FIRST_SUB_PATH_INDEX; i < uriParts.length; i++) {
                uriParts[i] = URLEncoder.encode(uriParts[i], SFTP_URL_ENCODING);
            }

            return StringUtils.join(uriParts, "/");
        }

        static String decodeUriParts(final String uri) throws UnsupportedEncodingException {
            return URLDecoder.decode(uri, SFTP_URL_ENCODING);
        }
    }
}
