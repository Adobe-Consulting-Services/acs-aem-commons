/*
 * #%L
 * ACS AEM Commons Bundle
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
package com.adobe.acs.commons.images.impl;

import com.day.cq.commons.PathInfo;
import org.apache.commons.lang.StringUtils;

/**
 * User: david
 */
public class QuickImageHelperImpl {
    public static final String KEY_WIDTH = "width";

    public static final String KEY_HEIGHT = "height";

    public static final String KEY_ROTATE = "rotate";

    public static final String KEY_CROP = "crop";

    public static final String KEY_QUICK = "quick";

    /**
     * Convenience wrapper for getSrc(..) supporting only rotation
     *
     * @param path to image resource to rendition
     * @param rotation degrees to rotate image
     * @return a well-formed absolute QuickImage URI
     */
    public static String getSrc(String path, int rotation) {
        return getSrc(path, 0, 0, rotation, 0, 0, 0, 0);
    }

    /**
     * Convenience wrapper for getSrc(..) supporting only width and/or height resizing
     *
     * @param path to image resource to rendition
     * @param width 0 will set width to proportionately scale
     * @param height 0 will set height to proportionately scale
     * @return a well-formed absolute QuickImage URI
     */
    public static String getSrc(String path, int width, int height) {
        return getSrc(path, width, height, 0, 0, 0, 0, 0);
    }

    /**
     * Convenience wrapper for getSrc(..) supporting only cropping
     *
     * @param path to image resource to rendition
     * @param cropX top left x-coordinate to begin drawing the crop rectangle
     * @param cropY top left y-coordinate to begin drawing the crop rectangle
     * @param cropWidth crop rectangle width starting from cropX,cropY
     * @param cropHeight crop rectangle height starting from cropX,cropY
     * @return a well-formed absolute QuickImage URI
     */
    public static String getSrc(String path, int cropX, int cropY, int cropWidth, int cropHeight) {
        return getSrc(path, 0, 0, 0, cropX, cropY, cropWidth, cropHeight);
    }

    /**
     * Generates a QuickImage URI based on provided params
     *
     * @param path to image resource to rendition
     * @param width 0 will set width to proportionately scale
     * @param height 0 will set height to proportionately scale
     * @param rotation degrees to rotate image
     * @param cropX top left x-coordinate to begin drawing the crop rectangle
     * @param cropY top left y-coordinate to begin drawing the crop rectangle
     * @param cropWidth crop rectangle width starting from cropX,cropY
     * @param cropHeight crop rectangle height starting from cropX,cropY
     * @return a well-formed absolute QuickImage URI
     */
    public static String getSrc(String path, int width, int height, int rotation, int cropX, int cropY, int cropWidth, int cropHeight) {
        final PathInfo pathInfo = new PathInfo(path);
        final String extension = pathInfo.getExtension();

        String src = path + ".quickimg";

        src += "/" + KEY_QUICK;

        if(width > 0) {
            src += "/" + KEY_WIDTH + "/" + width;
        }

        if(height > 0) {
            src += "/" + KEY_HEIGHT + "/" + height;
        }

        if(rotation != 0) {
            src += "/" + KEY_ROTATE + "/" + rotation;
        }

        if(cropX > 0 && cropY > 0 && cropWidth > 0 && cropHeight > 0) {
            src += "/" + KEY_CROP;
            src += "/" + cropX;
            src += "/" + cropY;
            src += "/" + cropWidth;
            src += "/" + cropHeight;
        }

        src += "/image.";

        if(StringUtils.isBlank(extension)) {
            src += "png";
        } else {
            src += extension;
        }

        return src;
    }
}
