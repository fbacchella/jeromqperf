package jmh;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import jmh.perf.RmiProvider;
import jmh.plot.Plotter;
import jmh.plot.PlottingClass;
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
            Pattern benchPattern = Pattern.compile("^([\\p{Alpha}_$][\\p{Alpha}\\p{Digit}_$\\.]*)\\.([\\p{Alpha}_$][\\p{Alpha}\\p{Digit}_$]+)$");
            Map<String, Plotter> plotters = new HashMap<>();
            for (RunResult rr: results) {
                BenchmarkParams params = rr.getParams();
                String benchName = params.getBenchmark();
                Matcher benchMatcher = benchPattern.matcher(benchName);
                benchMatcher.matches();
                String benchClassName = benchMatcher.group(1);
                plotters.computeIfAbsent(benchClassName, k -> resolvPlotter(benchClassName)).addResult(rr);
            }
            plotters.values().forEach(p -> {
                try {
                    p.drawSvg(svgName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private static Plotter resolvPlotter(String benchClassName) {
        try {
            Class<?> bebench = Run.class.getClassLoader().loadClass(benchClassName);
            PlottingClass pc = bebench.getAnnotation(PlottingClass.class);
            Class<? extends Plotter> plotterClass = pc.plotter();
            return plotterClass.getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("No plotting for the benchmar " + benchClassName);
        }
    }
}
