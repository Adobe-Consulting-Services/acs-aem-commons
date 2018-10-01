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
package com.adobe.acs.commons.mcp.form;

import aQute.bnd.annotation.ProviderType;

/**
 * Provisions for path fields Accepts the following options: base=[path] -- Root
 * of tree shown to user multiple -- If added it indicates the user can make
 * multiple selections and values are stored in a multi-value field
 */
@ProviderType
public abstract class PathfieldComponent extends FieldComponent {

    private static final String OPTION_PREDICATE = "predicate";

    @Override
    public void init() {
        setResourceType("granite/ui/components/coral/foundation/form/pathbrowser");
        getComponentMetadata().put("pickerMultiselect", hasOption("multiple"));
        getOption("base").ifPresent(path -> getComponentMetadata().put("rootPath", path));
        getComponentMetadata().put(OPTION_PREDICATE, "nosystem");
    }

    public static class AssetSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getComponentMetadata().put(OPTION_PREDICATE, "hierarchy");
        }
    }

    public static class NodeSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getComponentMetadata().put(OPTION_PREDICATE, "nosystem");
        }
    }

    public static class PageSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getComponentMetadata().put(OPTION_PREDICATE, "hierarchyNotFile");
        }
    }

    public static class FolderSelectComponent extends PathfieldComponent {

        @Override
        public void init() {
            super.init();
            getComponentMetadata().put(OPTION_PREDICATE, "folder");
        }
    }
}
