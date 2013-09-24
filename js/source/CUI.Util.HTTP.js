/**
 HTTP Utility functions used by CoralUI widgets

 @namespace
 */
CUI.util.HTTP = {
    /**
     * Checks whether the specified status code is OK.
     * @static
     * @param {Number} status The status code
     * @return {Boolean} True if the status is OK, else false
     */
    isOkStatus: function(status) {
        try {
            return (String(status).indexOf("2") === 0);
        } catch (e) {
            return false;
        }
    },

    /**
     * Returns <code>true</code> if HTML5 Upload is supported
     * @return {Boolean} HTML5 Upload support status
     */
    html5UploadSupported: function() {
        var xhr = new XMLHttpRequest();
        return !! (
            xhr && ('upload' in xhr) && ('onprogress' in xhr.upload)
        );
    }

};
