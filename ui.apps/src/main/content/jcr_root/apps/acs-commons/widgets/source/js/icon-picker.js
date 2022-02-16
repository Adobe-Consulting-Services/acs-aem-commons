/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
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
/*global CQ: false, ACS: false */
/*jslint eval: true
*/
CQ.Ext.ns("ACS.CQ");
ACS.CQ.GraphicIconSelection = CQ.Ext.extend(CQ.form.CompositeField, {

    /**
     * @cfg {Boolean} allowBlank
     * False to validate that an item is selected.<br>
     * Defaults to true.
     */
    allowBlank: true,

    autoEl: { tag: 'div' },

    /**
     * @cfg {Object[]/String} options
     * <p>The options of the selection. An option has the properties "value", "text"
     * and "image".</p>
     * Example for the object array structure:
     * <pre><code>
[
    {
        value: "pink",
        text: "Pink"
    }
]
</code></pre>
     * <p>If options is a string it is assumed to be an URL pointing to a JSON
     * resource that returns the options (same structure as above applies). This
     * should be either an absolute URL or it can use the path of the content
     * resource being edited using a placeholder
     * ({@link #Selection.PATH_PLACEHOLDER} = "$PATH"), for example:
     * <code>$PATH.options.json</code>.</p>
     * <p>Set {@link #optionsRoot} and
     * {@link #optionsTextField}, {@link #optionsValueField} or {@link #optionsImageField}
     * if the properties are named differently and/or if the array with the options is
     * in a sub-property of the JSON returned from the URL.</p>
     */
    options: null,

    /**
     * @cfg {String} optionsRoot
     * <p>Name of the property containing the options array when using a custom JSON URL
     * for {@link #options}. Use "." to denote the root object/array of the result itself.</p>
     * <p>Only if optionsRoot is set, {@link #optionsTextField}, {@link #optionsValueField} and
     * {@link #optionsImageField} will be used. If optionsRoot is <em>not</em> set, the returned
     * JSON must be a standard widgets json, "resulting" in the exact format as described in
     * {@link #options} - it will also be passed through {@link CQ.Util#formatData}. Not set by default.
     */
    optionsRoot: null,

    /**
     * @cfg {String} optionsTextField
     * Name of the field for the options text when using a custom JSON URL for {@link #options}
     * (defaults to "text"). Only used if {@link #optionsRoot} is set.
     */
    optionsTextField: null,

    /**
     * @cfg {String} optionsValueField
     * name of the field for the options value when using a custom JSON URL for {@link #options}
     * (defaults to "value"). Only used if {@link #optionsRoot} is set.
     */
    optionsValueField: null,

    /**
     * @cfg {String} optionsImageField
     * Name of the field for the options image when using a custom JSON URL for {@link #options}
     * (defaults to "image"). Only used if {@link #optionsRoot} is set.
     */
    optionsImageField: null,

    /**
     * @cfg {Function/String} optionsCallback
     * <p>A function or the name of a function that is called on <code>processRecords</code>
     * to populate the options.</p>
     * <p>The function allows to set or request options that depend on the current
     * path or content record.
     * <div class="mdetail-params">
     *      <strong style="font-weight: normal;">The function will be called with the following arguments:</strong>
     *      <ul><li><code>path</code> : String<div class="sub-desc">The content path</div></li></ul>
     *      <ul><li><code>record</code> : CQ.Ext.data.Record<div class="sub-desc">The content record</div></li></ul>
     *  </div>
     * @deprecated Use {@link #optionsProvider} instead
     */
    optionsCallback: null,

    /**
     * @cfg {Function/String} optionsProvider
     * <p>A function or the name of a function that will be called on <code>processRecords</code>
     * to receive the options. The function must return an array of options. See {@link #options}
     * for more information on options.</p>
     * <p>The options provider allows to set or request options that depend on the current
     * path or content record.
     * <div class="mdetail-params">
     *      <strong style="font-weight: normal;">The provider will be called with the following arguments:</strong>
     *      <ul><li><code>path</code> : String<div class="sub-desc">The content path</div></li></ul>
     *      <ul><li><code>record</code> : CQ.Ext.data.Record<div class="sub-desc">The content record</div></li></ul>
     *  </div>
     * @since 5.3
     */
    optionsProvider: null,

    /**
     * @cfg {Object} optionsConfig
     * The config of the options components. For selections of type "checkbox" and
     * "radio" optionsConfig will be passed to every single
     * {@link CQ.Ext.form.CheckBox CheckBox} or {@link CQ.Ext.form.Radio Radio}.
     * For "select" and "combobox" optionsConfig will be passed to the underlaying
     * {@link CQ.Ext.form.ComboBox ComboBox}.<br>
     * Please check the underlaying component's config options to see the possible
     * properties. Be aware that some properties are set internally and cannot
     * be overwritten.
     * @type Object
     */
    optionsConfig: null,

    /**
     * @cfg {String} blankText Error text to display if the {@link #allowBlank} validation fails
     * (for selections of type checkbox only).
     * Defaults to "You must select at least one item in this group".
     * @since 5.4
     */
    blankText : CQ.I18n.getMessage("You must select an item in this group"),

    /**
     * @cfg {String} sortDir
     * The sort direction of the the options. If "ASC" or "DESC" the options will
     * be sorted by its (internationalized) text. Defaults to <code>null</code>.
     * @since 5.4
     */
    sortDir: null,

    /**
     * Hidden field of selections of type "select".
     * @type CQ.Ext.form.Hidden
     * @private
     */
    hiddenField: null,

    /**
     * Returns the normalized data value. "undefined" or the combo box'
     * {@link CQ.Ext.form.ComboBox#emptyText emptyText} return an empty
     * string (""). Type "checkbox" return always an array even if there is
     * only single checkbox available.<br>
     * To return the raw value see {@link #getRawValue}.
     * @return {String/String[]} value The field value
     */
    getValue : function(){
        if(!this.rendered) {
            return this.value !== null ? this.value : "";
        }

        return this.hiddenField.getValue();
    },

    /**
     * Loads the options of the selection if an optionsProvider is available.
     * This method is usually called solely by {@link CQ.Dialog} after its
     * content has been loaded.
     * @param {String} path content path (optional)
     * @private
     */
    processPath: function(path) {
        var options = [],
            p,
            url;
        if (this.optionsProvider) {
            // @since 5.3
            if (path === undefined) {
                path = null;
            }
            try {
                if (typeof this.optionsProvider !== "function") {
                    try {
                        eval("p = " + this.optionsProvider); // jshint ignore:line
                        options = p.call(this, path);
                    }
                    catch (e1) {
                        CQ.Log.warn("Selection#processPath: failed to evaluate optionsProvider: " + e1.message);
                    }
                } else {
                    options = this.optionsProvider.call(this, path);
                }
                this.setOptions(options);
            }
            catch (e2) {
                CQ.Log.warn("Selection#processPath: " + e2.message);
            }
        } else if (this.optionsCallback) {
            // @deprecated
            if (path === undefined) {
                path = null;
            }
            try {
                if (typeof this.optionsCallback !== "function") {
                    try {
                        eval(this.optionsCallback).call(this, path); // jshint ignore:line
                    }
                    catch (e3) {
                        CQ.Log.warn("Selection#processPath: failed to evaluate optionsCallback: " + e3.message);
                    }
                }
                else {
                    this.optionsCallback.call(this, path);
                }
            } catch (e4) {
                CQ.Log.warn("Selection#processPath: failed to call optionsCallback: " + e4.message);
            }
        } else if (this.contentBasedOptionsURL) {
            url = this.contentBasedOptionsURL;
            url = url.replace(ACS.CQ.GraphicIconSelection.PATH_PLACEHOLDER_REGEX, path);
            this.setOptions(this.fetchOptions(url, this));
        }
    },

    /**
     * Sets the options of the selection.
     * @param {Object[]} options The options
     * <pre><code>
[
    {
        value: "pink",
        text: "Pink",
        image: "/etc/project/designs/images/pink-thing.png"
    }
]
</code></pre>
     */
    setOptions: function(options) {
        if (this.rendered) {
            this.doLayout();
        }
    },

    /**
     * Sorts the options by its title
     * @private
     */
    sortOptions: function(o) {
        if (this.sortDir !== "ASC" && this.sortDir !== "DESC") {
            return;
        }
        var x = this.sortDir === "ASC" ? 1 : -1;
        o.sort(function(a, b) {
            var ta = a.text.toLowerCase(),
                tb = b.text.toLowerCase();
            if (ta > tb) {
                return x;
            }
            if (ta < tb) {
                return -x;
            }
            return 0;
        });
    },

    /**
     * Sets a data value into the field and validates it. Multiple values for
     * selections of type "checkbox" are passed as an array. If an array is
     * passed to other types the first value is taken.
     * @param {String/String[]} value The value to set
     */
    setValue: function(value){
        value = value || '';
        var jqThis = jQuery(this.getEl().dom),
            jqList = jqThis.find('.graphic-selection-list'),
            jqCurrent = jqThis.find('.graphic-selection-current'),
            jqItems = jqThis.find('.graphic-selection-item'),
            currentItem = jqThis.find("[data-value='" + value + "']");

        jqItems.removeClass('selected');
        currentItem.addClass('selected');

        this.hiddenField.setValue(value);
    },

    /**
     * Validates a value according to the field's validation rules and marks the field as invalid
     * if the validation fails
     * @param {Mixed} value The value to validate
     * @return {Boolean} True if the value is valid, else false
     */
    validateValue: function(value){
        return this.hiddenField.validateValue(value);
    },

    // TODO - Rework these
    // overriding CQ.form.CompositeField#markInvalid
    markInvalid : function(msg){
        this.fireEvent('invalid', this, msg);
    },

    // overriding CQ.form.CompositeField#clearInvalid
    clearInvalid : function(){
        this.fireEvent('valid', this);
    },

    /**
     * Fetches the options via HTTP.
     * @private
     */
    fetchOptions: function(url, config) {
        var options = [],
            json,
            optVF,
            optTF,
            optTFXSS,
            root,
            opt,
            i;

        try {
            json = CQ.HTTP.eval(url); // jshint ignore:line
            if (config.optionsRoot) {
                // convert ext format to our format
                optVF = config.optionsValueField || "value";
                optTF = config.optionsTextField || "text";
                optTFXSS = CQ.shared.XSS.getXSSPropertyName(optTF);
                root = (config.optionsRoot === ".") ? json : json[config.optionsRoot];

                for (i = 0; i < root.length; i++) {
                    opt = {
                        value: root[i][optVF],
                        text: root[i][optTF],
                        text_xss: root[i][optTFXSS]
                    };
                    options.push(opt);
                }
            } else {
                options = CQ.Util.formatData(json);
            }
        }
        catch (e) {
            CQ.Log.warn("CQ.form.GraphicIconSelection#fetchOptions failed: " + e.message);
            options = [];
        }

        var optionsToReturn = [];

        for (i = 0; i < options.length; i++) {
            //if(options[i].value !== "") {
            optionsToReturn.push(options[i]);
            //}
        }

        return optionsToReturn;
    },

    constructor: function(config) {
        if (config.allowBlank !== undefined) {
            this.allowBlank = config.allowBlank;
        }

        if (config.optionsCallback) {
            this.optionsCallback = config.optionsCallback;
            if (!config.options) config.options = [];
        }

        if (typeof config.options == "string") {
            if (config.options.indexOf(ACS.CQ.GraphicIconSelection.PATH_PLACEHOLDER) >= 0) {
                // if $path as reference to content path is used, we have to delay
                // the option fetching until processPath() is called
                this.contentBasedOptionsURL = config.options;
                config.options = [];
            } else {
                config.options = this.fetchOptions(config.options, config);
            }
        }
        else if (!config.options) {
            config.options = [];
        }

        this.optionsConfig = config.optionsConfig ? config.optionsConfig : {};

        var defaults = {
            height: "auto",
            hideMode: "display"
        };

        CQ.Util.applyDefaults(config, defaults);
        CQ.form.CompositeField.superclass.constructor.call(this, config);

        this.hiddenField = new CQ.Ext.form.Hidden({ name: this.name });
        this.add(this.hiddenField);

        this.options = config.options;

    },

    /**
     * Initializes the component by registering specific events.
     * @private
     */
    initComponent: function() {
        ACS.CQ.GraphicIconSelection.superclass.initComponent.call(this);

        // cq-selection is required to address invalid checkboxes in CSS
        this.addClass(CQ.DOM.encodeClass(this.name) + " graphic-selection icon-selection");

        this.addEvents(
            /**
             * @event selectionchanged
             * Fires when the user changes the selection/interacts with the selection
             * control(s). Note that the event is sent on selection and deselection for
             * checkboxes, but only on selection for radiobuttons and comboboxes. Also note
             * that the event currently does not get fired for the initial selection of
             * a combobox.
             * @param {CQ.form.Selection} this
             * @param {Mixed} value The raw value of the selected, checked or unchecked
             *                      option or the entered value of a combobox
             * @param {Boolean} isChecked <code>false</code> if a checkbox has been unchecked,
             *                            <code>true</code> otherwise
             */
            ACS.CQ.GraphicIconSelection.EVENT_SELECTION_CHANGED
        );
    },

    onRender: function(ct, pos) {
        ACS.CQ.GraphicIconSelection.superclass.onRender.call(this, ct, pos);
        var t = this.tpl || new CQ.Ext.XTemplate(
            '<div class="graphic-selection-list clearfix image-staging">' +
                '<tpl for=".">' +
                    '<a href="#" class="graphic-selection-item" data-value="{value}" title="{text}">' +
                        '<div class="graphic-selection-image">' +
                            '<i class="{value}"></i>' +
                        '</div>' +
                    '</a>' +
                '</tpl>' +
            '</div>'
        );
        t.append(this.el, this.options);

        var extThis = this,
            jqThis = jQuery(this.getEl().dom),
            jqList = jqThis.find('.graphic-selection-list'),
            listItems = jqList.find('.graphic-selection-item'),
            listImageContainers = jqList.find('.graphic-selection-image');


        listItems.click(function(e) {
            e.preventDefault();
            if (!this.disabled) {
                extThis.setValue($(this).data('value'));
            }
        });

        if (this.value !== null) {
            this.setValue(this.value);
        }
    },

    // Overrides the original implementation and disables all composite fields.
    disable: function() {
        ACS.CQ.GraphicIconSelection.superclass.disable.call(this);
        this.disabled = true;
        this.addClass('disabled');
        return this;
    },

    // Overrides the original implementation and enables all composite fields.
    enable: function() {
        ACS.CQ.GraphicIconSelection.superclass.enable.call(this);
        this.disabled = false;
        this.removeClass('disabled');
        return this;
    },

    focus: CQ.Ext.emptyFn,
    blur: CQ.Ext.emptyFn
});

/**
 * Name of the "selectionchanged" event which is fired when the user changes the
 * value of a selection.
 * @static
 * @final
 * @String
 */
ACS.CQ.GraphicIconSelection.EVENT_SELECTION_CHANGED = "selectionchanged";

/**
 * Placeholder to use in "options" config when given as URL. Will be
 * replaced with the path to the resource currently being edited in
 * a CQ.Dialog.
 * @static
 * @final
 * @String
 */
ACS.CQ.GraphicIconSelection.PATH_PLACEHOLDER = "$PATH";

/**
 * Regexp variant of above, including global match/replace. For
 * internal use only.
 * @static
 * @private
 * @final
 * @String
 */
ACS.CQ.GraphicIconSelection.PATH_PLACEHOLDER_REGEX = /\$PATH/g;

CQ.Ext.reg("graphiciconselection", ACS.CQ.GraphicIconSelection);
