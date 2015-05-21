package com.arhimondr.ssh.tunel;

import java.io.IOException;

public class ExceptionUtils {
    public static void rethrowIOException(Throwable e) throws IOException {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else if (e instanceof IOException) {
            throw (IOException) e;
        } else if (e instanceof Error) {
            throw (Error) e;
        } else {
            throw new IllegalArgumentException("Unexpected exception", e);
        }
    }
}
