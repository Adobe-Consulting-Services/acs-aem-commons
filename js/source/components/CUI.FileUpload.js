(function($) {
    CUI.FileUpload = new Class(/** @lends CUI.FileUpload# */{
        toString: 'FileUpload',
        extend: CUI.Widget,

        /**
         Triggered when a file is selected and accepted into the queue

         @name CUI.FileUpload#fileselected
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when a selected file is rejected before upload

         @name CUI.FileUpload#filerejected
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {String} evt.message            The reason why the file has been rejected
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when the internal upload queue changes (file added, file uploaded, etc.)

         @name CUI.FileUpload#queuechanged
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {String} evt.operation          The operation on the queue (ADD or REMOVE)
         @param {int} evt.queueLength           The number of items in the queue
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when selected files list is processed

         @name CUI.FileUpload#filelistprocessed
         @event

         @param {Object} evt                    Event object
         @param {int} evt.addedCount            The number of files that have been added to the processing list
         @param {int} evt.rejectedCount         The number of files that have been rejected
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file starts

         @name CUI.FileUpload#fileuploadstart
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file progresses

         @name CUI.FileUpload#fileuploadprogress
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event (from which the upload ratio can be calculated)
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file is completed (for non-HTML5 uploads only, regardless of success status)

         @name CUI.FileUpload#fileuploadload
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {String} evt.content            The server response to the upload request, which needs to be analyzed to determine if upload was successful
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file succeeded

         @name CUI.FileUpload#fileuploadsuccess
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file failed

         @name CUI.FileUpload#fileuploaderror
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {String} evt.message            The reason why the file upload failed
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file has been cancelled

         @name CUI.FileUpload#fileuploadcanceled
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when dragging over a drop zone

         @name CUI.FileUpload#dropzonedragover
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.originalEvent      The original mouse drag event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when dragging out of a drop zone

         @name CUI.FileUpload#dropzonedragleave
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.originalEvent      The original mouse drag event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when dropping files in a drop zone

         @name CUI.FileUpload#dropzonedrop
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.originalEvent      The original mouse drop event
         @param {FileList} evt.files            The list of dropped files
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         @extends CUI.Widget
         @classdesc A file upload widget

         <p>
         <span class="fileupload button icon-upload" data-init="fileupload"><input type="file" data-placeholder="Select file(s)"></span>
         </p>

         @desc Creates a file upload field
         @constructs

         @param {Object}   options                                    Component options
         @param {String}   [options.name="file"]                      (Optional) name for an underlying form field.
         @param {String}   [options.placeholder=null]                 Define a placeholder for the input field
         @param {String}   [options.uploadUrl=null]                   URL where to upload the file
         @param {String}   [options.uploadUrlBuilder=null]            Upload URL builder
         @param {boolean}  [options.disabled=false]                   Is this component disabled?
         @param {boolean}  [options.multiple=false]                   Can the user upload more than one file?
         @param {int}      [options.sizeLimit=null]                   File size limit
         @param {boolean}  [options.autoStart=false]                  Should upload start automatically once the file is selected?
         @param {String}   [options.fileNameParameter=null]           Name of File name's parameter
         @param {boolean}  [options.useHTML5=true]                    (Optional) Prefer HTML5 to upload files (if browser allows it)
         @param {boolean}  [options.dropZone=null]                    (Optional) Drop zone to upload files from file system directly (if browser allows it)
         @param {Object}   [options.events={}]                        (Optional) Event handlers
         */
        construct: function(options) {
            // Adjust DOM to our needs
            this._render();

            this.inputElement.on("change", function(event) {
                if (this.options.disabled) {
                    return;
                }
                this._onFileSelectionChange(event);
            }.bind(this));
        },

        defaults: {
            name: "file",
            placeholder: null,
            uploadUrl: null,
            uploadUrlBuilder: null,
            disabled: false,
            multiple: false,
            mimeTypes: null,
            sizeLimit: null,
            autoStart: false,
            fileNameParameter: null,
            useHTML5: true,
            dropZone: null,
            events: {}
        },

        inputElement: null,
        $spanElement: null,
        fileNameElement: null,
        uploadQueue: [],

        /** @ignore */
        _render: function() {
            var self = this;

            // If current element is input field -> wrap it into SPAN
            if (this.$element.get(0).tagName === "INPUT") {
                var clazz = this.$element.attr("class");

                var span = $("<span/>", {
                    "class": clazz
                });
                this.$element.removeAttr("class");
                this.$element.after(span);
                this.$element.detach();
                span.prepend(this.$element);
                this.$element = span;
            }

            // Get the span element
            this.$spanElement = this.$element.is("span") ? this.$element : this.$element.find("span");

            // Get the input element
            this.inputElement = this.$element.find("input[type='file']");

            // Read configuration from markup
            this._readDataFromMarkup();

            if (!CUI.util.HTTP.html5UploadSupported()) {
                this.options.useHTML5 = false;
            }

            this._createMissingElements();

            this.$element.addClass("fileupload");
            this.$element.removeClass("focus");

            if (this.inputElement.attr("title")) {
                this.$element.prepend($("<label/>", {
                    "for": self.options.name
                }).html(this.inputElement.attr("title")));
            }

            // Register event handlers
            if (this.options.events) {
                if (typeof this.options.events === "object") {
                    for (var name in this.options.events) {
                        this._registerEventHandler(name, this.options.events[name]);
                    }
                }
            }

            // Register drop zone
            if (this.options.useHTML5) {
                this.options.dropZone = this._registerDropZone();
            } else {
                this.options.dropZone = null;
            }

            if (!this.options.placeholder) {
                this.options.placeholder = this.inputElement.attr("placeholder");
            }

            if (this.options.autoStart) {
                this._registerEventHandler("fileselected", function(event) {
                    event.fileUpload.uploadFile(event.item);
                });
            }

            // URL built via JavaScript function
            if (this.options.uploadUrlBuilder) {
                this.options.uploadUrl = this.options.uploadUrlBuilder(this);
            }

            if (!this.options.uploadUrl || /\$\{.+\}/.test(this.options.uploadUrl)) {
                this.options.disabled = true;
            }

            this._update();
        },

        _registerDropZone: function() {
            var self = this;
            if (self.options.dropZone) {
                // TODO: provide an additional way to get the drop zone via a function

                // Try to get the drop zone via a jQuery selector
                try {
                    self.options.dropZone = $(self.options.dropZone);
                } catch (e) {
                    delete self.options.dropZone;
                }

                if (self.options.dropZone) {
                    self.options.dropZone
                        .on("dragover", function(e) {
                            if (self._isActive()) {
                                self.isDragOver = true;

                                if (e.stopPropagation) {
                                    e.stopPropagation();
                                }
                                if (e.preventDefault) {
                                    e.preventDefault();
                                }

                                self.$element.trigger({
                                    type: "dropzonedragover",
                                    originalEvent: e,
                                    fileUpload: self
                                });
                            }

                            return false;
                        })
                        .on("dragleave", function(e) {
                            if (self._isActive()) {
                                if (e.stopPropagation) {
                                    e.stopPropagation();
                                }
                                if (e.preventDefault) {
                                    e.preventDefault();
                                }

                                self.isDragOver = false;
                                window.setTimeout(function() {
                                    if (!self.isDragOver) {
                                        self.$element.trigger({
                                            type: "dropzonedragleave",
                                            originalEvent: e,
                                            fileUpload: self
                                        });
                                    }
                                }, 1);
                            }

                            return false;
                        })
                        .on("drop", function(e) {
                            if (self._isActive()) {
                                if (e.stopPropagation) {
                                    e.stopPropagation();
                                }
                                if (e.preventDefault) {
                                    e.preventDefault();
                                }

                                var files = e.originalEvent.dataTransfer.files;

                                self.$element.trigger({
                                    type: "dropzonedrop",
                                    originalEvent: e,
                                    files: files,
                                    fileUpload: self
                                });

                                self._onFileSelectionChange(e, files);
                            }

                            return false;
                        })
                    ;
                }
            }
        },

        _registerEventHandler: function(name, handler) {
            this.$element.on(name, handler);
        },

        _createMissingElements: function() {
            var self = this;

            var multiple = self.options.useHTML5 && self.options.multiple;
            if (self.inputElement.length === 0) {
                self.inputElement = $("<input/>", {
                    type: "file",
                    name: self.options.name,
                    multiple: multiple
                });
                self.$element.prepend(self.inputElement);
            } else {
                self.inputElement.attr("multiple", multiple);
            }
        },

        /** @ignore */
        _readDataFromMarkup: function() {
            var self = this;
            if (this.inputElement.attr("name")) {
                this.options.name = this.inputElement.attr("name");
            }
            if (this.inputElement.attr("placeholder")) {
                this.options.placeholder = this.inputElement.attr("placeholder");
            }
            if (this.inputElement.attr("data-placeholder")) {
                this.options.placeholder = this.inputElement.attr("data-placeholder");
            }
            if (this.inputElement.attr("disabled") || this.inputElement.attr("data-disabled")) {
                this.options.disabled = true;
            }
            if (this.inputElement.attr("multiple") || this.inputElement.attr("data-multiple")) {
                this.options.multiple = true;
            }
            if (this.inputElement.attr("data-upload-url")) {
                this.options.uploadUrl = this.inputElement.attr("data-upload-url");
            }
            if (this.inputElement.attr("data-upload-url-builder")) {
                this.options.uploadUrlBuilder = CUI.util.buildFunction(this.inputElement.attr("data-upload-url-builder"), ["fileUpload"]);
            }
            if (this.inputElement.attr("data-size-limit")) {
                this.options.sizeLimit = this.inputElement.attr("data-size-limit");
            }
            if (this.inputElement.attr("data-auto-start")) {
                this.options.autoStart = true;
            }
            if (this.inputElement.attr("data-usehtml5")) {
                this.options.useHTML5 = this.inputElement.attr("data-usehtml5") === "true";
            }
            if (this.inputElement.attr("data-dropzone")) {
                this.options.dropZone = this.inputElement.attr("data-dropzone");
            }
            if (this.inputElement.attr("data-file-name-parameter")) {
                this.options.fileNameParameter = this.inputElement.attr("data-file-name-parameter");
            }
            $.each(this.inputElement.get(0).attributes, function(i, attribute) {
                var match = /^data-event-(.*)$/.exec(attribute.name);
                if (match && match.length > 1) {
                    var eventHandler = CUI.util.buildFunction(attribute.value, ["event"]);
                    if (eventHandler) {
                        self.options.events[match[1]] = eventHandler.bind(self);
                    }
                }
            });
        },

        /** @ignore */
        _update: function() {
            if (this.options.placeholder) {
                this.inputElement.attr("placeholder", this.options.placeholder);
            }

            if (this.options.disabled) {
                this.$element.addClass("disabled");
                this.inputElement.attr("disabled", "disabled");
            } else {
                this.$element.removeClass("disabled");
                this.inputElement.removeAttr("disabled");
            }
        },

        /** @ignore */
        _onFileSelectionChange: function(event, files) {
            var addedCount = 0, rejectedCount = 0;
            if (this.options.useHTML5) {
                files = files || event.target.files;
                for (var i = 0; i < files.length; i++) {
                    if (this._addFile(files[i])) {
                        addedCount++;
                    } else {
                        rejectedCount++;
                    }
                }
            } else {
                if (this._addFile(event.target)) {
                    addedCount++;
                } else {
                    rejectedCount++;
                }
            }

            this.$element.trigger({
                type: "filelistprocessed",
                addedCount: addedCount,
                rejectedCount: rejectedCount,
                fileUpload: this
            });
        },

        /** @ignore */
        _addFile: function(file) {
            var self = this;

            var fileName;
            if (this.options.useHTML5) {
                fileName = file.name;
            } else {
                fileName = $(file).attr("value");
            }
            if (fileName.lastIndexOf("\\") !== -1) {
                fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            }

            if (!self._getQueueItemByFileName(fileName)) {
                var item = {
                    fileName: fileName
                };
                if (this.options.useHTML5) {
                    item.file = file;
                    item.fileSize = file.size;

                    // Check file size
                    if (self.options.sizeLimit && file.size > self.options.sizeLimit) {
                        self.$element.trigger({
                            type: "filerejected",
                            item: item,
                            message: "File is too big",
                            fileUpload: self
                        });
                        return false;
                    }
                }

                // Add item to queue
                self.uploadQueue.push(item);
                self.$element.trigger({
                    type: "queuechanged",
                    item: item,
                    operation: "ADD",
                    queueLength: self.uploadQueue.length,
                    fileUpload: self
                });

                self.$element.trigger({
                    type: "fileselected",
                    item: item,
                    fileUpload: self
                });

                return true;
            }

            return false;
        },

        /** @ignore */
        _getQueueIndex: function(fileName) {
            var index = -1;
            $.each(this.uploadQueue, function(i, item) {
                if (item.fileName === fileName) {
                    index = i;
                    return false;
                }
            });
            return index;
        },

        /** @ignore */
        _getQueueItem: function(index) {
            return index > -1 ? this.uploadQueue[index] : null;
        },

        /** @ignore */
        _getQueueItemByFileName: function(fileName) {
            return this._getQueueItem(this._getQueueIndex(fileName));
        },

        /**
         Upload a file item

         @param {Object} item                   Object representing a file item
         */
        uploadFile: function(item) {
            var self = this;

            if (self.options.useHTML5) {
                item.xhr = new XMLHttpRequest();
                item.xhr.addEventListener("loadstart", function(e) { self._onUploadStart(e, item); }, false);
                item.xhr.addEventListener("load", function(e) { self._onUploadLoad(e, item); }, false);
                item.xhr.addEventListener("error", function(e) { self._onUploadError(e, item); }, false);
                item.xhr.addEventListener("abort", function(e) { self._onUploadCanceled(e, item); }, false);

                var upload = item.xhr.upload;
                upload.addEventListener("progress", function(e) { self._onUploadProgress(e, item); }, false);

                // TODO: encoding of special characters in file names
                var file = item.file;
                var fileName = item.fileName;
                if (window.FormData) {
                    var f = new FormData();
                    if (self.options.fileNameParameter) {
                        // Custom file and file name parameter
                        f.append(self.inputElement.attr("name"), file);
                        f.append(self.options.fileNameParameter || "fileName", fileName);
                    } else {
                        f.append(fileName, file);
                    }
                    f.append("_charset_", "utf-8");

                    item.xhr.open("POST", self.options.uploadUrl + "?:ck=" + new Date().getTime(), true);
                    item.xhr.send(f);
                } else {
                    item.xhr.open("PUT", self.options.uploadUrl + "/" + fileName, true);
                    item.xhr.send(file);
                }

            } else {
                var $body = $(document.body);

                // Build an iframe
                var iframeName = "upload-" + new Date().getTime();
                var $iframe = $("<iframe/>", {
                    name: iframeName
                });
                $iframe.addClass("fileupload").appendTo($body);

                // Build a form
                var $form = $("<form/>", {
                    method: "post",
                    enctype: "multipart/form-data",
                    action: self.options.uploadUrl,
                    target: iframeName
                });
                $form.addClass("fileupload").appendTo($body);

                var $charset = $("<input/>", {
                    type: "hidden",
                    name: "_charset_",
                    value: "utf-8"
                });
                $form.prepend($charset);

                // Define value of the file name element
                if (this.options.fileNameParameter) {
                    this.fileNameElement = $("<input/>", {
                        type: "hidden",
                        name: this.options.fileNameParameter,
                        value: item.fileName
                    });
                    $form.prepend(this.fileNameElement);
                }

                $iframe.one("load", function() {
                    var content = this.contentWindow.document.body.innerHTML;
                    self.inputElement.prependTo(self.$spanElement);
                    $form.remove();
                    $iframe.remove();

                    self.$element.trigger({
                        type: "fileuploadload",
                        item: item,
                        content: content,
                        fileUpload: self
                    });
                });

                self.inputElement.prependTo($form);
                $form.submit();
            }
        },

        /**
         Cancel upload of a file item

         @param {Object} item                   Object representing a file item
         */
        cancelUpload: function(item) {
            item.xhr.abort();
        },

        /** @ignore */
        _onUploadStart: function(e, item) {
            this.$element.trigger({
                type: "fileuploadstart",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadProgress: function(e, item) {
            // Update progress bar
            this.$element.trigger({
                type: "fileuploadprogress",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadLoad: function(e, item) {
            var request = e.target;
            if (request.readyState === 4) {
                this._internalOnUploadLoad(e, item, request.status, request.responseText);
            }
        },

        /** @ignore */
        _internalOnUploadLoad: function(e, item, requestStatus, responseText) {
            if (CUI.util.HTTP.isOkStatus(requestStatus)) {
                this.$element.trigger({
                    type: "fileuploadsuccess",
                    item: item,
                    originalEvent: e,
                    fileUpload: this
                });
            } else {
                this.$element.trigger({
                    type: "fileuploaderror",
                    item: item,
                    originalEvent: e,
                    message: responseText,
                    fileUpload: this
                });
            }

            // Remove file name element if needed
            if (this.fileNameElement) {
                this.fileNameElement.remove();
            }

            // Remove queue item
            this.uploadQueue.splice(this._getQueueIndex(item.fileName), 1);
            this.$element.trigger({
                type: "queuechanged",
                item: item,
                operation: "REMOVE",
                queueLength: this.uploadQueue.length,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadError: function(e, item) {
            this.$element.trigger({
                type: "fileuploaderror",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadCanceled: function(e, item) {
            this.$element.trigger({
                type: "fileuploadcanceled",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _isActive: function() {
            return !this.inputElement.is(':disabled');
        }

    });

    CUI.util.plugClass(CUI.FileUpload);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            $("[data-init~='fileupload']", e.target).fileUpload();
        });
    }

}(window.jQuery));
