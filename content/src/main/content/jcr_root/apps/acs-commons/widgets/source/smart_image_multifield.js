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
/**
 * @class ACS.CQ.form.ImageMultiField.MultiField
 * @extends CQ.form.MultiField
 * <p>ImageMultiField widget for adding unlimited number of CQ.html5.form.SmartImage widgets in a component dialog </p>
 * <code>Sample configuration
 * <basic
 *     jcr:primaryType="cq:Widget"
 *     title="Images"
 *     xtype="panel">
 *     <items jcr:primaryType="cq:WidgetCollection">
 *          <images
 *              jcr:primaryType="cq:Widget"
 *              border="false"
 *              hideLabel="true"
 *              name="./images"
 *              xtype="imagemultifield">
 *              <fieldConfig
 *                  jcr:primaryType="cq:Widget"
 *                  border="false"
 *                  hideLabel="true"
 *                  layout="form"
 *                  padding="10px 0 0 100px"
 *                  xtype="imagemultifieldpanel">
 *                  <items jcr:primaryType="cq:WidgetCollection">
 *                          <image
 *                          jcr:primaryType="cq:Widget"
 *                          cropParameter="./imageCrop"
 *                          ddGroups="[media]"
 *                          fileNameParameter="./imageName"
 *                          fileReferenceParameter="./imageReference"
 *                          height="250"
 *                          mapParameter="./imageMap"
 *                          name="./image"
 *                          rotateParameter="./imageRotate"
 *                          sizeLimit="100"
 *                          xtype="imagemultifieldsmartimage"/>
 *                  </items>
 *              </fieldConfig>
 *          </images>
 *     </items>
 * </basic>
 * </code>
 * @constructor
 * Creates a new ImageMultiField.MultiField.
 * @param {Object} config The config object
 **/
CQ.Ext.ns("ACS.CQ.form.ImageMultiField");

ACS.CQ.form.ImageMultiField.Panel = CQ.Ext.extend(CQ.Ext.Panel, {
    initComponent: function () {
        ACS.CQ.form.ImageMultiField.Panel.superclass.initComponent.call(this);

        var multifield = this.findParentByType('imagemultifield'),
            image = this.find('xtype', 'imagemultifieldsmartimage')[0],
            imageName = multifield.nextImageName,
            changeParams = ["cropParameter", "fileNameParameter","fileReferenceParameter",
                                "mapParameter","rotateParameter" ];

        if(!imageName){
            imageName = image.name;

            if(!imageName){
                imageName = "demo";
            }else if(imageName.indexOf("./") === 0){
                imageName = imageName.substr(2); //get rid of ./
            }

            multifield.nextImageNum = multifield.nextImageNum + 1;
            imageName = this.name + "/" + imageName + "-" + multifield.nextImageNum;
        }

        image.name = imageName;

        CQ.Ext.each(changeParams, function(cItem){
            if(image[cItem]){
                image[cItem] = imageName + "/" +
                    ( image[cItem].indexOf("./") === 0 ? image[cItem].substr(2) : image[cItem]);
            }
        });

        CQ.Ext.each(image.imageToolDefs, function(toolDef){
            toolDef.transferFieldName = imageName + toolDef.transferFieldName.substr(1);
            toolDef.transferField.name = toolDef.transferFieldName;
        });
    },

    setValue: function (record) {
        var multifield = this.findParentByType('imagemultifield'),
            image = this.find('xtype', 'imagemultifieldsmartimage')[0],
            recCopy = CQ.Util.copyObject(record),
            imagePath = multifield.path + "/" + image.name,
            imgRec = recCopy.get(image.name), x, fileRefParam;

        for(x in imgRec){
            if(imgRec.hasOwnProperty(x)){
                recCopy.data[x] = imgRec[x];
            }
        }

        recCopy.data[this.name.substr(2)] = undefined;

        fileRefParam = image.fileReferenceParameter;
        image.fileReferenceParameter = fileRefParam.substr(fileRefParam.lastIndexOf("/") + 1);

        image.processRecord(recCopy, imagePath);
        image.fileReferenceParameter = fileRefParam;
    },

    validate: function(){
        return true;
    }
});

CQ.Ext.reg("imagemultifieldpanel", ACS.CQ.form.ImageMultiField.Panel);

ACS.CQ.form.ImageMultiField.SmartImage = CQ.Ext.extend(CQ.html5.form.SmartImage, {
    syncFormElements: function() {
        if(!this.fileNameField.getEl().dom){
            return;
        }

        ACS.CQ.form.ImageMultiField.SmartImage.superclass.syncFormElements.call(this);
    } ,

    afterRender: function() {
        ACS.CQ.form.ImageMultiField.SmartImage.superclass.afterRender.call(this);

        var dialog = this.findParentByType('dialog'),
            target = this.dropTargets[0], multifield, dialogZIndex;

        if (dialog && dialog.el && target.highlight) {
            dialogZIndex = parseInt(dialog.el.getStyle("z-index"), 10);

            if (!isNaN(dialogZIndex)) {
                target.highlight.zIndex = dialogZIndex + 1;
            }
        }

        multifield = this.findParentByType('multifield');
        multifield.dropTargets.push(target);

        this.dropTargets = undefined;
    }
});

CQ.Ext.reg('imagemultifieldsmartimage', ACS.CQ.form.ImageMultiField.SmartImage);

CQ.Ext.override(CQ.form.SmartImage.ImagePanel, {
    addCanvasClass: function(clazz) {
        var imageCanvas = CQ.Ext.get(this.imageCanvas);

        if(imageCanvas){
            imageCanvas.addClass(clazz);
        }
    },

    removeCanvasClass: function(clazz) {
        var imageCanvas = CQ.Ext.get(this.imageCanvas);

        if(imageCanvas){
            imageCanvas.removeClass(clazz);
        }
    }
});

CQ.Ext.override(CQ.form.SmartImage.Tool, {
    processRecord: function(record) {
        var iniValue = record.get(this.transferFieldName);

        if(!iniValue && ( this.transferFieldName.indexOf("/") !== -1 )){
            iniValue = record.get(this.transferFieldName.substr(this.transferFieldName.lastIndexOf("/") + 1));
        }

        if (iniValue === null) {
            iniValue = "";
        }

        this.initialValue = iniValue;
    }
});

CQ.Ext.override(CQ.form.MultiField.Item, {
    reorder: function(item) {
        if(item.field && item.field.xtype === "imagemultifieldpanel"){
            var c = this.ownerCt, iIndex = c.items.indexOf(item), tIndex = c.items.indexOf(this);

            if(iIndex < tIndex){ //user clicked up
                c.insert(c.items.indexOf(item), this);
                this.getEl().insertBefore(item.getEl());
            }else{//user clicked down
                c.insert(c.items.indexOf(this), item);
                this.getEl().insertAfter(item.getEl());
            }

            c.doLayout();
        }else{
            item.field.setValue(this.field.getValue());
            this.field.setValue(item.field.getValue());
        }
    }
});

ACS.CQ.form.ImageMultiField.MultiField = CQ.Ext.extend(CQ.form.MultiField , {
    Record: CQ.data.SlingRecord.create([]),
    nextImageNum: 0,
    nextImageName: undefined,

    initComponent: function() {
        ACS.CQ.form.ImageMultiField.MultiField.superclass.initComponent.call(this);

        var imagesOrder = new CQ.Ext.form.Hidden({
            name: this.getName() + "/order"
        }), dialog;

        this.add(imagesOrder);

        dialog = this.findParentByType('dialog');

        dialog.on('beforesubmit', function(){
            var imagesInOrder = this.find('xtype','imagemultifieldsmartimage'),
                order = [];

            CQ.Ext.each(imagesInOrder , function(image){
                order.push(image.name.substr(image.name.lastIndexOf("/") + 1));
            });

            imagesOrder.setValue(JSON.stringify(order));
        },this);

        this.dropTargets = [];
    },

    addItem: function(value){
        if(!value){
            value = new this.Record({},{});
        }
        ACS.CQ.form.ImageMultiField.MultiField.superclass.addItem.call(this, value);
    },

    processRecord: function(record, path) {
        if (this.fireEvent('beforeloadcontent', this, record, path) !== false) {
            this.items.each(function(item) {
                if(item.field && item.field.xtype === "imagemultifieldpanel"){
                    this.remove(item, true);
                }
            }, this);

            var images = record.get(this.getName()), oName, oValue, iNames, highNum, val;
            this.nextImageNum = 0;

            if (images) {
                oName = this.getName() + "/order";
                oValue = record.get(oName) ? record.get(oName) : "";
                iNames = JSON.parse(oValue);

                CQ.Ext.each(iNames, function(iName){
                    val = parseInt(iName.substr(iName.indexOf("-") + 1), 10);

                    if(!highNum || highNum < val){
                        highNum = val;
                    }

                    this.nextImageName = this.getName() + "/" + iName;
                    this.addItem(record);
                }, this);

                this.nextImageNum = highNum;
            }

            this.nextImageName = undefined;

            this.fireEvent('loadcontent', this, record, path);
        }
    }
});

CQ.Ext.reg('imagemultifield', ACS.CQ.form.ImageMultiField.MultiField);

