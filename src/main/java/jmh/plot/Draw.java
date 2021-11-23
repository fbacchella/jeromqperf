package jmh.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Draw {
    
    private Draw() {

    }

    static final int OVERALLSCALE = 10;
    static final int BPHEIGHT = OVERALLSCALE * 5;
    private static final DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

    public static void generate(String filename, double chartMin, double chartMax, int interval, double scale, String unitBefore, String unitAfter, BoxPlot... boxPlots) throws IOException {

        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        SVGGraphics svgGenerator = new SVGGraphics(document);

        try {
            // The height of the box draw area
            double dheight = (((boxPlots.length * 1.5) + 0.5) * BPHEIGHT);
            int height = (int) dheight;
            // The width for the text
            int xoffset = BPHEIGHT * 2;
            int maxwidth = (int) (1.0 * xoffset + (chartMax + interval) * scale);
            int maxheight = (int) (dheight + 5*scale + BPHEIGHT);
//            System.out.format("dheight=%s height=%s xoffset=%s maxwidth=%s maxheight=%s%n",
//                    dheight, height, xoffset, maxwidth, maxheight);
            svgGenerator.setSVGCanvasSize(new Dimension(maxwidth, maxheight));
            // grid
            svgGenerator.setPaint(Color.LIGHT_GRAY);
            double chartLinePos = 1.0 * xoffset + ((interval/5.0))*scale;
            while (chartLinePos < maxwidth) {
                svgGenerator.drawLine((int) chartLinePos, 0, (int) chartLinePos, height);
                chartLinePos += ((interval/5.0) * scale);
            }

            svgGenerator.setPaint(Color.BLACK);
            // y axis
            svgGenerator.drawLine(xoffset, 0, xoffset, height);
            // y axis
            svgGenerator.drawLine(xoffset, height, maxwidth, height);
            // x axis labels
            for (int i = 0; i <= chartMax; i += interval) {
                anchoredText(svgGenerator, unitBefore + i + unitAfter,(int) (xoffset + (i * scale)), height + (int) (BPHEIGHT * 0.25), "middle");
            }

            for (int i = 0; i < boxPlots.length; i++) {
                // label
                anchoredText(svgGenerator, boxPlots[i].name, xoffset - OVERALLSCALE, (int) (((i * 1.5) + 1) * BPHEIGHT), "end");
                // draw
                boxPlots[i].paint(svgGenerator, scale, xoffset, (int) (((i * 1.5) + 0.5) * BPHEIGHT));
            }
            try (Writer svgOut = Files.newBufferedWriter(Paths.get(filename), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                svgGenerator.stream(svgOut, true);
            }
        } finally {
            svgGenerator.dispose();
        }
    }

    public static void anchoredText(SVGGraphics svgGenerator, String string, int x, int y, String textAnchor) {
        Element text = svgGenerator.getDOMFactory().createElementNS(SVGConstants.SVG_NAMESPACE_URI, SVGConstants.SVG_TEXT_TAG);
        text.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, svgGenerator.getGeneratorContext().doubleString(x));
        text.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, svgGenerator.getGeneratorContext().doubleString(y));
        //center text
        text.setAttributeNS(null, "text-anchor", textAnchor);

        text.setAttributeNS(SVGConstants.XML_NAMESPACE_URI,
                            SVGConstants.XML_SPACE_QNAME,
                            SVGConstants.XML_PRESERVE_VALUE);
        text.appendChild(svgGenerator.getDOMFactory().createTextNode(string));
        svgGenerator.domGroupManager().addElement(text, DOMGroupManager.FILL);
    }

}
