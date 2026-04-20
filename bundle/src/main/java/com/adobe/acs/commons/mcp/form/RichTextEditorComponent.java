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

import com.adobe.acs.commons.mcp.util.SyntheticResourceBuilder;
import org.apache.sling.api.resource.Resource;

/**
 * Provide basic Rich Text editor
 * https://helpx.adobe.com/experience-manager/6-4/sites/administering/using/rich-text-editor.html
 * Note: Right now there are no optiosn to configure the toolbars.
 * You can copy this class and change them as needed.
 */
public class RichTextEditorComponent extends FieldComponent {

    boolean useFixedInlineToolbar = true;
    boolean customStart = false;

    @Override
    public void init() {
        setResourceType("cq/gui/components/authoring/dialog/richtext");
        getProperties().put("useFixedInlineToolbar", useFixedInlineToolbar);
    }

    @Override
    @SuppressWarnings("squid:S1192")
    public Resource buildComponentResource() {
        return new SyntheticResourceBuilder(getName(), getResourceType())
                .withAttributes(getProperties())
                .createChild("rtePlugins")
                .createChild("format")
                .withAttributes("features", "bold,italic,underline")
                .createSibling("justify")
                .withAttributes("features", "*")
                .createSibling("links")
                .withAttributes("features", "modifylink,unlink")
                .createSibling("lists")
                .withAttributes("features", "*")
                .createSibling("misctools")
                .withAttributes("features", "*")
                .createChild("specialCharsConfig")
                .createChild("chars")
                .createChild("default_copyright")
                .withAttributes("entity", "&copy;", "name", "copyright")
                .createSibling("default_euro")
                .withAttributes("entity", "&euro;", "name", "euro")
                .createSibling("default_registered")
                .withAttributes("entity", "&reg;", "name", "registered")
                .createSibling("default_trademark")
                .withAttributes("entity", "&trade;", "name", "trademark")
                .up("rtePlugins")
                .createChild("paraformat")
                .withAttributes("features", "*")
                .createChild("formats")
                .createChild("default_p")
                .withAttributes("description", "Paragraph", "tag", "p")
                .createSibling("default_h1")
                .withAttributes("description", "Heading 1", "tag", "h1")
                .createSibling("default_h2")
                .withAttributes("description", "Heading 1", "tag", "h2")
                .createSibling("default_h3")
                .withAttributes("description", "Heading 1", "tag", "h3")
                .createSibling("default_h4")
                .withAttributes("description", "Heading 1", "tag", "h4")
                .createSibling("default_h5")
                .withAttributes("description", "Heading 1", "tag", "h5")
                .createSibling("default_h6")
                .withAttributes("description", "Heading 1", "tag", "h6")
                .createSibling("default_blockquote")
                .withAttributes("description", "Quote", "tag", "blockquote")
                .createSibling("default_pre")
                .withAttributes("description", "Preformatted", "tag", "pre")
                .up("rtePlugins")
                .createChild("table")
                .withAttributes("features", "*")
                .createChild("hiddenHeaderConfig")
                .withAttributes(
                        "hiddenHeaderClassName", "cq-wcm-foundation-aria-visuallyhidden",
                        "hiddenHeaderEditingCSS", "cq-RichText-hiddenHeader--editing")
                .up("rtePlugins")
                .createChild("tracklinks")
                .withAttributes("features", "*")
                //--------------------
                .up("_top")
                .createChild("uiSettings")
                .createChild("cui")
                .createChild("inline")
                .withAttributes("toolbar", new String[]{
            "format#bold", "format#italic", "format#underline",
            "#justify",
            "#lists",
            "links#modifylink", "links#unlink",
            "#paraformat",
            "table#createoredit"})
                .createChild("popovers")
                .createChild("justify")
                .withAttributes("items", new String[]{"justify#justifyleft", "justify#justifycenter", "justify#justifyright"}, "ref", "justify")
                .createSibling("lists")
                .withAttributes("items", new String[]{"lists#unordered", "lists#ordered", "lists#outdent", "lists#indent"}, "ref", "lists")
                .createSibling("paraformat")
                .withAttributes("items", "paraformat:getFormats:paraformat-pulldown", "ref", "paraformat")
                .up("cui")
                .createChild("dialogFullScreen")
                .withAttributes("toolbar", new String[]{
            "format#bold", "format#italic", "format#underline",
            "justify#justifyleft", "justify#justifycenter", "justify#justifyright",
            "lists#unordered", "lists#ordered", "lists#outdent", "lists#indent",
            "links#modifylink", "links#unlink",
            "table#createoredit",
            "#paraformat",
            "image#imageProps"})
                .createChild("popovers")
                .createChild("paraformat")
                .withAttributes("items", "paraformat:getFormats:paraformat-pulldown", "ref", "paraformat")
                .up("cui")
                .createChild("tableEditOptions")
                .withAttributes("toolbar", new String[]{"table#insertcolumn-before", "table#insertcolumn-after", "table#removecolumn", "-",
            "table#insertrow-before", "table#insertrow-after", "table#removerow", "-",
            "table#mergecells-right", "table#mergecells-down", "table#mergecells", "table#splitcell-horizontal", "table#splitcell-vertical", "-",
            "table#selectrow", "table#selectcolumn", "-",
            "table#ensureparagraph", "-",
            "table#modifytableandcell", "table#removetable", "-",
            "undo#undo", "undo#redo", "-",
            "table#exitTableEditing", "-"})
                .build();
    }
}
