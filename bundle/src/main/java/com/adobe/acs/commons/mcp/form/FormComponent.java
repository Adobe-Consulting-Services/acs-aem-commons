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

import java.util.Collection;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represent a form
 */
@ProviderType
public final class FormComponent extends AbstractContainerComponent {

    private static final String ACTION_OPT = "action";
    private static final String ASYNC_OPT = "async";
    private static final String AUTOCOMPLETE_OPT = "autocomplete";
    private static final String AUTOSUBMIT_FORM_OPT = "autosubmitForm";
    private static final String DATA_PATH_OPT = "dataPath";
    private static final String ENCTYPE_OPT = "enctype";
    private static final String FOUNDATION_FORM_OPT = "foundationForm";
    private static final String LOADING_MASK_OPT = "loadingMask";
    private static final String MARGIN_OPT = "margin";
    private static final String MAXIMIZED_OPT = "maximized";
    private static final String METHOD_OPT = "method";
    private static final String NAME_NOT_FOUND_MODE_OPT = "nameNotFoundMode";
    private static final String NOVALIDATE_OPT = "novalidate";
    private static final String STYLE_OPT = "style";
    private static final String SUCCESSRESPONSE_OPT = "successresponse";
    private static final String TARGET_OPT = "target";
    private static final String UI_OPT = "ui";

    private String action = "";
    private boolean async = false;
    private String autocomplete = "";
    private boolean autosubmitForm = false;
    private String dataPath = null;
    private String enctype = "multipart/form-data";
    private boolean foundationForm = true;
    private boolean loadingMask = true;
    private boolean margin = true;
    private boolean maximized = false;
    private String method = "post";
    private String nameNotFoundMode = "ignore-freshness";
    private boolean novalidate = true;
    private String style = "vertical";
    private boolean successresponse = false;
    private String target = "";
    private String ui = "";

    public FormComponent() {
        setResourceType("granite/ui/components/coral/foundation/form");
        setGroupingContainer(new AbstractGroupingContainerComponent.TabsComponent());
    }

    @Override
    public void init() {
        super.init();
    }

    private void initOptions() {
        if (!StringUtils.isEmpty(getPath()) && StringUtils.isEmpty(action)) {
            setAction(getPath());
        }
        if (!StringUtils.isEmpty(getPath()) && StringUtils.isEmpty(dataPath)) {
            setDataPath(getPath());
        }
        getOption(ACTION_OPT).ifPresent(this::setAction);
        getBooleanOption(ASYNC_OPT).ifPresent(this::setAsync);
        getOption(AUTOCOMPLETE_OPT).ifPresent(this::setAutocomplete);
        getBooleanOption(AUTOSUBMIT_FORM_OPT).ifPresent(this::setAutosubmitForm);
        getOption(DATA_PATH_OPT).ifPresent(this::setDataPath);
        getOption(ENCTYPE_OPT).ifPresent(this::setEnctype);
        getBooleanOption(FOUNDATION_FORM_OPT).ifPresent(this::setFoundationForm);
        getBooleanOption(LOADING_MASK_OPT).ifPresent(this::setLoadingMask);
        getBooleanOption(MARGIN_OPT).ifPresent(this::setMargin);
        getBooleanOption(MAXIMIZED_OPT).ifPresent(this::setMaximized);
        getOption(METHOD_OPT).ifPresent(this::setMethod);
        getOption(NAME_NOT_FOUND_MODE_OPT).ifPresent(this::setNameNotFoundMode);
        getBooleanOption(NOVALIDATE_OPT).ifPresent(this::setNovalidate);
        getOption(STYLE_OPT).ifPresent(this::setStyle);
        getBooleanOption(SUCCESSRESPONSE_OPT).ifPresent(this::setSuccessresponse);
        getOption(TARGET_OPT).ifPresent(this::setTarget);
        getOption(UI_OPT).ifPresent(this::setUi);
    }

    @Override
    public Resource buildComponentResource() {
        initOptions();
        ResourceMetadata meta = getComponentMetadata();
        meta.put(ACTION_OPT, getAction());
        meta.put(ASYNC_OPT, isAsync());
        meta.put(AUTOCOMPLETE_OPT, getAutocomplete());
        meta.put(AUTOSUBMIT_FORM_OPT, isAutosubmitForm());
        meta.put(DATA_PATH_OPT, getDataPath());
        meta.put(ENCTYPE_OPT, getEnctype());
        meta.put(FOUNDATION_FORM_OPT, isFoundationForm());
        meta.put(LOADING_MASK_OPT, isLoadingMask());
        meta.put(MARGIN_OPT, isMargin());
        meta.put(MAXIMIZED_OPT, isMaximized());
        meta.put(METHOD_OPT, getMethod());
        meta.put(NAME_NOT_FOUND_MODE_OPT, getNameNotFoundMode());
        meta.put(NOVALIDATE_OPT, isNovalidate());
        meta.put(STYLE_OPT, getStyle());
        meta.put(SUCCESSRESPONSE_OPT, isSuccessresponse());
        meta.put(TARGET_OPT, getTarget());
        meta.put(UI_OPT, getUi());
        purgeEmptyMetadata();

        AbstractResourceImpl res = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), meta);
        if (getHelper() != null) {
            res.setResourceResolver(getHelper().getRequest().getResourceResolver());
        }

        AbstractResourceImpl items = generateItemsResource(getPath() + "/items", true);
        if (isForceDotSlashPrefix()) {
            correctNameAttribute(items);
        }
        res.addChild(items);

        return res;
    }

    /**
     * In order to keep the sling post handler happy, form field names have to start with "./" otherwise the values don't go to the right places.
     * @param res
     */
    private void correctNameAttribute(Resource res) {
        String name = res.getValueMap().get("name", String.class);
        // If we replace all name attibutes it causes issues with stuff like the RTE configration
        boolean hasResourceType = StringUtils.isNotBlank(res.getResourceType());
        if (name != null && !name.startsWith("./") && hasResourceType) {
            res.getValueMap().put("name", "./" + name);
        }
        res.getChildren().forEach(this::correctNameAttribute);
    }

    @Override
    public boolean hasCategories(Collection<FieldComponent> values) {
        return getDialogStyle() == DialogProvider.DialogStyle.COMPONENT || super.hasCategories(values);
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public final void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public final void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the enctype
     */
    public String getEnctype() {
        return enctype;
    }

    /**
     * @param enctype the enctype to set
     */
    public final void setEnctype(String enctype) {
        this.enctype = enctype;
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public final void setTarget(String target) {
        this.target = target;
    }

    /**
     * @return the autocomplete
     */
    public String getAutocomplete() {
        return autocomplete;
    }

    /**
     * @param autocomplete the autocomplete to set
     */
    public final void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

    /**
     * @return the novalidate
     */
    public boolean isNovalidate() {
        return novalidate;
    }

    /**
     * @param novalidate the novalidate to set
     */
    public final void setNovalidate(boolean novalidate) {
        this.novalidate = novalidate;
    }

    /**
     * @return the dataPath
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     * @param dataPath the dataPath to set
     */
    public final void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    /**
     * @return the nameNotFoundMode
     */
    public String getNameNotFoundMode() {
        return nameNotFoundMode;
    }

    /**
     * @param nameNotFoundMode the nameNotFoundMode to set
     */
    public final void setNameNotFoundMode(String nameNotFoundMode) {
        this.nameNotFoundMode = nameNotFoundMode;
    }

    /**
     * @return the autosubmitForm
     */
    public boolean isAutosubmitForm() {
        return autosubmitForm;
    }

    /**
     * @param autosubmitForm the autosubmitForm to set
     */
    public final void setAutosubmitForm(boolean autosubmitForm) {
        this.autosubmitForm = autosubmitForm;
    }

    /**
     * @return the margin
     */
    public boolean isMargin() {
        return margin;
    }

    /**
     * @param margin the margin to set
     */
    public final void setMargin(boolean margin) {
        this.margin = margin;
    }

    /**
     * @return the maximized
     */
    public boolean isMaximized() {
        return maximized;
    }

    /**
     * @param maximized the maximized to set
     */
    public final void setMaximized(boolean maximized) {
        this.maximized = maximized;
    }

    /**
     * @return the foundationForm
     */
    public boolean isFoundationForm() {
        return foundationForm;
    }

    /**
     * @param foundationForm the foundationForm to set
     */
    public final void setFoundationForm(boolean foundationForm) {
        this.foundationForm = foundationForm;
    }

    /**
     * @return the async
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * @param async the async to set
     */
    public final void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * @return the loadingMask
     */
    public boolean isLoadingMask() {
        return loadingMask;
    }

    /**
     * @param loadingMask the loadingMask to set
     */
    public final void setLoadingMask(boolean loadingMask) {
        this.loadingMask = loadingMask;
    }

    /**
     * @return the ui
     */
    public String getUi() {
        return ui;
    }

    /**
     * @param ui the ui to set
     */
    public final void setUi(String ui) {
        this.ui = ui;
    }

    /**
     * @return the successresponse
     */
    public boolean isSuccessresponse() {
        return successresponse;
    }

    /**
     * @param successresponse the successresponse to set
     */
    public final void setSuccessresponse(boolean successresponse) {
        this.successresponse = successresponse;
    }

    /**
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }
}
