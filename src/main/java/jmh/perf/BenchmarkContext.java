package jmh.perf;

import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.zeromq.ZMQ.Error;

import jmh.ThreadBuilder;
import lombok.Getter;
import zmq.Ctx;
import zmq.Msg;
import zmq.SocketBase;
import zmq.ZError;
import zmq.ZMQ;

/**
 * A abstract class from which you can derive class that will be used as JMH state
 * @author Fabrice Bacchella
 *
 */
public class BenchmarkContext {

    private final CountDownLatch serverBarrier = new CountDownLatch(1);
    private final CompletableFuture<Object> failure = new CompletableFuture<>();
    private final Thread.UncaughtExceptionHandler failureHandler = this::errorHandler;
    private final Thread server;
    private final Ctx ctx;
    private final ElementsFactory factory;

    // Used to handle failing server
    final Set<Thread> benchTreads = ConcurrentHashMap.newKeySet();

    public BenchmarkContext(ElementsFactory factory) {
        // RmiProvider.start();
        this.factory = factory;
        ctx = factory.getContext();
        ctx.setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            RmiProvider.stop();
            System.exit(1);
        });
        server = factory.withServer() ? getServer() : null;
    }

    private void errorHandler(Thread t, Throwable ex) {
        failure.completeExceptionally(ex);
        benchTreads.forEach(Thread::interrupt);
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
        // RmiProvider.stop();
    }

    /**
     * Might be called in Start setup to call in internal server
     * @throws InterruptedException
     * @throws BrokenBarrierException
     */
    private Thread  getServer() {
        Thread t = ThreadBuilder.get()
                                .setDaemon(false)
                                .setTask(this::runServer)
                                .setExceptionHandler(failureHandler)
                                .setName("ZMQServer")
                                .build(true);
        // Wait for the server to be started
        try {
            serverBarrier.await();
            return t;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void stopServer() throws InterruptedException {
        if (serverRunning()) {
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
                printError("error in server connect", serverSocket.errno());
                return;
            }
            serverBarrier.countDown();
            while (! Thread.interrupted()) {
                Msg cmsg = serverSocket.recv(0);
                if (cmsg == null) {
                    printError("error in server recvmsg", serverSocket.errno());
                    return;
                }
                if (factory.waitAnswser()) {
                    Msg smsg = factory.getServerMsg(cmsg);
                    rc = serverSocket.send(smsg, 0);
                    if (!rc) {
                        printError("error in server send", serverSocket.errno());
                        return;
                    }
                }
            }
        } finally {
            serverSocket.close();
        }
    }

    private static void printError(String message, int errno) {
        if (errno != ZError.EINTR) {
            Error err = Error.findByCode(errno);
            System.out.format("%s: %s\n", message, err.getMessage());
        }
    }

    public boolean checkServer() {
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
                throw new IllegalStateException("error in connect: " + ZMQ.strerror(socket.errno()));
            }
        }

        public void close() {
            if (socket != null) {
                socket.close();
            }
            benchTreads.remove(Thread.currentThread());
        }

    }

    public void iteration(SocketBase s, int msgSize) {
        if (! checkServer()) {
            throw new IllegalStateException("Server is dead");
        }
        Msg msg = factory.getClientMsg(msgSize);
        boolean rc = s.send(msg, 0);
        if (! rc) {
            throw new IllegalStateException("error in send: " + ZMQ.strerror(s.errno()));
        }
        if (! checkServer()) {
            throw new IllegalStateException("Server is dead");
        }
        if (factory.waitAnswser()) {
            Msg smsg = s.recv(0);
            if (smsg == null) {
                throw new IllegalStateException("error in recv: " + ZMQ.strerror(s.errno()));
            }
        }
    }

}
