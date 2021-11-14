package jmh.plot;

import java.io.IOException;

import org.openjdk.jmh.results.RunResult;

public interface Plotter {

    void addResult(RunResult rr);

    void drawSvg(String svgname) throws IOException ;

}
