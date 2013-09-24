(function($) {
    
    var COLOPICKER_FOOTER_TEMPLATE = "<div class=\"button-group navigation-bar\"></div>";
    var CLASSIC_PALETTE_BUTTON = "<button id='classicButton'><i class=\"icon-viewgrid\"></i></button>";
    var EDIT_BUTTON = "<button id='editButton'><i class=\"icon-edit\"></i></button>";
    var COLOR_SLIDER_BUTTON = "<button><i class=\"icon-properties\"></i></button>";
    var EDIT_MODE = "editMode";
    var CLASSIC_MODE = "classicMode";

    CUI.Colorpicker = new Class(
            /** @lends CUI.Colorpicker# */
            {
                toString : 'Colorpicker',

                extend : CUI.Widget,

                defaults : {
                    config : {
                        colors : {},
                        displayModes : {}
                    },
                    disabled : false,
                    name : null,
                    title : ""
                },

                palettePageSize : 3,
                colorShadeNo : 6,
                lowerLimit : 0,
                upperLimit : 0,
                colorNames : [],
                currentPage : 0,
                pages : 1,

                /**
                 * @extends CUI.Widget
                 * @classdesc Colorpicker will create markup after the template.
                 * 
                 * @desc Creates a new colorpicker
                 * @constructs
                 */
                construct : function(options) {
                    this._readDataFromMarkup();
                    this._adjustMarkup();

                    if (this.options.config === null ||
                            this.options.config.colors.length === 0) {
                        this.options.disabled = true;
                    }
                    if (!this.options.disabled &&
                            (this.options.config.displayModes.classicPalette && this.options.config.displayModes.freestylePalette)) {
                        this.options.disabled = true;
                    }
                    if (!this.options.disabled &&
                            (this.options.config.displayModes.length === 0 || (!this.options.config.displayModes.classicPalette && !this.options.config.displayModes.freestylePalette))) {
                        this.options.config.displayModes.classicPalette = true;
                    }

                    this.$openButton = this.$element
                            .find('input.colorpicker-launcher');
                    this.$hiddenInput = this.$element.find("input[name='" +
                            this.options.name + "']");
                    
                    if (this.$element.attr("value")) {
                        var initialVal = this.$element.attr("value");
                        if(CUI.util.color.isValid("rgba", initialVal) || CUI.util.color.isValid("rgb", initialVal)){
                            this._setColor(initialVal);
                        }else{
                            this.$element.removeAttr("value");
                        }
                        
                    }

                    if (this.options.disabled) {
                        this.$element.find(">input").attr("disabled",
                                "disabled");
                    } else {
                        this.colorNames = [];
                        $.each(this.options.config.colors,
                                function(key, value) {
                                    this.colorNames.push(key);
                                }.bind(this));
                        $('body').off(
                                "tap." + this.options.name + " click." +
                                        this.options.name).fipo(
                                "tap." + this.options.name,
                                "click." + this.options.name, function(event) {
                                    if (!this.keepShown) {
                                        if (this.$element.find(".popover").has(event.target).length === 0)
                                        {
                                            this._hidePicker();
                                        }
                                    }
                                }.bind(this));

                        this.$openButton.on("click", function(event) {
                            try {
                                if (!this.pickerShown) {
                                    this._openPicker();
                                } else {
                                    this._hidePicker();
                                }
                                this.keepShown = true;
                                setTimeout(function() {
                                    this.keepShown = false;
                                }.bind(this), 200);
                            } catch (e) {
//                                console.log(e.message);
                            }

                        }.bind(this));
                    }

                },

                _readDataFromMarkup : function() {

                    if (this.$element.data("disabled")) {
                        this.options.disabled = true;
                    }

                    if (this.$element.data("name")) {
                        this.options.name = this.$element.data("name");
                    }
                    
                    if (this.$element.attr("title")) {
                        this.options.title = this.$element.attr("title");
                    }

                    var el = this.$element;
                    if (el.data('config') !== undefined) {
                        this.options.config = {};
                        this.options.config.colors = {};
                        this.options.config.displayModes = {};
                        if (el.data('config').colors) {
                            this.options.config.colors = el.data('config').colors;
                        } else {
                            this.options.disabled = true;
                        }
                        if (el.data('config').pickerModes) {
                            this.options.config.displayModes = el.data('config').pickerModes;
                        }
                    }
                },

                _adjustMarkup : function() {
                    this.$element.addClass("colorpicker");

                    if (this.$element.find(".popover").length === 0) {
                        this.$element
                                .append('<div class="popover arrow-top" style="display:none"><div class="inner"></div></div>');
                        this.$element
                                .find(".inner")
                                .append(
                                        '<div class="colorpicker-holder"><div class="palette-header"></div><div class="colorpicker-body"></div><div class="colorpicker-footer"></div></div>');
                    }

                    if (this.$element.find("input[type=hidden]").length === 0) {
                        this.$element.append("<input type=\"hidden\" name=\"" +
                                this.options.name + "\">");
                    }

                    var $button = this.$element
                            .find('input.colorpicker-launcher');
                    if ($button.attr('type') === undefined) {
                        $button.attr('type', 'button');
                    }
                },

                _openPicker : function() {

                    this._renderPicker(CLASSIC_MODE);
                    //take into consideration the popover border width
                    var left = this.$openButton.position().left +
                            this.$openButton.width() / 2 -
                            (this.$element.find(".popover").width() / 2 + 9);
                    if (left < 0) {
                        left = 0;
                    }
                    var top = this.$openButton.position().top +
                            this.$openButton.outerHeight() + 14;

                    this._renderPickerFooter();

                    this.$element.find(".popover").css({
                        "position" : "absolute",
                        "left" : left + "px",
                        "top" : top + "px"
                    }).show();
                    this.pickerShown = true;
                },

                _hidePicker : function() {
                    this.lowerLimit = 0;
                    this.upperLimit = 0;
                    this.currentPage = 0;
                    this.$element.removeClass("focus");
                    this.$element.find(".popover").hide();
                    this.pickerShown = false;
                },

                //render color picker based on the palette mode
                _renderPicker : function(mode, slide, pageNo) {

                    if (mode === CLASSIC_MODE && !this._calculatePaletteBoundaries(slide, pageNo)) {
                        return;
                    }

                    var table = null;
                    if (mode === CLASSIC_MODE){
                        table = this._renderPalette();
                    }else{
                        table = this._renderEditPalette();
                    }

                    var $picker = this.$element.find(".colorpicker-holder");
                    var $palette_nav = $picker.find(".palette-navigator");
                    var $picker_body = $picker.find(".colorpicker-body");

                    if (slide && $picker.find("table").length > 0) {
                        this._slidePicker(table, (slide === "left"));
                    } else {
                      //display selected color if any and selected page
                        $picker.find("table").remove();
                        $picker.find(".sliding-container").remove();
                        if (mode === EDIT_MODE) {
                            $picker_body.append(table);
                            $palette_nav.remove();
                            if (this.$hiddenInput.val() !== undefined && this.$hiddenInput.val().length > 0){
                                table.find("div.color").css("background", this.$hiddenInput.val());
                                var hex = CUI.util.color.RGBAToHex(this.$hiddenInput.val());
                                table.find("input[name=':hex']").val(hex);
                                var rgb = CUI.util.color.HexToRGB(hex);
                                this._fillRGBFields(rgb);
                                var cmyk = CUI.util.color.RGBtoCMYK(rgb);
                                this._fillCMYKFields(cmyk);
                            }
                        } else {
                            if ($palette_nav.length > 0){
                                $palette_nav.before(table);
                            }else{
                                $picker_body.append(table);
                                this._renderPaletteNavigation();
                            }
                            
                        }
                           
                    }

                },
                //display navigation mode buttons and select the one corresponding to the current display mode
                _renderPickerFooter : function() {
                    this.$element.find(".colorpicker-footer").html(
                            COLOPICKER_FOOTER_TEMPLATE);
                    if (this.options.config.displayModes !== undefined) {
                        if (this.options.config.displayModes.classicPalette ||
                                this.options.config.displayModes.freestylePalette) {
                            var paletteButton = $(CLASSIC_PALETTE_BUTTON);
                            paletteButton.addClass("selected");
                            this.$element.find(".navigation-bar").append(
                                    paletteButton);
                        }
                        if (this.options.config.displayModes.edit) {
                            this.$element.find(".navigation-bar").append(
                                    EDIT_BUTTON);
                        } else {
                            this.$element.find(".colorpicker-footer").remove();
                            return;
                        }
                    }
                    
                    this.$element.find(".colorpicker-footer button").off("tap.button click.button").fipo( "tap.button", "click.button", function(event) {
                                event.stopPropagation();
                                event.preventDefault();
                                var $target = $(event.target);
                                var $button = null;
                                this.$element.find(
                                        ".navigation-bar > .selected")
                                        .removeClass("selected");
                                if (event.target.nodeName === "BUTTON") {
                                    $target.addClass("selected");
                                    $button = $(event.target);
                                } else {
                                    $target.parent().addClass(
                                            "selected");
                                    $button = $target.parent();
                                }
                                if ($button.attr("id") === "editButton"){
                                    this._renderPicker(EDIT_MODE);
                                }else{
                                    this._renderPicker(CLASSIC_MODE, false, this.currentPage);
                                }

                            }.bind(this));
                },
                //function for palette navigation
                _calculatePaletteBoundaries : function(slide, pageNo) {
                    var colorsPerPage = 0;
                    if (this.options.config.displayModes.freestylePalette) {
                        colorsPerPage = this.palettePageSize *
                                this.colorShadeNo;
                    } else {
                        colorsPerPage = this.palettePageSize;
                    }
                    if (!slide) {
                        if (pageNo !== undefined) {
                            this.lowerLimit = colorsPerPage * pageNo;
                            this.upperLimit = this.lowerLimit + colorsPerPage -
                                    1;
                            this.currentPage = pageNo;
                        } else {
                            this.upperLimit += colorsPerPage - 1;
                            this.lowerLimit = 0;
                            this.currentPage = 0;
                        }
                    } else if (slide === "left") {
                        pageNo = this.currentPage + 1;
                        if (pageNo + 1 > this.pages) {
                            return false;
                        }
                        this.lowerLimit = colorsPerPage * pageNo;
                        this.upperLimit = this.lowerLimit + colorsPerPage - 1;
                        this.currentPage = pageNo;
                    } else {
                        pageNo = this.currentPage - 1;
                        if (pageNo < 0) {
                            return false;
                        }
                        this.lowerLimit = colorsPerPage * pageNo;
                        this.upperLimit = this.lowerLimit + colorsPerPage - 1;
                        this.currentPage = pageNo;
                    }
                    return true;
                },
                //display navigation bullets
                _renderPaletteNavigation : function() {
                    this.$element.find(".palette-navigator").remove();
                    var navigator = $("<div>");
                    navigator.addClass("palette-navigator");
                    if (this.options.config.displayModes.classicPalette) {
                        this.pages = Math.ceil(this.colorNames.length /
                                this.palettePageSize);
                    } else {
                        this.pages = Math.ceil(this.colorNames.length /
                                (this.palettePageSize * this.colorShadeNo));
                    }
                    if (this.pages > 1) {
                        for ( var i = 0; i < this.pages; i++) {
                            navigator.append("<i class='dot' page='" + i +
                                    "'></i>");
                        }
                    }
                    this.$element.find(".colorpicker-body").append(navigator);
                    this.$element.find("i[page='" + this.currentPage + "']")
                            .addClass("active");

                    // Move around
                    this.$element.find(".colorpicker-body").on("swipe",
                            function(event) {
                                this._renderPicker(CLASSIC_MODE, event.direction === "left" ? "left" : "right");
                            }.bind(this));
                    this.$element.find(".dot").off("tap.dot click.dot").fipo("tap.dot", "click.dot", function(event) {
                        event.stopPropagation();

                        if (this.currentPage === parseInt($(event.target).attr("page"), 10)) {
                            return;
                        }

                        this._renderPicker(CLASSIC_MODE, false, parseInt($(event.target).attr("page"), 10));
                    }.bind(this));
                },

                _slidePicker : function(newtable, isLeft) {
                    this.$element.find(".sliding-container table").stop(true,
                            true);
                    this.$element.find(".sliding-container").remove();

                    var oldtable = this.$element.find("table");
                    var width = oldtable.width();
                    var height = oldtable.height();

                    var container = $("<div class=\"sliding-container\">");

                    container.css({
                        "display" : "block",
                        "position" : "relative",
                        "width" : width + "px",
                        "height" : height + "px",
                        "overflow" : "hidden"
                    });

                    this.$element.find(".palette-navigator").before(container);
                    container.append(oldtable).append(newtable);
                    oldtable.css({
                        "position" : "absolute",
                        "left" : 0,
                        "top" : 0
                    });
                    oldtable.after(newtable);
                    newtable.css({
                        "position" : "absolute",
                        "left" : (isLeft) ? width : -width,
                        "top" : 0
                    });

                    var speed = 400;

                    oldtable.animate({
                        "left" : (isLeft) ? -width : width
                    }, speed, function() {
                        oldtable.remove();
                    });

                    newtable.animate({
                        "left" : 0
                    }, speed, function() {
                        if (container.parents().length === 0)
                            return; // We already were detached!
                        newtable.css({
                            "position" : "relative",
                            "left" : 0,
                            "top" : 0
                        });
                        newtable.detach();
                        this.$element.find(".palette-navigator").before(
                                newtable);
                        container.remove();
                    }.bind(this));
                },
                //render the selected color name and hex code
                _renderPaletteHeader : function() {
                    var title = $('<div class="palette-header"><div class="title"></div><div class="selection"></div></div>');
                    var $picker = this.$element.find(".colorpicker-holder");
                    if ($picker.find(".palette-header").length > 0) {
                        $picker.find(".palette-header").replaceWith(title);
                    } else {
                        $picker.prepend(title);
                    }
                    $picker.find(".title").html(
                            "<span>" + this.options.title + "</span>");
                },

                _renderPalette : function() {
                    this._renderPaletteHeader();

                    var table = $("<table>");
                    var html = "";

                    for ( var i = 0; i < this.palettePageSize; i++) {
                        html += "<tr>";
                        var opacity = 0;
                        var rgb = "";
                        var cssClass = "";
                        var shade = "";
                        for ( var sh = 0; sh < this.colorShadeNo; sh++) {
                            if (this.options.config.displayModes.classicPalette) {
                              //display colors with shades
                                if (this.colorNames.length - 1 < i +
                                        this.lowerLimit) {
                                    html += "<td><a></a></td>";
                                } else {
                                    rgb = CUI.util.color.HexToRGB(this.options.config.colors[this.colorNames[i +
                                                    this.lowerLimit]]);
                                    shade = "rgba(" + rgb.r + "," + rgb.g +
                                            "," + rgb.b + "," +
                                            (1 - opacity).toFixed(2) + ")";
                                    opacity += 0.16;
                                    if (CUI.util.color.isSameColor(shade,
                                            this.$hiddenInput.val())) {
                                        cssClass = "selected";
                                        this._fillSelectedColor(this.colorNames[i + this.lowerLimit], CUI.util.color.RGBAToHex(shade));
                                    } else {
                                        cssClass = "";
                                    }
                                    html += "<td class='filled'><a style='background-color:" +
                                            shade +
                                            "' color='" +
                                            shade +
                                            "' colorName='" +
                                            this.colorNames[i + this.lowerLimit] +
                                            "' class='" +
                                            cssClass +
                                            "'>" +
                                            "</a></td>";
                                }
                            } else {
                              //display colors without shades (freestyle)
                                if (this.colorNames.length - 1 < i *
                                        this.colorShadeNo + sh) {
                                    html += "<td><a></a></td>";
                                } else {
                                    rgb = CUI.util.color.HexToRGB(this.options.config.colors[this.colorNames[i *
                                                    this.colorShadeNo + sh]]);
                                    shade = "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + 1 + ")";
                                    if (CUI.util.color.isSameColor(shade,
                                            this.$hiddenInput.val())) {
                                        cssClass = "selected";
                                    } else {
                                        cssClass = "";
                                    }
                                    html += "<td class='filled'><a style='background-color:" +
                                            shade +
                                            "' color='" +
                                            shade +
                                            "' colorName='" +
                                            this.colorNames[i *
                                                    this.colorShadeNo + sh] +
                                            "' class='" +
                                            cssClass +
                                            "'>" +
                                            "</a></td>";
                                }
                            }
                        }
                        html += "</tr>";
                    }
                    table.append("<tbody>" + html + "</tbody>");
                    //click on a color box
                    table.find("a").off("tap.a click.a").fipo("tap.a", "click.a", function(event) {
                                        event.stopPropagation();
                                        event.preventDefault();

                                        if (CUI.util.color.isSameColor(this.$hiddenInput
                                                .val(), $(event.target).attr(
                                                "color"))) {
                                            return;
                                        }

                                        this.$element.find("table").find(
                                                ".selected").removeClass(
                                                "selected");
                                        $(event.target).addClass("selected");

                                        var colorName = $(event.target).attr("colorName") !== undefined ? $(event.target).attr("colorName"): "";
                                        this._fillSelectedColor(colorName, CUI.util.color.RGBAToHex($(event.target).attr("color")));

                                        this._setColor($(event.target).attr("color"));
                                    }.bind(this));
                    var $navigator = this.$element.find(".palette-navigator");
                    $navigator.find(".active").removeClass("active");
                    $navigator.find("i[page='" + this.currentPage + "']").addClass("active");

                    return table;
                },
                
                _fillSelectedColor : function(colorName, hexVal){
                    this.$element.find(".colorpicker-holder").find(".selection")
                    .html(
                            "<div><span>" +
                            colorName +
                            "</span><span>" +
                            hexVal +
                                    "</span></div>");  
                },
                //render edit mode screen
                _renderEditPalette : function(){
                    var table = $("<table>");
                    var html = "<tr>" + 
                                //hex color representation
                                    "<td colspan='2' rowspan='2'>" +
                                        "<div class='color'></div>" + 
                                     "</td>" +
                                     "<td class='label'>HEX</td>" + 
                                     "<td colspan='2'>" +
                                         "<input type='text' name=':hex'/>" +
                                     "</td>" + 
                                     "<td colspan='2'>&nbsp;</td>" + 
                                "</tr>" + 
                                //RGB color representation in 3 input fields(r, g,b)
                                "<tr>" + 
                                    "<td class='label'>RGB</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':rgb_r'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':rgb_g'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':rgb_b'/>" +
                                    "</td>" + 
                                    "<td>&nbsp;</td>" + 
                                "</tr>" +
                              //CMYK color representation in 4 input fields(c,m,y,k)
                                "<tr>" + 
                                    "<td colspan='2'>&nbsp;</td>" + 
                                    "<td class='label'>CMYK</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_c'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_m'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_y'/" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_k'/>" +
                                    "</td>" + 
                                "</tr>" +
                                //save button to store the color on the launcher
                                "<tr>" + 
                                    "<td colspan='3'>&nbsp;</td>" + 
                                    "<td colspan='4'>" +
                                        "<button class='primary'>Save Color</button>" +
                                    "</td>" + 
                                "</tr>";

                    table.append("<tbody>" + html + "</tbody>");
                    
                    this.$element.find(".palette-header").remove();
                    //input validations for change events
                    table.find("input[name^=':rgb_']").each(function(index, element){
                        $(element).attr("maxlength", "3");
                        $(element).on("blur", function(event){
                            var rgbRegex = /^([0]|[1-9]\d?|[1]\d{2}|2([0-4]\d|5[0-5]))$/;
                            if (!rgbRegex.test($(event.target).val().trim()) || $("input:text[value=''][name^='rgb_']").length > 0){
                                $(event.target).val("");
                                this._clearCMYKFields();
                                this.$element.find("input[name=':hex']").val("");
                                this.$element.find("div.color").removeAttr("style");
                                return;
                            }
                            var rgb = {r:this.$element.find("input[name=':rgb_r']").val(), g:this.$element.find("input[name=':rgb_g']").val(), b:this.$element.find("input[name=':rgb_b']").val()};
                            var cmyk = CUI.util.color.RGBtoCMYK(rgb);
                            var hex = CUI.util.color.RGBToHex(rgb);
                            this._fillCMYKFields(cmyk);
                            this.$element.find("input[name=':hex']").val(hex);
                            this.$element.find("div.color").css("background", hex);
                        }.bind(this));
                    }.bind(this));
                    table.find("input[name^=':cmyk_']").each(function(index, element){
                        $(element).attr("maxlength", "3");
                        $(element).on("blur", function(event){
                            var cmykRegex = /^[1-9]?[0-9]{1}$|^100$/;
                            if (!cmykRegex.test($(event.target).val().trim()) || $("input:text[value=''][name^='cmyk_']").length > 0){
                                $(event.target).val("");
                                this._clearRGBFields();
                                this.$element.find("input[name=':hex']").val("");
                                this.$element.find("div.color").removeAttr("style");
                                return;
                            }
                            var cmyk = {c:this.$element.find("input[name=':cmyk_c']").val(), m:this.$element.find("input[name=':cmyk_m']").val(), y:this.$element.find("input[name=':cmyk_y']").val(), k:this.$element.find("input[name=':cmyk_k']").val()};
                            var rgb = CUI.util.color.CMYKtoRGB(cmyk);
                            var hex = CUI.util.color.RGBToHex(rgb);
                            this.$element.find("input[name=':hex']").val(hex);
                            this._fillRGBFields(rgb);
                            this.$element.find("div.color").css("background", hex);
                        }.bind(this));
                    }.bind(this));
                    table.find("input[name=':hex']").each(function(index, element){
                        $(element).attr("maxlength", "7");
                        $(element).on("blur", function(event){
                            var hex = CUI.util.color.fixHex($(event.target).val().trim());
                            if (hex.length === 0){
                                this._clearRGBFields();
                                this._clearCMYKFields();
                                this.$element.find("div.color").removeAttr("style");
                                return;
                            }
                            var rgb = CUI.util.color.HexToRGB(hex);
                            var cmyk = CUI.util.color.RGBtoCMYK(rgb);
                            this._fillRGBFields(rgb);
                            this._fillCMYKFields(cmyk);
                            table.find("div.color").css("background", hex);
                        }.bind(this));
                    }.bind(this));
                    
                    table.on("click tap", "input, div",
                            function(event) {
                                event.stopPropagation();
                                event.preventDefault();
                                
                            });
                    table.on("click tap", "button",
                            function(event) {
                                event.stopPropagation();
                                event.preventDefault();
                                if (this.$element.find("input[name=':hex']").val() !== undefined && this.$element.find("input[name=':hex']").val().length > 0){
                                    this._setColor(this.$element.find("input[name=':hex']").val());
                                }
                            }.bind(this));

                    return table;
                },
                //set selected color on the launcher
                _setColor : function(color) {
                    this.$hiddenInput.val(color);
                    this.$openButton.css("background-color", this.$hiddenInput
                            .val());
                },

                _fillRGBFields : function(rgb) {
                    this.$element.find("input[name=':rgb_r']").val(rgb.r);
                    this.$element.find("input[name=':rgb_g']").val(rgb.g);
                    this.$element.find("input[name=':rgb_b']").val(rgb.b);
                },

                _clearRGBFields : function() {
                    this.$element.find("input[name^=':rgb']").val("");
                },
                
                _fillCMYKFields : function(cmyk) {
                    this.$element.find("input[name=':cmyk_c']").val(cmyk.c);
                    this.$element.find("input[name=':cmyk_m']").val(cmyk.m);
                    this.$element.find("input[name=':cmyk_y']").val(cmyk.y);
                    this.$element.find("input[name=':cmyk_k']").val(cmyk.k);
                },
                
                _clearCMYKFields : function() {
                    this.$element.find("input[name^=':cmyk']").val("");
                }
                
            });

    CUI.util.plugClass(CUI.Colorpicker);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
          $("[data-init~=colorpicker]", e.target).colorpicker();
           
        });
    }
}(window.jQuery));

