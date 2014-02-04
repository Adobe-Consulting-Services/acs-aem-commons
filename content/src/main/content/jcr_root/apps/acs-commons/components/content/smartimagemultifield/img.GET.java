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
package apps.acs_002dcommons.components.content.smartimagemultifield;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.servlet.http.HttpServletResponse;

import com.day.cq.wcm.foundation.Image;
import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.image.Layer;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

/**
 * Servlet to return image binary data for multifield smart images. Expects resource path
 * like /content/multi-image/_jcr_content/par/smartimagemultifield.img.png/image-3
 */
public class img_GET extends AbstractImageServlet {

    protected Layer createLayer(ImageContext c)
            throws RepositoryException, IOException {
        return null;
    }

    protected void writeLayer(SlingHttpServletRequest req, SlingHttpServletResponse resp, ImageContext c, Layer layer)
            throws IOException, RepositoryException {
        Iterator<Resource> children = c.resource.listChildren();

        if(!children.hasNext()){
            return;
        }

        String rUri = req.getRequestURI();
        String selImage = rUri.substring(rUri.lastIndexOf("/"));
        Resource resource = req.getResourceResolver().getResource(children.next().getPath() + selImage);

        if(resource == null){
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Image image = new Image(resource);
        image.setItemName(Image.NN_FILE, "image");
        image.setItemName(Image.PN_REFERENCE, "imageReference");

        if (!image.hasContent()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        image.set(Image.PN_MIN_WIDTH, c.properties.get("minWidth", ""));
        image.set(Image.PN_MIN_HEIGHT, c.properties.get("minHeight", ""));
        image.set(Image.PN_MAX_WIDTH, c.properties.get("maxWidth", ""));
        image.set(Image.PN_MAX_HEIGHT, c.properties.get("maxHeight", ""));

        layer = image.getLayer(false, false, false);

        boolean modified = image.crop(layer) != null;

        modified |= image.resize(layer) != null;

        modified |= image.rotate(layer) != null;

        if (modified) {
            resp.setContentType(c.requestImageType);
            layer.write(c.requestImageType, 1.0, resp.getOutputStream());
        } else {
            Property data = image.getData();
            InputStream in = data.getStream();
            resp.setContentLength((int) data.getLength());
            String contentType = image.getMimeType();

            if (contentType.equals("application/octet-stream")) {
                contentType=c.requestImageType;
            }

            resp.setContentType(contentType);
            IOUtils.copy(in, resp.getOutputStream());

            in.close();
        }

        resp.flushBuffer();
    }
}