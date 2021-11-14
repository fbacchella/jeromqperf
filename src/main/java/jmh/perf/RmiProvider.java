package jmh.perf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class RmiProvider {

    static private final Runnable stop;
    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            java.rmi.registry.LocateRegistry.createRegistry(1501);
            String path = "/jndi/rmi://0.0.0.0:1501/jmxrmi";
            JMXServiceURL url = new JMXServiceURL("rmi", "0.0.0.0", 1501, path);
            JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                      Collections.emptyMap(),
                                                      mbs);
            cs.start();
            stop = () -> {
                try {
                    cs.stop();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    public static void start() {
        
    }
    public static void stop() {
        stop.run();
    }

}
