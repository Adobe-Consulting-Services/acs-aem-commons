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
(function($, $document){
    var ACS_PARSYS_PLACEHOLDER_TEXT = "acsParsysPlaceholderText",
        ACS_PARSYS_TEXT_COLOR = "acsParsysTextColor",
        ACS_PARSYS_BG_COLOR = "acsParsysBackgroundColor",
        ACS_PARSYS_BORDER_COLOR = "acsParsysBorderColor",
        PARSYS = "foundation/components/parsys/new",
        IPARSYS = "foundation/components/iparsys/new",
        PARSYS_SELECTOR = "[data-path$='/*']",
        PLACE_HOLDER = "cq-Overlay--placeholder",
        configCache = {};

    function getDesignPath(editable){
        var parsys = editable.getParent(),
            designSrc = parsys.config.designDialogSrc,
            result = {}, param;

        if (designSrc !== undefined) {
            designSrc = designSrc.substring(designSrc.indexOf("?") + 1);

            designSrc.split(/&/).forEach(function (it) {
                if (_.isEmpty(it)) {
                    return undefined;
                }
                param = it.split("=");
                result[param[0]] = param[1];
            });
        }

        if (result.content === undefined) {
            return undefined;
        }

        return decodeURIComponent(result.content);
    }

    function isParsys(editable){
        return editable && (editable.type === PARSYS || editable.type === IPARSYS);
    }

    function getParsyses(){
        var parsys = [];

        if (Granite && Granite.author && Granite.author.edit) {
            _.each(Granite.author.edit.findEditables(), function(editable){
                if(isParsys(editable)){
                    parsys.push(editable);
                }
            });
        }

        return parsys;
    }

    function getColor(color){
        color = color.trim();

        if(color.indexOf("#") !== 0){
            color = "#" + color;
        }

        return color;
    }

    function configureParsys(parsys, type){
        if(!parsys || !parsys.getParent() || !parsys.getParent().overlay){
            return;
        }

        var $overlay = $(parsys.getParent().overlay.dom),
            $placeholder = $overlay.find(PARSYS_SELECTOR);

        if(!$placeholder.hasClass(PLACE_HOLDER)){
            return;
        }

        function configure(data){
            var designPath;

            if(_.isEmpty(data)){
                return;
            }

            configCache[parsys.getParent().path] = data;

            var color;

            if(!_.isEmpty(data[ACS_PARSYS_PLACEHOLDER_TEXT])){
                $placeholder.attr("data-text", data[ACS_PARSYS_PLACEHOLDER_TEXT]);
            }

            if(!_.isEmpty(data[ACS_PARSYS_TEXT_COLOR])){
                $placeholder.css("color", getColor(data[ACS_PARSYS_TEXT_COLOR]));
            }

            if(!_.isEmpty(data[ACS_PARSYS_BG_COLOR])){
                $placeholder.css("background-color", getColor(data[ACS_PARSYS_BG_COLOR]));
            }

            if(!_.isEmpty(data[ACS_PARSYS_BORDER_COLOR]) && type && (type === 'mouseover')){
                $placeholder.css("border-color", getColor(data[ACS_PARSYS_BORDER_COLOR]));
            }
        }

        if(_.isEmpty(configCache[parsys.getParent().path])){
            designPath = getDesignPath(parsys);
            if (designPath !== undefined) {
                $.ajax( designPath + ".2.json" ).done(configure);
            }
        }else{
            configure(configCache[parsys.getParent().path]);
        }
    }

    function extendParsys(event){
        if(event.layer !== "Edit"){
            return;
        }

        _.each(getParsyses(), function(parsys){
            configureParsys(parsys);
        });
    }

    $document.on('cq-layer-activated', extendParsys);

    $document.on("cq-overlay-hover.cq-edit-layer", function (event) {
        if(!event.inspectable){
            return;
        }

        configureParsys(event.inspectable, event.originalEvent.type);
    });
}(jQuery, jQuery(document)));