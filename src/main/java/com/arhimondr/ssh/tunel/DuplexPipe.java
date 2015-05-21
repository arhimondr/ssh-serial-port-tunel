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

    private final InputStream inboundIn;
    private final OutputStream inboundOut;
    private final RateLimiter inboundRateLimiter;

    private final InputStream outboundIn;
    private final OutputStream outboundOut;
    private final RateLimiter outboundRateLimiter;


    public DuplexPipe(InputStream inboundIn,
                      OutputStream inboundOut,
                      InputStream outboundIn,
                      OutputStream outboundOut,
                      int inboundRate,
                      int outboundRate) {
        this.inboundIn = inboundIn;
        this.inboundOut = inboundOut;
        this.outboundIn = outboundIn;
        this.outboundOut = outboundOut;
        this.inboundRateLimiter = RateLimiter.create(inboundRate);
        this.outboundRateLimiter = RateLimiter.create(outboundRate);
    }

    public void startDuplexTransmission() throws InterruptedException, IOException {
        startTransmission(inboundIn, inboundOut, "ssh-to-serial-port", inboundRateLimiter);
        startTransmission(outboundIn, outboundOut, "serial-port-to-ssh", outboundRateLimiter);

        try {
            waitForCompletion();
        } finally {
            closeQuietly(inboundIn, inboundOut, outboundIn, outboundOut);
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
                int lastRead = -1;
                while ((lastRead = inputStream.read(buffer)) != -1) {
                    Logger.log("Bytes (" + lastRead + ") received from [" + type + "] in");
                    if (lastRead > 0) {
                        rateLimiter.acquire(lastRead);
                        outputStream.write(buffer, 0, lastRead);
                        Logger.log("Bytes (" + lastRead + ") writen to [" + type + "] out");
                    }
                }
                Logger.log("[" + type + "] finished");
                return null;
            }
        });
    }
}
