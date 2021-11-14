package jmh.bench.allocators;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class Push {

    @Benchmark
    public void fireAndForget(ZmqState state, ThreadState threadState) throws Exception {
        state.zctx.iteration(threadState.s.getSocket(), state.msgSize);
    }

}
