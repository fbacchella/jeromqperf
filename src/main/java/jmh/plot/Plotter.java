package jmh.plot;

import java.io.IOException;

import org.openjdk.jmh.results.RunResult;

public interface Plotter {

    /**
     * Add a {@link RunResult} to a SVG plot
     * @param rr
     */
    void addResult(RunResult rr);

    /**
     * Draw an plot, using the svn name as a radical for the file name
     * @param svgname
     * @throws IOException
     */
    void drawSvg(String svgname) throws IOException ;

}
