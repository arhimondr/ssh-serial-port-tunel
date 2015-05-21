package com.arhimondr.ssh.tunel;


import com.google.common.util.concurrent.RateLimiter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;

import static com.arhimondr.ssh.tunel.ExceptionUtils.rethrowIOException;
import static com.arhimondr.ssh.tunel.IOUtils.BUFFER_SIZE;
import static com.arhimondr.ssh.tunel.IOUtils.closeQuietly;
import static java.util.concurrent.TimeUnit.MINUTES;

public class DuplexPipe {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final ExecutorCompletionService<Void> completionService =
            new ExecutorCompletionService<Void>(executorService);

    private final InputStream serialPortIn;
    private final OutputStream socketOut;
    private final RateLimiter serialPortToSocketRateLimiter;

    private final InputStream socketIn;
    private final OutputStream serialPortOut;
    private final RateLimiter socketToSerialPortRateLimiter;


    public DuplexPipe(InputStream serialPortIn,
                      OutputStream socketOut,
                      int serialPortToSocketRate,
                      InputStream socketIn,
                      OutputStream serialPortOut,
                      int socketToSerialPortRate) {
        this.serialPortIn = serialPortIn;
        this.socketOut = socketOut;
        this.socketIn = socketIn;
        this.serialPortOut = serialPortOut;
        this.serialPortToSocketRateLimiter = RateLimiter.create(serialPortToSocketRate);
        this.socketToSerialPortRateLimiter = RateLimiter.create(socketToSerialPortRate);
    }

    public void startDuplexTransmission() throws InterruptedException, IOException {
        startTransmission(serialPortIn, socketOut, "serial-port-to-socket", serialPortToSocketRateLimiter);
        startTransmission(socketIn, serialPortOut, "socket-to-serial-port", socketToSerialPortRateLimiter);

        try {
            waitForCompletion();
        } finally {
            closeQuietly(serialPortIn, socketOut, socketIn, serialPortOut);
            executorService.shutdownNow();
            executorService.awaitTermination(1, MINUTES);
        }
    }

    private void waitForCompletion() throws InterruptedException, IOException {
        Future<Void> future = completionService.take();
        try {
            future.get();
        } catch (ExecutionException e) {
            rethrowIOException(e.getCause());
        }
    }

    private void startTransmission(final InputStream inputStream,
                                   final OutputStream outputStream,
                                   final String type,
                                   final RateLimiter rateLimiter) {
        completionService.submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                byte[] buffer = new byte[BUFFER_SIZE];
                int lastRead;
                try {
                    while ((lastRead = inputStream.read(buffer)) != -1) {
                        Logger.log("Bytes (" + lastRead + ") read via [" + type + "]");
                        if (lastRead > 0) {
                            rateLimiter.acquire(lastRead);
                            outputStream.write(buffer, 0, lastRead);
                            Logger.log("Bytes (" + lastRead + ") transmitted via [" + type + "]");
                        } else {
                            Thread.sleep(100);
                        }
                    }
                } catch (InterruptedException e) {
                    Logger.log("[" + type + "] transmission interrupted");
                }
                Logger.log("[" + type + "] transmission finished");
                return null;
            }
        });
    }
}
