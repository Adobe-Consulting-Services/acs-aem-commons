/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
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

/*global use: false, request: false */
use(function() {
    "use strict";
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