package com.arhimondr.ssh.tunel;


import com.google.common.util.concurrent.RateLimiter;
import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;

import static com.arhimondr.ssh.tunel.IOUtils.BUFFER_SIZE;
import static com.arhimondr.ssh.tunel.IOUtils.RATE_LIMIT;
import static java.util.concurrent.TimeUnit.MINUTES;

public class SerialPortTransmitter {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final String serialPortName;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    private final RateLimiter inboundRateLimiter = RateLimiter.create(RATE_LIMIT);
    private final RateLimiter outboundRateLimiter = RateLimiter.create(RATE_LIMIT);

    private SerialPort serialPort;

    public SerialPortTransmitter(String serialPortName, InputStream inputStream, OutputStream outputStream) {
        this.serialPortName = serialPortName;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void startDuplexTransmission() throws IOException, InterruptedException {
        try {
            openPort();
            startPortReader();
            Future<Void> future = startPortWriter();
            try {
                future.get();
            } catch (ExecutionException e) {
                ExceptionUtils.rethrowIOException(e.getCause());
            }
        } finally {
            closePort();
            executorService.shutdownNow();
            executorService.awaitTermination(1, MINUTES);
        }

    }

    private void openPort() throws IOException {
        serialPort = new SerialPort(serialPortName);
        try {
            serialPort.openPort();
            serialPort.setParams(
                    SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE
            );
        } catch (SerialPortException e) {
            throw new IOException("Error during opening serial port with name " + serialPortName, e);
        }

    }

    private void startPortReader() {
        executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                byte[] buffer;
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        buffer = serialPort.readBytes();
                        if (buffer != null) {
                            Logger.log("Received " + buffer.length + " bytes from serial port.");
                            inboundRateLimiter.acquire(buffer.length);
                            outputStream.write(buffer);
                            Logger.log("Writen " + buffer.length + " bytes to the client from serial port.");
                        } else {
                            Thread.sleep(100);
                        }
                    }
                } catch (SerialPortException e) {
                    throw new IOException("Error during reading serial port with name " + serialPortName, e);
                } catch (InterruptedException e) {
                    Logger.log("Port reader interrupted");
                }
                Logger.log("Port reader finished");
                return null;
            }
        });
    }

    private Future<Void> startPortWriter() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                byte[] buffer = new byte[BUFFER_SIZE];
                int length;
                try {
                    while ((length = inputStream.read(buffer)) != -1) {
                        Logger.log("Received " + length + " bytes from the client.");
                        if (length > 0) {
                            byte[] trimmed = new byte[length];
                            System.arraycopy(buffer, 0, trimmed, 0, length);
                            outboundRateLimiter.acquire(trimmed.length);
                            serialPort.writeBytes(trimmed);
                            Logger.log("Writen " + length + " bytes to the serial port.");
                        }
                    }
                } catch (SerialPortException e) {
                    throw new IOException("Error during writing serial port with name " + serialPortName, e);
                }

                Logger.log("Port writer finished");
                return null;
            }
        });
    }

    private void closePort() {
        if (serialPort != null) {
            try {
                serialPort.closePort();
            } catch (SerialPortException ignore) {
            }
        }
    }

}
