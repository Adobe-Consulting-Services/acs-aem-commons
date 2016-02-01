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
            parName = parentPath.substring(parentPath.lastIndexOf("/") + 1);
            currentLimit = pageInfo.designObject.content[cellSearchPath][parName][ACS_COMPONENTS_LIMIT];

            isWithin = getSiblings(editComponent).length <= parseInt(currentLimit);
        }catch(err){
            console.log("ACS Commons - error getting the component limit", err);
        }

        return {
            isWithin: isWithin,
            currentLimit: currentLimit
        };
    }

    function extendDrop(dropFn){
        return function(dragSource, e, data){
            CQ.utils.WCM.getDesign(this.path).getContent();

            var limit = isWithinLimit(this.editComponent);

            if(!limit.isWithin){
                this.editComponent.hideTarget();
                CQ.Ext.Msg.alert('Error', "Limit exceeded, allowed - " + limit.currentLimit);
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