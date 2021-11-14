package jmh.bench.allocators;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import jmh.perf.BenchmarkContext.StatefullSocket;

@State(Scope.Thread)
public class ThreadState {

    StatefullSocket s;

    @Setup(Level.Trial)
    public void newClientSocket(ZmqState globalState) {
        s = globalState.zctx.getSocketState();
    }

    @TearDown
    public void close(ZmqState globalState) {
        s.close();
    }

}
