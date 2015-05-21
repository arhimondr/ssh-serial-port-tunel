package com.arhimondr.ssh.tunel;

import jssc.SerialPort;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class IOUtils {

    public static final int BUFFER_SIZE = 256;

    public static final int RATE_LIMIT = SerialPort.BAUDRATE_57600 / 8;

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignore) {

            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {

            }
        }
    }

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            closeQuietly(closeable);
        }
    }
}
