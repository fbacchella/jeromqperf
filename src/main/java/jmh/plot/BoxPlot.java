package jmh.plot;

import java.awt.Graphics2D;

public class BoxPlot {

    public String name;
    public double[] outliers;
    public double q1;
    public double q2;
    public double q3;
    public double low;
    public double high;

    public BoxPlot(String s) {
        this.name = s;
    }

    public void paint(Graphics2D g2d, double scale, int xoffset, int yoffset) {
        // q1 line
        g2d.drawLine(xoffset + (int) (q1 * scale), yoffset, xoffset + (int) (q1 * scale), yoffset + Draw.BPHEIGHT);
        // q2 line
        g2d.drawLine(xoffset + (int) (q2 * scale), yoffset, xoffset + (int) (q2 * scale), yoffset + Draw.BPHEIGHT);
        // q3 line
        g2d.drawLine(xoffset + (int) (q3 * scale), yoffset, xoffset + (int) (q3 * scale), yoffset + Draw.BPHEIGHT);
        // top
        g2d.drawLine(xoffset + (int) (q1 * scale), yoffset, xoffset + (int) (q3 * scale), yoffset);
        // bottom
        g2d.drawLine(xoffset + (int) (q1 * scale), yoffset + Draw.BPHEIGHT, xoffset + (int) (q3 * scale), yoffset + Draw.BPHEIGHT);

        // left
        g2d.drawLine(xoffset + (int) (low * scale), (int) (Draw.BPHEIGHT * 0.25) + yoffset, xoffset + (int) (low * scale), (int) (Draw.BPHEIGHT * 0.75) + yoffset);
        // left line
        g2d.drawLine(xoffset + (int) (low * scale), (int) (Draw.BPHEIGHT * 0.5) + yoffset, xoffset + (int) (q1 * scale), (int) (Draw.BPHEIGHT * 0.5) + yoffset);
        // right
        g2d.drawLine(xoffset + (int) (high * scale), (int) (Draw.BPHEIGHT * 0.25) + yoffset, xoffset + (int) (high * scale), (int) (Draw.BPHEIGHT * 0.75) + yoffset);
        // right line
        g2d.drawLine(xoffset + (int) (q3 * scale), (int) (Draw.BPHEIGHT * 0.5) + yoffset, xoffset + (int) (high * scale), (int) (Draw.BPHEIGHT * 0.5) + yoffset);

        // outliers
        for (double outlier : outliers) {
            g2d.drawOval(xoffset + (int) (outlier * scale), (int) (Draw.BPHEIGHT * 0.5) + yoffset - 1, 2, 2);
        }
    }
}
