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
(function ($, $document, gAuthor) {
    "use strict";

    var ACS_COMPONENTS_LIMIT = "acsComponentsLimit";

    function getDesignPath(editable){
        var parsys = editable.getParent(),
            designSrc = parsys.config.designDialogSrc,
            result = {}, param;

        designSrc = designSrc.substring(designSrc.indexOf("?") + 1);

        designSrc.split(/&/).forEach( function(it) {
            if (_.isEmpty(it)) {
                return;
            }
            param = it.split("=");
            result[param[0]] = param[1];
        });

        return decodeURIComponent(result.content);
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
        var editables = gAuthor.edit.findEditables(),
            children = [], parent;

        _.each(editables, function(editable){
            parent = editable.getParent();

            if(parent && (parent.path === parsys.path)){
                children.push(editable);
            }
        });

        return children;
    }

    function isWithinLimit(editable){
        var path = getDesignPath(editable),
            children = getChildEditables(editable.getParent()),
            isWithin = true, currentLimit = "";

        $.ajax( { url: path + ".2.json", async: false } ).done(function(data){
            if(_.isEmpty(data) || !data[ACS_COMPONENTS_LIMIT]){
                return;
            }

            currentLimit = data[ACS_COMPONENTS_LIMIT];

            var limit = parseInt(data[ACS_COMPONENTS_LIMIT]);

            isWithin = children.length <= limit;
        });

        if(!isWithin){
            showErrorAlert("Limit exceeded, allowed - " + currentLimit);
        }

        return isWithin;
    }

    function extendComponentDrop(){
        var dropController = gAuthor.ui.dropController,
            compDragDrop;

        if (dropController !== undefined) {
            compDragDrop = dropController.get(gAuthor.Component.prototype.getTypeName());

            //handle drop action
            compDragDrop.handleDrop = function(dropFn){
                return function (event) {
                    if(!isWithinLimit(event.currentDropTarget.targetEditable)){
                        return;
                    }

                    return dropFn.call(this, event);
                };
            }(compDragDrop.handleDrop);

            //handle insert action
            gAuthor.edit.actions.openInsertDialog = function(openDlgFn){
                return function (editable) {
                    if(!isWithinLimit(editable)){
                        return;
                    }

                    return openDlgFn.call(this, editable);
                };
            }(gAuthor.edit.actions.openInsertDialog);

            //handle paste action
            var insertAction = gAuthor.edit.Toolbar.defaultActions.INSERT;

            insertAction.handler = function(insertHandlerFn){
                return function(editableBefore, param, target){
                    if(!isWithinLimit(editableBefore)){
                        return;
                    }

                    return insertHandlerFn.call(this, editableBefore, param, target);
                };
            }(insertAction.handler);
        }
    }

    $(function() {
        if (gAuthor && gAuthor.ui && gAuthor.ui.dropController) {
            extendComponentDrop();
        }
    });
}(jQuery, jQuery(document), (window.Granite.author = window.Granite.author || {})));