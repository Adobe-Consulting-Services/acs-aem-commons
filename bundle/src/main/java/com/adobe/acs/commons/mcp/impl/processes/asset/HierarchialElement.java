/*
 * Copyright 2018 Adobe.
 *
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
 */
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.functions.CheckedFunction;
import com.day.cq.commons.jcr.JcrUtil;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an element in the asset tree, which is either an asset/file or a folder
 */
public interface HierarchialElement {

    public boolean isFile();

    default public boolean isFolder() {
        return !isFile();
    }

    public HierarchialElement getParent();
    
    public Stream<HierarchialElement> getChildren();

    public String getName();

    public String getItemName();

    public Source getSource();

    public String getJcrBasePath();
    
    public default String getNodePath() {
        HierarchialElement parent = getParent();
        return (parent == null ? getJcrBasePath() : parent.getNodePath()) + "/" + getNodeName();
    }

    public default String getNodeName() {
        String name = getName();
        if (isFile() && name.contains(".")) {
            return name;
        } else if (JcrUtil.isValidName(name)) {
            return name;
        } else {
            return JcrUtil.createValidName(name, JcrUtil.HYPHEN_LABEL_CHAR_MAPPING, "-");
        }
    }
    
    public default Stream<HierarchialElement> getFileChildren() {
        return getChildren().filter(HierarchialElement::isFile);
    }
    
    public default Stream<HierarchialElement> getFolderChildren() {
        return getChildren().filter(HierarchialElement::isFolder);
    }

    public default void visitAllFolders(CheckedConsumer<HierarchialElement> visitor, 
            CheckedFunction<HierarchialElement, Stream<HierarchialElement>> childFunction) throws Exception {
        LinkedList<HierarchialElement> nodes = new LinkedList<>();
        nodes.add(this);
        while (!nodes.isEmpty()) {
            HierarchialElement node = nodes.pop();
            childFunction.apply(node).forEach(nodes::add);
            visitor.accept(node);
        }
    }
    
    public default void visitAllFiles(CheckedConsumer<HierarchialElement> visitor,
            CheckedFunction<HierarchialElement, Stream<HierarchialElement>> childFolderFunction,
            CheckedFunction<HierarchialElement, Stream<HierarchialElement>> childFileFunction) throws Exception {
        LinkedList<HierarchialElement> nodes = new LinkedList<>();
        nodes.add(this);
        while (!nodes.isEmpty()) {
            HierarchialElement node = nodes.pop();
            childFolderFunction.apply(node).forEach(nodes::add);
            for (HierarchialElement child : childFileFunction.apply(node).collect(Collectors.toList())) {
                visitor.accept(child);
            }
        }
    }    
    
    public default void visitAllFolders(CheckedConsumer<HierarchialElement> visitor) throws Exception {
        visitAllFolders(visitor, HierarchialElement::getFolderChildren);
    }
    
    public default void visitAllFiles(CheckedConsumer<HierarchialElement> visitor) throws Exception {
        visitAllFiles(visitor, HierarchialElement::getFolderChildren, HierarchialElement::getFileChildren);
    }
}
