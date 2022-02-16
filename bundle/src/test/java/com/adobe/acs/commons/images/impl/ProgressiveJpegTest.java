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
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class ProgressiveJpegTest {

    private BufferedImage simpleImage;

    @Before
    public void setUp() throws Exception {
        simpleImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = simpleImage.createGraphics();
        graphics.setPaint(new Color(50, 100, 150, 200));
        graphics.fillRect(0, 0, simpleImage.getWidth(), simpleImage.getHeight());
    }

    @Test
    public void testWrite_sameAsLayerWrite() throws Exception {
        BufferedImage expectedImage = createExpectedImage(simpleImage);
        BufferedImage actualImage = createActualImage(simpleImage);

        assertArrayEquals(getRGB(expectedImage), getRGB(actualImage));
    }

    private static byte[] writeProgressiveImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Layer layer = new Layer(image);
        ProgressiveJpeg.write(layer, 1, byteOut);
        return byteOut.toByteArray();
    }

    private int[] getRGB(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static BufferedImage createExpectedImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Layer layer = new Layer(image);
        layer.write("image/jpeg", 1, byteOut);
        return ImageIO.read(new ByteArrayInputStream(byteOut.toByteArray()));
    }

    private static BufferedImage createActualImage(BufferedImage image) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(writeProgressiveImage(image)));
    }

}
