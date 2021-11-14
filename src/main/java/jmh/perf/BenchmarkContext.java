package jmh.perf;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.zeromq.ZMQException;

import jmh.ThreadBuilder;
import lombok.Getter;
import zmq.Ctx;
import zmq.Msg;
import zmq.SocketBase;
import zmq.ZMQ;

/**
 * A ZMQ wrapper that provides convienient methods to run ZMQ benchs.
 * 
 * @author Fabrice Bacchella
 *
 */
public class BenchmarkContext {

    private final CountDownLatch serverBarrier = new CountDownLatch(1);
    private final CompletableFuture<Object> failure = new CompletableFuture<>();
    private final Thread.UncaughtExceptionHandler failureHandler = this::errorHandler;
    private final Thread server;
    private final Ctx ctx;
    private final ZMQFactory factory;
    private final ZMQFactory.ServerProcessing processing;

    // Used to handle failing server and notify every benchmark threads
    private final Set<Thread> benchTreads = ConcurrentHashMap.newKeySet();

    public BenchmarkContext(ZMQFactory factory) {
        this.factory = factory;
        ctx = factory.getContext();
        ctx.setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            RmiProvider.stop();
            System.exit(1);
        });
        processing = factory.withServer() ? factory.getServerProcessing(this) : null;
        server = factory.withServer() ? getServer() : null;
    }

    /**
     * Must be called in tear down, it will stop an eventually started server and stop the context
     */
    public void stop() {
        try {
            stopServer();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ThreadBuilder.get().setTask(() -> ctx.terminate()).build(true);
    }

    private Thread getServer() {
        Thread t = ThreadBuilder.get()
                                .setDaemon(false)
                                .setTask(this::runServer)
                                .setExceptionHandler(failureHandler)
                                .setName("ZMQServer")
                                .build(true);
        try {
            // Wait for the server to be started
            serverBarrier.await();
            return t;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void errorHandler(Thread t, Throwable ex) {
        failure.completeExceptionally(ex);
        benchTreads.forEach(Thread::interrupt);
    }

    public void stopServer() throws InterruptedException {
        if (server != null && serverRunning()) {
            server.interrupt();
        }
    }

    public boolean serverRunning() {
        return server != null && server.isAlive();
    }

    private void runServer() {
        SocketBase serverSocket = factory.getServerSocket(ctx);
        try {
            boolean rc = serverSocket.bind(factory.getUrl());
            if (!rc) {
                throw new ZMQException("error in server bind", serverSocket.errno());
            }
            serverBarrier.countDown();
            while (! Thread.interrupted()) {
                processing.process(serverSocket);
            }
        } finally {
            serverSocket.close();
        }
    }

    private boolean checkServer() {
        if (server != null) {
            if (failure.isCompletedExceptionally()) {
                try {
                    failure.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                }
                return false;
            } else {
               return true;
            }
        } else {
            return true;
        }
    }

    public StatefullSocket getSocketState() {
        return new StatefullSocket();
    }

    public class StatefullSocket implements AutoCloseable {

        @Getter
        private final SocketBase socket;

        StatefullSocket() {
            benchTreads.add(Thread.currentThread());
            socket = factory.getClientSocket(ctx);
            if (! ZMQ.connect(socket, factory.getUrl())) {
                throw new ZMQException("error in connect", socket.errno());
            }
        }

        public void close() {
            if (socket != null) {
                socket.close();
            }
            benchTreads.remove(Thread.currentThread());
        }

    }

    /**
     * A method that can be used in a server loop or a client benchmark<p>
     * Send a message, but don't expect an answer
     * @param s The socket to use for the message
     * @param msgSize the message size
     */
    public void simpleSendMessage(SocketBase s, int msgSize) {
        if (! checkServer()) {
            throw new IllegalStateException("Server is dead");
        }
        Msg msg = factory.getQueryMsg(msgSize);
        boolean rc = s.send(msg, 0);
        if (! rc) {
            throw new ZMQException("error in simpleSendMessage:send", s.errno());
        }
        if (! checkServer()) {
            throw new IllegalStateException("Server is dead");
        }
    }

    /**
     * A method that can be used in a server loop or a client benchmark<p>
     * Send a message, and wait for an answer
     * @param s The socket to use for the message
     * @param msgSize the message size
     */
    public void queryAnswerMessage(SocketBase s, int msgSize) {
        if (! checkServer()) {
            throw new IllegalStateException("Server is dead");
        }
        Msg msg = factory.getQueryMsg(msgSize);
        boolean rc = s.send(msg, 0);
        if (! rc) {
            throw new ZMQException("error in queryAnswerMessage:send", s.errno());
        }
        if (! checkServer()) {
            throw new IllegalStateException("Server is dead");
        }
        Msg smsg = s.recv(0);
        if (smsg == null) {
            throw new ZMQException("error in queryAnswerMessage:recv", s.errno());
        }
    }

    /**
     * A method that can be used in a server loop or a client benchmark<p>
     * Consume a received message
     * @param s The query message
     */
    public void consume(SocketBase s) {
        Msg cmsg = s.recv(0);
        if (cmsg == null) {
            throw new ZMQException("error in consume:recv", s.errno());
        }
    }

    /**
     * A method that can be used in a server loop or a client benchmark<p>
     * Wait for a message and process it
     * @param s The query message
     */
    public void processMessage(SocketBase s) {
        boolean rc;
        Msg cmsg = s.recv(0);
        if (cmsg == null) {
            throw new ZMQException("error in consume:recv", s.errno());
        }
        if (factory.waitAnswser()) {
            Msg smsg = factory.getAnswerMsg(cmsg);
            rc = s.send(smsg, 0);
            if (!rc) {
                throw new ZMQException("error in consume:send", s.errno());
            }
        }
    }

}
