"use strict";
use(function() {
    var REGEX = /(\d+),(\d+)\.(\d+)([NEWS])/,
        itemPath = request.getParameter("item"),
        result = {
            divId : "acs-asset-location-" + new Date().getTime()
        };

    function toDD(parsed) {
        var degrees = Number.parseInt(parsed[1], 10),
            minutes = Number.parseInt(parsed[2], 10),
            seconds = Number.parseInt(parsed[3], 10),
            dd = degrees + (minutes/60) + (seconds/3600);

        if (parsed[4] === "S" || parsed[4] === "W") {
            dd = -dd;
        }
        return dd;
    }

    if (itemPath) {
        var itemResource = request.resourceResolver.getResource(itemPath),
			asset = itemResource.adaptTo(com.day.cq.dam.api.Asset),
            metadata = asset.getMetadata(),
            latitudeParsed = REGEX.exec(metadata['exif:GPSLatitude']),
            longitudeParsed = REGEX.exec(metadata['exif:GPSLongitude']);

        if (latitudeParsed && longitudeParsed) {
            result.valid = true;
            result.latitude = toDD(latitudeParsed);
            result.longitude = toDD(longitudeParsed);
        }
    }
    return result;
});