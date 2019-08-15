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
(function(){
    var pathName = window.location.pathname,
        ACS_COMPONENTS_LIMIT = "acsComponentsLimit";

    if( ( pathName !== "/cf" ) && ( pathName.indexOf("/content") !== 0)){
        return;
    }

    function getSiblings(editable){
        var parent, siblings = [];

        _.each(CQ.WCM.getEditables(), function(e){
            parent = e.getParent();

            if(!parent || (parent.path !== editable.getParent().path)){
                return;
            }

            siblings.push(e);
        });

        return siblings;
    }

    function isWithinLimit(editComponent){
        var pageInfo = CQ.utils.WCM.getPageInfo(editComponent.path),
            isWithin = true, currentLimit = "",
            cellSearchPath, parentPath, parName;

        if(!pageInfo || !pageInfo.designObject){
            return;
        }

        try{
            cellSearchPath = editComponent.cellSearchPath;
            parentPath = editComponent.getParent().path;

            cellSearchPath = cellSearchPath.substring(0, cellSearchPath.indexOf("|"));
            parNames = parentPath.split("jcr:content/");
            parNames = parNames[1].split("/");

            cellSearchPathInfo = pageInfo.designObject.content[cellSearchPath];

            for(var i = 0; i < parNames.length; i++){
                var prop = parNames[i];
                cellSearchPathInfo = cellSearchPathInfo[prop];
                if (!cellSearchPathInfo) {
                    return;
                }
            }
            currentLimit = cellSearchPathInfo[ACS_COMPONENTS_LIMIT];
            if(currentLimit){
                isWithin = getSiblings(editComponent).length <= parseInt(currentLimit);
            }
        }catch(err){
            if(console && console.log) {
                console.log("ACS Commons - error getting the component limit", err);
            }
        }

        return {
            isWithin: isWithin,
            currentLimit: currentLimit
        };
    }

    function extendDrop(dropFn){
        return function(dragSource, e, data){
            var limit = isWithinLimit(this.editComponent);

            if(typeof limit !== 'undefined' && !limit.isWithin){
                this.editComponent.hideTarget();
                CQ.Ext.Msg.alert('Error', "Limit exceeded, allowed: " + limit.currentLimit);
                return false;
            }

            return dropFn.call(this, dragSource, e, data);
        };
    }

    function applyLimitAndExtendDrop() {
        var editables = CQ.utils.WCM.getEditables();

        _.each(editables, function (editable) {
            var dropTargets = editable.getDropTargets();

            if(_.isEmpty(dropTargets)){
                return;
            }

            dropTargets[0].notifyDrop = extendDrop(dropTargets[0].notifyDrop);
        });
    }

    CQ.Ext.onReady(function() {
        CQ.WCM.on("editablesready", applyLimitAndExtendDrop, this);
    });
}());
