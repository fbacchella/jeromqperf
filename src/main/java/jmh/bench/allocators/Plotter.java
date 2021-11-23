package jmh.bench.allocators;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.util.Statistics;

import jmh.plot.BoxPlot;
import jmh.plot.Draw;

public class Plotter implements jmh.plot.Plotter {

    private final Map<String, Map<String, RunResult>> resultsMap = new HashMap<>();

    public Plotter() {
    }

    @Override
    public void addResult(RunResult rr) {
        BenchmarkParams params = rr.getParams();
        String allocator = params.getParam("allocator");
        String msgSize = params.getParam("msgSize");
        resultsMap.computeIfAbsent(msgSize, k -> new TreeMap<>(nodeComparator)).put(allocator, rr);
    }

    public void drawSvg(String svgName) throws IOException {
        for (Map.Entry<String, Map<String, RunResult>> m: resultsMap.entrySet()) {
            BoxPlot[] plots = new BoxPlot[m.getValue().size()];
            int bpRank=0;
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (Map.Entry<String, RunResult> e: m.getValue().entrySet()) {
                BoxPlot bp = makeBoxPlot(e.getKey(), e.getValue());
                System.out.format("%s %s %s %s %s %s %s %n", m.getKey(), e.getKey(), bp.low, bp.q1, bp.q2, bp.q3, bp.high);
                plots[bpRank++] = bp;
                max = Math.max(max, bp.high);
                min = Math.min(min, bp.low);
            }
            draw(svgName, m.getKey(), max, plots);
        }
    }

    private BoxPlot makeBoxPlot(String name, RunResult rr) {
        Statistics stats = rr.getPrimaryResult().getStatistics();
        BoxPlot bp = new BoxPlot(name);
        bp.q1 = stats.getPercentile(25);
        bp.q2 = stats.getPercentile(50);
        bp.q3 = stats.getPercentile(75);
        bp.low = stats.getMin();
        // Useful only on throughput mode
        bp.high = stats.getMax();
        bp.outliers = new double[]{};
        return bp;
    }

    private void draw(String svgName, String runName, double max, BoxPlot... plots) throws IOException {
        svgName = svgName.replace(".svg", "");
        Draw.generate(svgName + "-" + runName + ".svg", 0, max, (int)Math.pow(10,Math.ceil(Math.log10(max/10))), 1000.0/max, "", " op/s", plots);
    }

    public static final Comparator<String> nodeComparator = (firstString, secondString) -> {
        if (secondString == null || firstString == null) {
            return 0;
        }

        firstString = firstString.toLowerCase();
        secondString = secondString.toLowerCase();

        int lengthFirstStr = firstString.length();
        int lengthSecondStr = secondString.length();

        int index1 = 0;
        int index2 = 0;

        CharBuffer space1 = CharBuffer.allocate(lengthFirstStr);
        CharBuffer space2 = CharBuffer.allocate(lengthSecondStr);

        while (index1 < lengthFirstStr && index2 < lengthSecondStr) {
            space1.clear();
            space2.clear();

            char ch1 = firstString.charAt(index1);
            boolean isDigit1 = Character.isDigit(ch1);
            char ch2 = secondString.charAt(index2);
            boolean isDigit2 = Character.isDigit(ch2);

            do {
                space1.append(ch1);
                index1++;

                if(index1 < lengthFirstStr) {
                    ch1 = firstString.charAt(index1);
                } else {
                    break;
                }
            } while (Character.isDigit(ch1) == isDigit1);

            do {
                space2.append(ch2);
                index2++;

                if(index2 < lengthSecondStr) {
                    ch2 = secondString.charAt(index2);
                } else {
                    break;
                }
            } while (Character.isDigit(ch2) == isDigit2);

            String str1 = space1.flip().toString();
            String str2 = space2.flip().toString();

            int result;

            if (isDigit1 && isDigit2) {
                try {
                    long firstNumberToCompare = Long.parseLong(str1);
                    long secondNumberToCompare = Long.parseLong(str2);
                    result = Long.compare(firstNumberToCompare, secondNumberToCompare);
                } catch (NumberFormatException e) {
                    // Something prevent the number parsing, do a string
                    // comparaison
                    result = str1.compareTo(str2);
                }
            } else {
                result = str1.compareTo(str2);
            }
            if (result != 0) {
                return result;
            }
        }
        return lengthFirstStr - lengthSecondStr;
    };

}
