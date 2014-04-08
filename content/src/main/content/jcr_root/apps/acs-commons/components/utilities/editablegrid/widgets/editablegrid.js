/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
CQ.Ext.ns("ACS.CQ.grid");
ACS.CQ.grid.EditorGridPanel = CQ.Ext.extend(CQ.Ext.grid.EditorGridPanel, {
    url:"",
    id:"gridpanel",
    updateurl:"",
    deleteurl:"",
    basePath : "",
    gridcolumns:['key','text','value'],
    gridfields :['uid'],
    region: "center",
    selModel: new CQ.Ext.grid.RowSelectionModel(),
    loadMask: true,
    filterable:true,
    clicksToEdit:2,
    cm:null,
    regexQuote : function(str) {
        return (str).replace(/([\\\.\+\*\?\[\^\]\$\(\)\{\}\=\!<\>\|\:])/g, "\\$1");
    },
    loadPath : function() {
        this.store.baseParams.path = this.basePath;
        this.store.reload();        
    }
   ,generateRandomId:function(){
       function _p8(s) {
           var p = (Math.random().toString(16)+"000000000").substr(2,8);
           return s ? "-" + p.substr(0,4) + "-" + p.substr(4,4) : p ;
       }
       return _p8() + _p8(true) + _p8(true) + _p8();
   },
    updateSaveButton : function(store) {
        CQ.Ext.getCmp("strings-save-button").setDisabled(!store.isDirty());
    },
    filter:function(opts){
        var filterTerm,filterRegex;
        if (!opts) {
            filterTerm = null;
            filterRegex = null;
            this.store.clearFilter();
            return;
        }
        filterTerm = opts.term;
        filterRegex = new RegExp(this.regexQuote(CQ.Ext.util.Format.htmlEncode(filterTerm)), "ig");
        
        this.store.filterBy(function(rec, id) {
            var found = false;
            // check for modified / new / deleted records
            if (opts.onlyChanged) {
                if (!rec.dirty && !rec.phantom && !rec.deleted) {
                    return false;
                }
            } else {
                if (opts.onlyModified && !rec.dirty) {
                    return false;
                }
                if (opts.onlyNew && !rec.phantom) {
                    return false;
                }
                if (opts.onlyDeleted && !rec.deleted) {
                    return false;
                }
            }
            // check all fields for occurrence of the filter term
            if (filterTerm) {
                rec.fields.each(function(field) {
                    var value = rec.get(field.name);
                    if (value && value.toLowerCase().indexOf(filterTerm.toLowerCase()) >= 0) {
                        found = true;
                        return false; // stop fields.each iteration
                    }
                    return true;
                });
            } else {
                found = true;
            }
            return found;
        });
    },
    view: new CQ.Ext.grid.GridView({
        // render rows as they come into viewable area.
        scrollDelay: false,
        getRowClass: function(record, index, rowParams, store) {
            if (record.deleted) {
                return " deleted-row";
            }
            return "";
        }
    }),
    initComponent : function(){
        var $this = this;
        CQ.Ext.each(this.gridcolumns, function(column) {
            $this.gridfields.push(column);
        });
        this.store = this.createStore();
        this.cm = new CQ.Ext.grid.ColumnModel(this.createColumnConfig());
        this.getSelectionModel().on("selectionchange", function(sm) {
            CQ.Ext.getCmp("strings-remove-button").setDisabled(sm.getCount() < 1);
        });
        
        this.store.on("add",  this.updateSaveButton);
        this.store.on("update",  this.updateSaveButton);
        this.store.on("remove",  this.updateSaveButton);
        this.store.on("save",  this.updateSaveButton);
        this.store.on("load",  this.updateSaveButton);
        this.store.on("beforesave", function() {
            $this.body.mask("Saving...");
        });
        this.store.on("save", function() {
            $this.body.unmask();
        });
        ACS.CQ.grid.EditorGridPanel.superclass.initComponent.call(this);
    },createStore :function(){
        var $this = this,store;
         store = new CQ.Ext.data.JsonStore({
            root: 'grid',
            idProperty :'uid',
            url:$this.url,
            baseParams: {
                path: $this.url
            },
            
            autoSave:false,
            writer: new CQ.Ext.data.JsonWriter({
                encode: false,
                listful: true,
                writeAllFields: true
            }),
            proxy: new CQ.Ext.data.HttpProxy({
                api: {
                    read: { url: $this.url, method: 'GET' },
                    update: $this.updateurl,
                    create: $this.updateurl,
                    destroy: $this.deleteurl
                },
                headers: {"Content-Type": "application/json; charset=utf-8"}
            }),
            fields:$this.gridfields,
            markForRemove: function(record) {
                if (record) {
                    record.deleted = true;
                }
            },
            isDirty: function() {
                
                if (this.getModifiedRecords().length > 0) {
                    return true;
                }
                var dirty = false;
                this.each(function (record) {
                    if (record.deleted) {
                        dirty = true;
                        return false;
                    }
                });
                return dirty;
            },
            saveAll: function() {
                // delete records marked for deletion
                this.each(function (record) {
                    record.id=record.data.uid;
                    if (record.deleted) {
                        this.remove(record);
                    }
                }, this);
                this.save();
            },
            onCreateRecords : function(success, rs, data) {
                this.commitChanges();
                this.each(function (record) {
                    record.phantom = false;
                }, this);
            } 
        });
         /*
         */
      return store;
    },createColumnConfig:function(){
        var columnModelConfig = {
                defaults: {
                    width: 120,
                    sortable: true
                },
                columns:[]      
        };
       
    
        CQ.Ext.each(this.gridcolumns, function(column) {
            columnModelConfig.columns.push({
                header: column.toUpperCase(),
                dataIndex: column,
                width: 200,
                sortable:true,
               
                filter: {
                    type: 'string'
                },
                editor: new CQ.Ext.form.TextField()
            });
           
        });
        return columnModelConfig;
    }
});
CQ.Ext.reg('acsgrid', ACS.CQ.grid.EditorGridPanel);
ACS.CQ.grid.ViewportPanel = CQ.Ext.extend(CQ.Ext.Panel, {
    grid:null,
    layout:"border",
    renderTo:"CQ",
    border:true,
    widthpercolumn:200,
      height:600,
    margins: '15 15 15 5',
    initComponent : function(){
        var grid = this.grid,width;
       width = (grid.gridcolumns.length+1)*this.widthpercolumn +10;
      
      if(width<800){
          width=800;
      }
     // this.width = width;
        this.items =[{
              region:"center",
              layout:"fit",
              width:800,
              items:{
                  layout: "border",
                  
                  border: false
                  ,                  tbar:[{
                      text: "Save",
                      id: "strings-save-button",
                      iconCls: "save-icon",
                      disabled: true,
                      handler: function() {
                          grid.store.saveAll();
                      }
                          }, {
                              text: "Reset & Refresh",
                              iconCls: "refresh-icon",
                              handler: function() {
                                  grid.store.reload();
                              }
                       },{
                           text: "Add",
                           iconCls: "add-icon",
                           handler: function() {
                             
                               grid.stopEditing();
                               var Record = grid.store.recordType,newRecord;
                                newRecord = new Record({
                                   "uid": grid.generateRandomId()
                               });

                                CQ.Ext.each(grid.gridcolumns, function(column) {
                                    newRecord.data[column]='';
                                });
                                grid.store.insert(0,newRecord);
                               grid.startEditing(0, 0);
                           }
                       },{
                           id: "strings-remove-button",
                           text: "Remove",
                           iconCls: "remove-icon",
                           disabled: true,
                           handler: function() {
                               var s = grid.getSelectionModel().getSelections(),i,rec;
                               for (i = 0 ;i < s.length; i++) {
                                   rec = s[i];
                                   rec.id=rec.data.uid;
                                   if (rec.phantom) {
                                       grid.store.remove(rec);
                                   } else {
                                      grid.store.markForRemove(rec);
                                   }
                                   grid.store.fireEvent("update", grid.store, rec, CQ.Ext.data.Record.EDIT);
                               }
                           }
                   }]
,items:[{
                       region:"north",
                       layout:"fit",
                       width:800,
                       height:50,
                       items:[this.createFilterBar()]
                   },{
                       region:"center",
                       layout:"fit",
                       width:800,
                       items:[this.grid]
                   }]

              }
          }
                  ];
        ACS.CQ.grid.ViewportPanel.superclass.initComponent.call(this); 
        this.grid.loadPath();
    },createFilterBar:function(){
        
        var filterBar,grid =this.grid;
        filterBar= {
                region: "south",
                height: 75,
                cls: "label",
                border: false,
                layout: "hbox",
                layoutConfig: {
                    padding: "5",
                    align: "middle"
                },
                defaults: {margins:'0 5 0 0'},
                items: [{
                        html: "Filter by text:",
                        border: false
                    },{
                        id: "filter-term-field",
                        xtype: "textfield",
                        listeners: {
                            // run trigger on ENTER as well
                            specialkey: function(field, e) {
                                if (e.getKey() === CQ.Ext.EventObject.ENTER) {
                                    filterBar.runFilter();
                                }
                            }
                        }
                    },{
                        xtype: "fieldset",
                        width: 290,
                        title: "Changes",
                        layout: "hbox",
                        layoutConfig: {
                            padding: "0",
                            align: "middle"
                        },
                        defaults: {margins:'0 5 0 0'},
                        style: "padding: 3px",
                        
                        items: [{
                            id: "any-change-checkbox",
                            xtype: "checkbox",
                            boxLabel: "Any"
                        },{
                            id: "only-mod-checkbox",
                            xtype: "checkbox",
                            boxLabel: "Modified"
                        },{
                            id: "only-new-checkbox",
                            xtype: "checkbox",
                            boxLabel: "New"
                        },{
                            id: "only-deleted-checkbox",
                            xtype: "checkbox",
                            boxLabel: "Deleted"
                        }]
                    },{
                        xtype: "button",
                        text: "Filter",
                        margins: '0 5 0 5',
                        listeners: {
                            click: function(button, e) {
                                filterBar.runFilter();
                            }
                        }
                    },{
                        id: "clear-filter-button",
                        xtype: "button",
                        text: "Clear",
                        disabled: true,
                        listeners: {
                            click: function(button, e) {
                                filterBar.clearFilter();
                            }
                        }
                }],
                runFilter: function() {
                    var filterOpts = {
                        term:         CQ.Ext.getCmp("filter-term-field").getValue(),
                        onlyChanged:  CQ.Ext.getCmp("any-change-checkbox").getValue(),
                        onlyModified: CQ.Ext.getCmp("only-mod-checkbox").getValue(),
                        onlyNew:      CQ.Ext.getCmp("only-new-checkbox").getValue(),
                        onlyDeleted:  CQ.Ext.getCmp("only-deleted-checkbox").getValue()
                    };
                    grid.filter(filterOpts);
                    CQ.Ext.getCmp("clear-filter-button").enable();
                },
                clearFilter: function() {
                    CQ.Ext.getCmp("clear-filter-button").disable();
                    
                    CQ.Ext.getCmp("filter-term-field").setValue("");
                    CQ.Ext.getCmp("any-change-checkbox").setValue(false);
                    CQ.Ext.getCmp("only-mod-checkbox").setValue(false);
                    CQ.Ext.getCmp("only-new-checkbox").setValue(false);
                    CQ.Ext.getCmp("only-deleted-checkbox").setValue(false);
                    grid.filter();
                }
            };
        return filterBar;
    }
});
CQ.Ext.reg('acsgridviewport', ACS.CQ.grid.ViewportPanel);