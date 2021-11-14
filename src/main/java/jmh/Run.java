package jmh;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Statistics;

import jmh.perf.RmiProvider;
import jmh.plot.BoxPlot;
import jmh.plot.Draw;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Run {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        System.setProperty("java.awt.headless","true");

        OptionParser parser = new OptionParser();
        parser.acceptsAll(Set.of("f", "fast"), "Fast run for tests");
        parser.acceptsAll(Set.of("h", "help"), "Shows help");
        OptionSpec<Integer> rmiOption     = parser.acceptsAll(Set.of("r", "withRMI"), "Start a RMI server")
                                                  .withOptionalArg().ofType(Integer.class);
        OptionSpec<String> svgNameOption  = parser.acceptsAll(Set.of("s", "svg"), "Generate an SVG of the given name")
                                                  .withOptionalArg().ofType(String.class);
        OptionSpec<String> logfileOption  = parser.acceptsAll(Set.of("l", "log"), "Generate a output log file")
                                                  .withOptionalArg().ofType(String.class);
        OptionSpec<String> jsonfileOption = parser.acceptsAll(Set.of("j", "json"), "Generate a json file with results")
                                                  .withOptionalArg().ofType(String.class);
        OptionSpec<String> profilerOption = parser.acceptsAll(Set.of("p", "profiler"), "Add the given class name as a profiler")
                                                   .withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(args);
        options.nonOptionArguments();

        if (options.has("h")) {
            parser.printHelpOn(System.out);
            System.exit(0);
        }

        if (options.has(rmiOption)) {
            RmiProvider.start();
        }

        long warmupTime;
        int warmupIterations;
        long measurementTime;
        int measurementIterations;
        int forks;
        int msgSend;
        if (options.has("f")) {
            warmupTime = 1;
            warmupIterations = 1;
            measurementTime = 1;
            measurementIterations = 5;
            forks = 1;
            msgSend = 2;
        } else {
            warmupTime = 1;
            warmupIterations = 10;
            measurementTime = 60;
            measurementIterations = 20;
            forks = 1;
            msgSend = 1000;
        }

        ChainedOptionsBuilder builder = new OptionsBuilder()
                .shouldFailOnError(false)
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(warmupTime))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(measurementTime))
                .threads(1)
                .operationsPerInvocation(1)
                .shouldDoGC(true)
                .param("msgSend", Integer.toString(msgSend))
                .forks(forks);
        
//
//        if (tests.size() == 0) {
//            builder.include("jmh.perf\\.*");
//        } else {
//            tests.stream().map(t -> "jmh.perf." + t).forEach(builder::include);
//        }
//
        if (options.has(logfileOption)) {
            String logfile = options.valueOf(logfileOption);
            builder.output(logfile);
        }

        if (options.has(jsonfileOption)) {
            String jsonfile = options.valueOf(jsonfileOption);
            builder.result(jsonfile);
            builder.resultFormat(ResultFormatType.JSON);
        }

        if (options.has("f")) {
            //builder.param("allocator", "heap", "direct")
            //       .param("msgSize", "1", "10");
//          builder.param("allocator", "nettyHeap", "heap")
//          .param("msgSend", "1000")
//          .param("msgSize", "10000000");
        } else {
            builder.addProfiler(GCProfiler.class);
        }

        if (options.has(profilerOption)) {
            String profilerName = options.valueOf(profilerOption);
            try {
                @SuppressWarnings("unchecked")
                Class<Profiler> profilerClass = (Class<Profiler>) Run.class.getClassLoader().loadClass("org.openjdk.jmh.profile." + profilerName);
                builder.addProfiler(profilerClass);
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new IllegalArgumentException("Invalid profiler name " + profilerName + ": " + e.getMessage());
            }
        }

        Options opt = builder.build();

        Collection<RunResult> results = new Runner(opt).run();

        if (options.has(svgNameOption)) {
            String svgName = options.valueOf(svgNameOption);
            Map<String, Map<String, RunResult>> resultsMap = new HashMap<>();
            for (RunResult rr: results) {
                BenchmarkParams params = rr.getParams();
                String allocator = params.getParam("allocator");
                String msgSize = params.getParam("msgSize");
                //String name = params.getBenchmark().replace("jmh.perf.", "").replace(".scan", "") + "/" + msgSize + "/" + allocator;
                resultsMap.computeIfAbsent(msgSize, k -> new TreeMap<>(nodeComparator)).put(allocator, rr);
            }
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
    }

    public static BoxPlot makeBoxPlot(String name, RunResult rr) {
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

    public static void draw(String svgName, String runName, double max, BoxPlot... plots) throws IOException {
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
