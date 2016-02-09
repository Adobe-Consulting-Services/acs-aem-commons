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
 * Extends /libs/foundation/components/parsys to configure drop zone placeholder text and colors
 * To enable this feature set the following properties on desing node - eg. /etc/designs/geometrixx/jcr:content/contentpage/par
 * acsParsysPlaceholderText - to replace default placeholder "Drop Components Here" with custom text eg. "Please drop images only!!!"
 * acsParsysTextColor - placeholder text color eg. #0000FF
 * acsParsysBackgroundColor - drop zone background color - eg.#9DB68C
 * acsParsysBorderColor - drop zone border color - eg. #E5E500
 */
(function(){
    var pathName = window.location.pathname,
        ACS_PARSYS_PLACEHOLDER_TEXT = "acsParsysPlaceholderText",
        ACS_PARSYS_TEXT_COLOR = "acsParsysTextColor",
        ACS_PARSYS_BG_COLOR = "acsParsysBackgroundColor",
        ACS_PARSYS_BORDER_COLOR = "acsParsysBorderColor";

    if( ( pathName !== "/cf" ) && ( pathName.indexOf("/content") !== 0)){
        return;
    }

    function isParsysNew(editable){
        if(!_.isObject(editable.params) || _.isEmpty(editable.params["./sling:resourceType"])){
            return false;
        }

        var resouceType = editable.params["./sling:resourceType"];

        return ( resouceType === CQ.wcm.EditBase.PARSYS_NEW || resouceType === "foundation/components/iparsys/new");
    }

    function getColor(color){
        color = color.trim();

        if(color.indexOf("#") !== 0){
            color = "#" + color;
        }

        return color;
    }

    function getConfiguration(editComponent) {
        var pageInfo = CQ.utils.WCM.getPageInfo(editComponent.path),
            designConfig = {}, cellSearchPath, parentPath, parName;

        if (!pageInfo || !pageInfo.designObject) {
            return;
        }

        try {
            cellSearchPath = editComponent.cellSearchPath;
            parentPath = editComponent.getParent().path;

            cellSearchPath = cellSearchPath.substring(0, cellSearchPath.indexOf("|"));
            parName = parentPath.substring(parentPath.lastIndexOf("/") + 1);

            designConfig = pageInfo.designObject.content[cellSearchPath][parName];
        } catch (err) {
            console.log("ACS AEM Commons - Error getting parsys configuration", err);
        }

        return designConfig;
    }

    function getParsyses(){
        var parsyses = {};

        _.each(CQ.WCM.getEditables(), function(e){
            if(!isParsysNew(e)){
                return;
            }

            parsyses[e.path] = e;
        });

        return parsyses;
    }

    function configureParsys(){
        var parsyses = getParsyses(), placeholder,
            $placeholder, $pContainer, designConfig;

        _.each(parsyses, function(parsys){
            if(!parsys.emptyComponent) {
                return;
            }

            designConfig = getConfiguration(parsys);

            placeholder = parsys.emptyComponent.findByType("static")[0];

            $placeholder = $(placeholder.el.dom);

            $pContainer = $placeholder.closest(".cq-editrollover-insert-container");

            if(designConfig[ACS_PARSYS_PLACEHOLDER_TEXT]){
                $placeholder.html(designConfig[ACS_PARSYS_PLACEHOLDER_TEXT]);
            }

            if(designConfig[ACS_PARSYS_TEXT_COLOR]){
                $placeholder.css("color", getColor(designConfig[ACS_PARSYS_TEXT_COLOR]));
            }

            if(designConfig[ACS_PARSYS_BG_COLOR]){
                $pContainer.css("background-color", getColor(designConfig[ACS_PARSYS_BG_COLOR]));
            }

            if(designConfig[ACS_PARSYS_BORDER_COLOR]){
                var color = getColor(designConfig[ACS_PARSYS_BORDER_COLOR]);

                parsys.highlight.on("beforeshow", function(highlight){
                    $("#" + highlight.id).css("background-color", color);
                });
            }
        });
    }

    function handleEditMode(){
        CQ.WCM.on("editablesready", configureParsys, this);
    }

    CQ.Ext.onReady(function () {
        if(CQ.WCM.isEditMode()){
            handleEditMode();
        }
    });
}());