package apps.acs_002dcommons.components.content.image;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import com.day.cq.commons.ImageResource;
import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;

/**
 * Image render servlet to process the required image resource.
 */

public class img_GET extends AbstractImageServlet {

    private static final long serialVersionUID = 2954340003021517742L;
    private static final int MAX_HEIGHT = 1024;
    private static final int MAX_WIDTH = 1024;
    private static final int MIN_SELECTOR_LENGTH = 3;

    protected final Layer createLayer(ImageContext c)
    throws RepositoryException, IOException {
        // don't create the later yet. handle everything later
        return null;
    }

    @Override
    protected final ImageResource createImageResource(Resource resource) {
        return new Image(resource);
    }

    protected final void writeLayer(SlingHttpServletRequest req, SlingHttpServletResponse resp, ImageContext c,
                                     Layer layer) throws IOException, RepositoryException {
        boolean modified = false;
        double selectorWidth = 0.0, selectorHeight = 0.0;

        Image image = new Image(c.resource);

        if (!image.hasContent()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String[] selectors = null;
        if (req.getRequestPathInfo().getSelectors() != null) {
            selectors = req.getRequestPathInfo().getSelectors();
            if (selectors.length >= MIN_SELECTOR_LENGTH) {
                selectorWidth = Integer.parseInt(selectors[1]);
                selectorHeight = Integer.parseInt(selectors[2]);

            }

            if (selectorWidth > MAX_WIDTH || selectorHeight > MAX_HEIGHT) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("The sclaed size is too big.");
                return;
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("NO selector found.");
            return;
        }

        image.loadStyleData(c.style);

        layer = image.getLayer(false, false, false);

        if (layer == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Can not get Layer.");
            return;
        }

        if (layer != null) {
            if (selectors.length < MIN_SELECTOR_LENGTH) {
                selectorWidth = layer.getWidth();
                selectorHeight = layer.getHeight();
            }

            modified = image.crop(layer) != null;

            modified |= image.rotate(layer) != null;

            modified |= image.resize(layer) != null;

            modified |= applyDiff(layer, c);

            modified |= autoResizeImage(selectorWidth, selectorHeight, layer) != null;
       }

      if (modified) {
          resp.setContentType("image/png");
          layer.write("image/png", 1.0, resp.getOutputStream());
      } else {
          Property data = image.getData();
          InputStream in = data.getStream();
          resp.setContentLength((int) data.getLength());
          resp.setContentType(image.getMimeType());
          IOUtils.copy(in, resp.getOutputStream());
          in.close();
      }
        resp.flushBuffer();
    }

    private Layer autoResizeImage(double selectorWidth, double selectorHeight, Layer layer) {
        double actualImageWidth = layer.getWidth();
        double actualImageHeight = layer.getHeight();

        double widthNum = (selectorWidth / actualImageWidth);
        double heigthNum = (selectorHeight / actualImageHeight);

        double resizedWidth = actualImageWidth;
        double resizedHeight = actualImageHeight;

        resizedWidth = actualImageWidth * ((heigthNum > widthNum) ? heigthNum : widthNum);
        resizedHeight = actualImageHeight * ((heigthNum > widthNum) ? heigthNum : widthNum);

        layer.resize((int) resizedWidth, (int) resizedHeight);

        selectorWidth = (int) selectorWidth == 0 ? resizedWidth : selectorWidth;
        selectorHeight = (int) selectorHeight == 0 ? resizedHeight : selectorHeight;

        int diffHeight = (int) (resizedHeight - selectorHeight);
        int newHeight = (int) (resizedHeight - diffHeight);

        int diffWidth = (int) (resizedWidth - selectorWidth);
        int newWidth = (int) (resizedWidth - diffWidth);

        layer.crop(new Rectangle(diffWidth / 2, diffHeight / 2, newWidth, newHeight));

        return layer;
    }
}