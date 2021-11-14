package jmh.allocators;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;

import zmq.msg.MsgAllocator;

public abstract class MetricsAllocator implements MsgAllocator {

    static final MetricRegistry metrics = new MetricRegistry();
    static private final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
    static {
        reporter.start();
    }

}
