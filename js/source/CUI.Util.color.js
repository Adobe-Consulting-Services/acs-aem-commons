(function ($, window, undefined) {
/**
 HTTP Utility functions used by CoralUI colorpicker for color transformation

 @namespace
 */
CUI.util.color = {
    
    /**
     * Transforms a string color or part of color (r,g,b) into a hexa value
     * @static
     * @param {String} x The string color or part of color
     * @return {String} Hexa representation
     */
    hex : function (x) {
        return ("0" + parseInt(x, 10).toString(16)).slice(-2);
    },
    
    /**
     * Transforms a hexa color into RGB representation
     * @static
     * @param {String} hex The string color hexa representation
     * @return {Object} {r, g, b}
     */
    HexToRGB : function(hex) {
        hex = parseInt(((hex.indexOf("#") > -1) ? hex.substring(1) : hex), 16);
        return {
            r : hex >> 16,
            g : (hex & 0x00FF00) >> 8,
            b : (hex & 0x0000FF)
        };
    },
    
    /**
     * Transforms a rgba color into RGB representation
     * @static
     * @param {String} hex The string color rgba representation
     * @return {String} Hexa representation of the color
     */
    RGBAToHex : function(rgbaVal) {
        var rgba = rgbaVal.substring(rgbaVal.indexOf('(') + 1, rgbaVal.lastIndexOf(')')).split(/,\s*/);
        return '#' + this.hex(rgba[0]) + this.hex(rgba[1]) + this.hex(rgba[2]);
    },
    
    /**
     * Transforms a rgb color into hexa representation
     * @static
     * @param {Object} {r, g, b}
     * @return {String} The string color hexa representation
     */
    RGBToHex : function(rgb) {
        return '#' + this.hex(rgb.r) + this.hex(rgb.g) + this.hex(rgb.b);
    },
    
    /**
     * Transforms a cmyk color into RGB representation
     * @static
     * @param {Object} {c, m, y, k}
     * @return {Object} {r, g, b}
     */
    CMYKtoRGB : function (cmyk){
        var result = {r:0, g:0, b:0};
 
        var c = parseInt(cmyk.c, 10) / 100;
        var m = parseInt(cmyk.m, 10) / 100;
        var y = parseInt(cmyk.y, 10) / 100;
        var k = parseInt(cmyk.k, 10) / 100;
 
        result.r = 1 - Math.min( 1, c * ( 1 - k ) + k );
        result.g = 1 - Math.min( 1, m * ( 1 - k ) + k );
        result.b = 1 - Math.min( 1, y * ( 1 - k ) + k );
 
        result.r = Math.round( result.r * 255 );
        result.g = Math.round( result.g * 255 );
        result.b = Math.round( result.b * 255 );
 
        return result;
    },
 
    /**
     * Transforms a rgb color into cmyk representation
     * @static
     * @param {Object} {r, g, b}
     * @return {Object} {c, m, y, k}
     */
    RGBtoCMYK : function (rgb){
        var result = {c:0, m:0, y:0, k:0};
        
        if (parseInt(rgb.r, 10) === 0 && parseInt(rgb.g, 10) === 0 && parseInt(rgb.b, 10) === 0) {
            result.k = 100;
            return result;
        }
 
        var r = parseInt(rgb.r, 10) / 255;
        var g = parseInt(rgb.g, 10) / 255;
        var b = parseInt(rgb.b, 10) / 255;
 
        result.k = Math.min( 1 - r, 1 - g, 1 - b );
        result.c = ( 1 - r - result.k ) / ( 1 - result.k );
        result.m = ( 1 - g - result.k ) / ( 1 - result.k );
        result.y = ( 1 - b - result.k ) / ( 1 - result.k );
 
        result.c = Math.round( result.c * 100 );
        result.m = Math.round( result.m * 100 );
        result.y = Math.round( result.y * 100 );
        result.k = Math.round( result.k * 100 );
 
        return result;
    },
    
    /**
     * Corrects a hexa value, if it is represented by 3 or 6 characters with or without '#'
     * @static
     * @param {String} hex The string representation of the hexa value
     * @return {String} Hexa corrected string or empty string if tghe hex value is not valid
     */
    fixHex : function(hex) {
        if (hex.length === 3) {
            hex = hex.charAt(0) + hex.charAt(0) + hex.charAt(1) +
                    hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.indexOf("#") === -1) {
            hex = "#" + hex;
        }
        var isOk = /(^#[0-9A-F]{6})|(^#[0-9A-F]{3})$/i.test(hex);
        if (!isOk) {
            this.$element.find("[name=':hex']").val("");
            return "";
        }

        return hex;
    },
    
    /**
     * Compares string representations of 2 colors
     * @static
     * @param {String} c1 The string representation of the first color
     * @param {String} c2 The string representation of the seccond color
     * @return {Boolean} True if they are equal, false otherwise
     */
    isSameColor : function(c1, c2) {
        return c1 && c2 && (c1 === c2);
    },
    
    isValid : function(colorFormat, colorAsStr){
        if(colorAsStr.indexOf(colorFormat) != -1){
            return this.fixHex(this.RGBAToHex(colorAsStr)) !== "";
        }
        return false;
    }

};

}(jQuery, this));