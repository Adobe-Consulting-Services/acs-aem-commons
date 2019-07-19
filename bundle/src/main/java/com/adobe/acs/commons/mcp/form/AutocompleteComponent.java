package com.adobe.acs.commons.mcp.form;

import com.adobe.acs.commons.mcp.util.SyntheticResourceBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

/**
 * Implements the Granite UI Autocomplete component. Docs here:
 * https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/form/autocomplete/index.html
 */
public class AutocompleteComponent extends FieldComponent {

    private boolean disabled = false;

    private boolean multiple = false;

    private boolean forceSelection = false;

    private String icon;

    private String datasource = "cq/gui/components/common/datasources/tags";

    private String values = "granite/ui/components/coral/foundation/form/autocomplete/tags";

    private String options = "granite/ui/components/coral/foundation/form/autocomplete/list";

    private String optionsQuery = null;

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

}
