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
 *
 * Extends /libs/foundation/components/parsys to limit the components that be added
 * using drag/drop, copy/paste or insert actions
 * To enable limit feature set the property acsComponentsLimit with required limit on design node
 * eg. to limit the components to 4 on rightpar of /content/geometrixx/en.html
 * set acsComponentsLimit=4 on /etc/designs/geometrixx/jcr:content/homepage/rightpar
 */
(function ($, $document) {
    "use strict";

    var ACS_COMPONENTS_LIMIT = "acsComponentsLimit";

    /**
     * mostly taken over from /libs/cq/gui/components/authoring/editors/clientlibs/core/js/storage/components.js _findAllowedComponentsFromPolicy
     * Find the limits of the given Editable based on a policy path
     *
     * @memberof Granite.author.components
     * @alias _findAllowedComponentsFromPolicy
     * @private
     * @ignore
     *
     * @param {Granite.author.Editable} editable    - The editable for which to find allowed components from
     * @param {{}} design                           - Configuration object from where to find allowed components
     * @returns {Array.<Granite.author.Component>}
     */
    function _findPropertyFromPolicy(editable, design, propertyName) {
        var cell = Granite.author.util.resolveProperty(design, editable.config.policyPath);

        if (cell && cell[propertyName]) {
            return cell[propertyName];
        }
        return null;
    }

    /**
     * mostly taken over from /libs/cq/gui/components/authoring/editors/clientlibs/core/js/storage/components.js _findAllowedComponentsFromDesign
     * Returns an array of strings representing the list of allowed components extracted from the given design configuration object
     *
     * <p>Those could be either a path, a resource type or component group</p>
     *
     * @memberof Granite.author.components
     * @alias _findAllowedComponentsFromDesign
     * @private
     * @ignore
     *
     * @param {Granite.author.Editable} editable    - Editable for which to compute a list of allowed components
     * @param {{}} design                           - Design configuration object from which to get the actual configuration
     * @returns {string[]|undefined}                - An array of string in case of a configuration object has been found. Undefined otherwise
     */
     function _findPropertyFromDesign(editable, design, propertyName) {
        if (editable && editable.config) {
            if (editable.config.policyPath) {
                return _findPropertyFromPolicy(editable, design, propertyName);
            } else {
                // All cell search paths
                var cellSearchPaths = editable.config.cellSearchPath;

                if (cellSearchPaths) {
                    for (var i = 0; i < cellSearchPaths.length; i++) {
                        var cell = Granite.author.util.resolveProperty(design, cellSearchPaths[i]);

                        if (cell && cell[propertyName]) {
                            return cell[propertyName];
                        }
                    }
                }
            }
        }
        return null;
    }

    function showErrorAlert(message, title){
        var fui = $(window).adaptTo("foundation-ui"),
            options = [{
                text: "OK",
                warning: true
            }];

        message = message || "Unknown Error";
        title = title || "Error";

        fui.prompt(title, message, "error", options);
    }

    function getChildEditables(parsys){
        var editables = Granite.author.edit.findEditables(),
            children = [], parent;

        _.each(editables, function(editable){
            parent = editable.getParent();

            if(parent && (parent.path === parsys.path)){
                children.push(editable);
            }
        });

        return children;
    }

    function isWithinLimit(parsysEditable){
        var children = getChildEditables(parsysEditable),
            isWithin = true, currentLimit = "";

        currentLimit = _findPropertyFromDesign(parsysEditable, Granite.author.pageDesign, ACS_COMPONENTS_LIMIT);
        if (currentLimit === null) {
            return false;
        }
        var limit = parseInt(currentLimit);
        isWithin = children.length <= limit;

        if(!isWithin){
            showErrorAlert("Limit of paragraphs within this paragraph system exceeded, allowed only up to " + currentLimit + " paragraphs.");
        }

        return isWithin;
    }

    function extendComponentDrop(){
        var dropController = Granite.author.ui.dropController,
            compDragDrop;

        if (dropController !== undefined) {
            compDragDrop = dropController.get(Granite.author.Component.prototype.getTypeName());

            //handle drop action
            if (compDragDrop !== undefined) {
                //handle drop action
                compDragDrop.handleDrop = function(dropFn){
                    return function (event) {
                        if(!isWithinLimit(event.currentDropTarget.targetEditable.getParent())){
                            return;
                        }

                        return dropFn.call(this, event);
                    };
                }(compDragDrop.handleDrop);
            }

            //handle insert action
            Granite.author.edit.actions.openInsertDialog = function(openDlgFn){
                return function (editable) {
                    if(!isWithinLimit(editable.getParent())){
                        return;
                    }

                    return openDlgFn.call(this, editable);
                };
            }(Granite.author.edit.actions.openInsertDialog);

            //handle paste action
            var insertAction = Granite.author.edit.Toolbar.defaultActions.INSERT;

            insertAction.handler = function(insertHandlerFn){
                return function(editableBefore, param, target){
                    if(!isWithinLimit(editableBefore.getParent())){
                        return;
                    }

                    return insertHandlerFn.call(this, editableBefore, param, target);
                };
            }(insertAction.handler);
        }
    }

    $(function() {
        if (Granite && Granite.author && Granite.author.edit && Granite.author.Component &&
                Granite.author.ui && Granite.author.ui.dropController) {
            extendComponentDrop();
        }
    });
}(jQuery, jQuery(document)));
