/*
 * Copyright 2019 Adobe.
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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/**
 * Represent a form
 */
public class FormComponent extends AbstractContainerComponent {

    private static final String ACTION = "action";
    private static final String ASYNC = "async";
    private static final String AUTOCOMPLETE = "autocomplete";
    private static final String AUTOSUBMIT_FORM = "autosubmitForm";
    private static final String DATA_PATH = "dataPath";
    private static final String ENCTYPE = "enctype";
    private static final String FOUNDATION_FORM = "foundationForm";
    private static final String LOADING_MASK = "loadingMask";
    private static final String MARGIN = "margin";
    private static final String MAXIMIZED = "maximized";
    private static final String METHOD = "method";
    private static final String NAME_NOT_FOUND_MODE = "nameNotFoundMode";
    private static final String NOVALIDATE = "novalidate";
    private static final String STYLE = "style";
    private static final String SUCCESSRESPONSE = "successresponse";
    private static final String TARGET = "target";
    private static final String UI = "ui";

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
        super.addComponent("submit", generateFormActions());
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
        getOption(ACTION).ifPresent(this::setAction);
        getBooleanOption(ASYNC).ifPresent(this::setAsync);
        getOption(AUTOCOMPLETE).ifPresent(this::setAutocomplete);
        getBooleanOption(AUTOSUBMIT_FORM).ifPresent(this::setAutosubmitForm);
        getOption(DATA_PATH).ifPresent(this::setDataPath);
        getOption(ENCTYPE).ifPresent(this::setEnctype);
        getBooleanOption(FOUNDATION_FORM).ifPresent(this::setFoundationForm);
        getBooleanOption(LOADING_MASK).ifPresent(this::setLoadingMask);
        getBooleanOption(MARGIN).ifPresent(this::setMargin);
        getBooleanOption(MAXIMIZED).ifPresent(this::setMaximized);
        getOption(METHOD).ifPresent(this::setMethod);
        getOption(NAME_NOT_FOUND_MODE).ifPresent(this::setNameNotFoundMode);
        getBooleanOption(NOVALIDATE).ifPresent(this::setNovalidate);
        getOption(STYLE).ifPresent(this::setStyle);
        getBooleanOption(SUCCESSRESPONSE).ifPresent(this::setSuccessresponse);
        getOption(TARGET).ifPresent(this::setTarget);
        getOption(UI).ifPresent(this::setUi);
    }

    @Override
    public Resource buildComponentResource() {
        initOptions();
        ResourceMetadata meta = getComponentMetadata();
        meta.put(ACTION, getAction());
        meta.put(ASYNC, isAsync());
        meta.put(AUTOCOMPLETE, getAutocomplete());
        meta.put(AUTOSUBMIT_FORM, isAutosubmitForm());
        meta.put(DATA_PATH, getDataPath());
        meta.put(ENCTYPE, getEnctype());
        meta.put(FOUNDATION_FORM, isFoundationForm());
        meta.put(LOADING_MASK, isLoadingMask());
        meta.put(MARGIN, isMargin());
        meta.put(MAXIMIZED, isMaximized());
        meta.put(METHOD, getMethod());
        meta.put(NAME_NOT_FOUND_MODE, getNameNotFoundMode());
        meta.put(NOVALIDATE, isNovalidate());
        meta.put(STYLE, getStyle());
        meta.put(SUCCESSRESPONSE, isSuccessresponse());
        meta.put(TARGET, getTarget());
        meta.put(UI, getUi());
        purgeEmptyMetadata();

        AbstractResourceImpl res = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), meta);
        if (sling != null) {
            res.setResourceResolver(sling.getRequest().getResourceResolver());
        }

        res.addChild(generateItemsResource(getPath() + "/items", true));
        return res;
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
    final public void setMethod(String method) {
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
    final public void setAction(String action) {
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
    final public void setEnctype(String enctype) {
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
    final public void setTarget(String target) {
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
    final public void setAutocomplete(String autocomplete) {
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
    final public void setNovalidate(boolean novalidate) {
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
    final public void setDataPath(String dataPath) {
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
    final public void setNameNotFoundMode(String nameNotFoundMode) {
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
    final public void setAutosubmitForm(boolean autosubmitForm) {
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
    final public void setMargin(boolean margin) {
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
    final public void setMaximized(boolean maximized) {
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
    final public void setFoundationForm(boolean foundationForm) {
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
    final public void setAsync(boolean async) {
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
    final public void setLoadingMask(boolean loadingMask) {
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
    final public void setUi(String ui) {
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
    final public void setSuccessresponse(boolean successresponse) {
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

    private FieldComponent generateFormActions() {
        FieldsetComponent container = new FieldsetComponent();
        container.setCssClass("coral-Form-fieldwrapper coral-Form-fieldwrapper--alignLeft");

        ButtonComponent submit = new ButtonComponent();
        submit.setText("Submit");
        submit.setIcon("save");
        submit.setType("submit");
        submit.setVariant("primary");

        ButtonComponent reset = new ButtonComponent();
        reset.setText("Reset");
        reset.setIcon("undo");
        reset.setType("reset");
        reset.setVariant("secondary");

        container.addComponent("reset", reset);
        container.addComponent("submit", submit);
        return container;
    }
}
