/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.mcp.form;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provisions for path fields Accepts the following options: base=[path] -- Root
 * of tree shown to user multiple -- If added it indicates the user can make
 * multiple selections and values are stored in a multi-value field
 */
public abstract class PathfieldComponent extends FieldComponent {

    private static final String OPTION_PREDICATE = "predicate";

    @Override
    public void init() {
        setResourceType("granite/ui/components/coral/foundation/form/pathbrowser");
        getProperties().put("pickerMultiselect", hasOption("multiple"));
        getOption("base").ifPresent(path -> getProperties().put("rootPath", path));
        getProperties().put(OPTION_PREDICATE, "nosystem");
    }

    @ProviderType
    public static final class AssetSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getProperties().put(OPTION_PREDICATE, "hierarchy");
        }
    }

    @ProviderType
    public static final class NodeSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getProperties().put(OPTION_PREDICATE, "nosystem");
        }
    }

    @ProviderType
    public static final class PageSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getProperties().put(OPTION_PREDICATE, "hierarchyNotFile");
        }
    }

    @ProviderType
    public static final class FolderSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getProperties().put(OPTION_PREDICATE, "folder");
        }
    }
}
