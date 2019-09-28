/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import com.adobe.acs.commons.mcp.util.SyntheticResourceBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

/**
 * Implements the Granite UI Autocomplete component. Docs here:
 * https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/form/autocomplete/index.html
 *
 * If displayProperty and predicates are set (e.g. via options) then the ACS
 * query autocomplete will be used:
 * https://adobe-consulting-services.github.io/acs-aem-commons/features/ui-widgets/query-autocomplete-datasource/index.html
 * Note: You still have to first enable this extension manually to use it.
 */
public class AutocompleteComponent extends FieldComponent {

    private boolean disabled = false;

    private boolean multiple = false;

    private boolean forceSelection = false;

    private String icon;

    private String datasource = "cq/gui/components/common/datasources/tags";

    private String displayProperty = null;

    private String values = "granite/ui/components/coral/foundation/form/autocomplete/tags";

    private String options = "granite/ui/components/coral/foundation/form/autocomplete/list";

    private String optionsQuery = null;

    private String[] predicates = null;

    @Override
    public void init() {
        setResourceType("granite/ui/components/coral/foundation/form/autocomplete");
        setMultiple(hasOption("multiple"));
        setDisabled(hasOption("disabled"));
        setForceSelection(hasOption("forceSelection"));
        getOption("icon").ifPresent(this::setIcon);
        getOption("datasource").ifPresent(this::setDatasource);
        getOption("values").ifPresent(this::setValues);
        getOption("options").ifPresent(this::setOptions);
        getOption("query").ifPresent(this::setOptionsQuery);
        getOption("displayProperty").ifPresent(this::setDisplayProperty);
        setPredicatesFromOptions();

        if (displayProperty != null && predicates != null) {
            addClientLibrary("acs-commons.widgets.search-based-path-browser");
            datasource = "acs-commons/granite/ui/components/form/queryautocomplete/datasource";
        }
    }

    @Override
    public Resource buildComponentResource() {
        SyntheticResourceBuilder builder = new SyntheticResourceBuilder(getName(), getResourceType());
        builder.withAttributes(getComponentMetadata())
                .withAttributes(
                        "multiple", isMultiple(),
                        "disabled", isDisabled(),
                        "forceSelection", isForceSelection(),
                        "icon", getIcon());
        if (StringUtils.isNotBlank(displayProperty) && getPredicates() != null) {
            builder.withAttributes("displayProperty", getDisplayProperty(), "predicates", getPredicates());
        }
        if (StringUtils.isNotBlank(datasource)) {
            builder.createChild("datasource", datasource)
                    .up();
        }
        if (StringUtils.isNotBlank(values)) {
            builder.createChild("values", values)
                    .up();
        }
        // Note: Options is required otherwise the component will throw a null pointer when rendering
        if (StringUtils.isNotBlank(options)) {
            builder.createChild("options", options);
            if (optionsQuery != null) {
                builder.withAttributes("src", optionsQuery);
            }
        }
        return builder.build();
    }

    /**
     * @return the disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @param disabled the disabled to set
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return the multiple
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * @param multiple the multiple to set
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /**
     * @return the forceSelection
     */
    public boolean isForceSelection() {
        return forceSelection;
    }

    /**
     * @param forceSelection the forceSelection to set
     */
    public void setForceSelection(boolean forceSelection) {
        this.forceSelection = forceSelection;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the datasource
     */
    public String getDatasource() {
        return datasource;
    }

    /**
     * @param datasource the datasource to set
     */
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * @return the values
     */
    public String getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(String values) {
        this.values = values;
    }

    /**
     * @return the options
     */
    public String getOptions() {
        return options;
    }

    /**
     * @param options the options to set
     */
    public void setOptions(String options) {
        this.options = options;
    }

    /**
     * @return the optionsQuery
     */
    public String getOptionsQuery() {
        return optionsQuery;
    }

    /**
     * @param optionsQuery the optionsQuery to set
     */
    public void setOptionsQuery(String optionsQuery) {
        this.optionsQuery = optionsQuery;
    }

    /**
     * @return the displayProperty
     */
    public String getDisplayProperty() {
        return displayProperty;
    }

    /**
     * @param displayProperty the displayProperty to set
     */
    public void setDisplayProperty(String displayProperty) {
        this.displayProperty = displayProperty;
    }

    /**
     * @return the predicates
     */
    public String[] getPredicates() {
        return predicates == null ? null : Arrays.copyOf(predicates, predicates.length);
    }

    /**
     * @param predicates the predicate to set
     */
    public void setPredicates(String[] predicates) {
        this.predicates = predicates == null ? null : Arrays.copyOf(predicates, predicates.length);
    }

    private void setPredicatesFromOptions() {
        String[] optionPredicates = getOptionNames().stream()
                .filter(s->s.startsWith("predicate_"))
                .map(s->s.substring("predicate_".length()) + "=" + getOption(s).get())
                .collect(Collectors.toList())
                .toArray(new String[]{});
        setPredicates(optionPredicates);
    }

}
