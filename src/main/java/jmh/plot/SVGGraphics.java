package jmh.plot;

import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.ExtensionHandler;
import org.apache.batik.svggen.ImageHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;

class SVGGraphics extends SVGGraphics2D {

    SVGGraphics(Document domFactory) {
        super(domFactory);
    }

    SVGGraphics(Document domFactory, ImageHandler imageHandler, ExtensionHandler extensionHandler, boolean textAsShapes) {
        super(domFactory, imageHandler, extensionHandler, textAsShapes);
    }

    SVGGraphics(SVGGeneratorContext generatorCtx, boolean textAsShapes) {
        super(generatorCtx, textAsShapes);
    }

    SVGGraphics(SVGGraphics2D g) {
        super(g);
    }

    DOMGroupManager domGroupManager() {
        return domGroupManager;
    }

}
