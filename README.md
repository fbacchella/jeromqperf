This project holds a generic infrastructure to tests jeromq. It allows to write test by providing a factory class `jmh.perf.ZMQFactory` to a jmh.perf.BenchmarkContext inside
JMH benchmark classes.

A simple example, used to test allocators can be found in the package `jmh.bench.allocators`.

The ZMQ context and runner will be provided by a class using the JMH annotation `org.openjdk.jmh.annotations.State(Scope.Benchmark)` what will be provided to each benchmark. This class needs to instantiate a `jmh.perf.BenchmarkContext` that will be used by each benchmark. THis class provides a single ZMQ context, and many helpers methods that can be used by individual benchmarks. This class is given an instance of a class implementing `jmh.perf.BenchmarkContext`, specifics to a set of benchmark that describe the elemets of ZMQ that needs to be measured.

It generate a simple `jeromqperf.jar` that will launch run.

Usage:

    Option                   Description                           
    ------                   -----------                           
    -f, --fast               Fast run for tests                    
    -h, --help               Shows help                            
    -j, --json               Generate a json file with results     
    -l, --log                Generate a output log file            
    -p, --profiler           Add the given class name as a profiler
    -r, --withRMI [Integer]  Start a RMI server                    
    -s, --svg                Generate an SVG of the given name     

And it take an option lists of bench to run.

To run the provided sample benchmark:

    java -Dio.netty.leakDetection.level=disabled -Xmx31g -XX:MaxGCPauseMillis=20 -XX:MaxDirectMemorySize=200g -jar jeromqperf.jar -s jeromq.svg -l jeromq.log -p GCProfiler jmh.bench.allocators..*

## Graph generation

It's possible for a given benchmark to generate a SVG plot for better display. To do that, one should implement the interface `jmh.plot.Plotter` and using the annotation `jmh.plot.PlottingClass` on benchmarks classes, tells which class to use.

An example can be found with the class `jmh.bench.allocators.Plotter` that draws bar plot with error for better visualisation of the variability.
