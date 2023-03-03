/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package ch.randelshofer.media.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;


/**
 * A reader for JPEG images in the CMYK color space.
 *
 * @author Werner Randelshofer
 * @version 1.2 2011-02-17 Removes support for JMF.
 * <br>1.0 2010-07-23 Created.
 */
public class CMYKJPEGImageReaderSpi extends ImageReaderSpi {

    public CMYKJPEGImageReaderSpi() {
        super("Werner Randelshofer",//vendor name
                "1.0",//version
                new String[]{"JPEG","JPG"},//names
                new String[]{"jpg"},//suffixes,
                new String[]{"image/jpg"},// MIMETypes,
                "org.monte.media.jpeg.CMYKJPEGImageReader",// readerClassName,
                new Class[]{ImageInputStream.class,InputStream.class,byte[].class},// inputTypes,
                null,// writerSpiNames,
                false,// supportsStandardStreamMetadataFormat,
                null,// nativeStreamMetadataFormatName,
                null,// nativeStreamMetadataFormatClassName,
                null,// extraStreamMetadataFormatNames,
                null,// extraStreamMetadataFormatClassNames,
                false,// supportsStandardImageMetadataFormat,
                null,// nativeImageMetadataFormatName,
                null,// nativeImageMetadataFormatClassName,
                null,// extraImageMetadataFormatNames,
                null// extraImageMetadataFormatClassNames
                );
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        if (source instanceof ImageInputStream) {
            ImageInputStream in = (ImageInputStream) source;
            in.mark();
            // Check if file starts with a JFIF SOI magic (0xffd8=-40)
            if (in.readShort() != -40) {
                in.reset();
                return false;
            }
            in.reset();
            return true;
        }
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new CMYKJPEGImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "CMYK JPEG Image Reader";
    }
}
