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

import com.day.image.Layer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Extension for {@link Layer} with progressive JPEG support.
 */
public class ProgressiveJpeg {

    private ProgressiveJpeg() {}

    /**
     * For JPEG images, this method behaves similar to {@link Layer#write(String, double, OutputStream)}. The major
     * difference is that it uses progressive encoding.
     *
     * @param layer the layer with the image to write to the output stream
     * @param quality JPEG compression quality between 0 and 1
     * @param out target output stream
     * @throws IOException if anything goes wrong
     */
    public static void write(Layer layer, double quality, OutputStream out) throws IOException {
        ImageWriter writer = null;
        ImageOutputStream imageOut = null;
        try {
            ImageWriteParam iwp = new JPEGImageWriteParam(null);
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            iwp.setCompressionQuality((float) quality);

            writer = ImageIO.getImageWritersBySuffix("jpeg").next();

            imageOut = ImageIO.createImageOutputStream(out);
            writer.setOutput(imageOut);

            BufferedImage image = getRgbImage(layer);
            writer.write(null, new IIOImage(image, null, null), iwp);
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            if (imageOut != null) {
                try {
                    imageOut.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Ensures that the image has the right color mode. Without, the image might be colored wrongly.
     */
    private static BufferedImage getRgbImage(Layer layer) {
        final BufferedImage image = layer.getImage();
        final BufferedImage rgbImage;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            new ColorConvertOp(null).filter(image, rgbImage);
        } else {
            rgbImage = image;
        }
        return rgbImage;
    }

}
