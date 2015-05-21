package com.arhimondr.ssh.tunel;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;
import java.net.Socket;

import static com.arhimondr.ssh.tunel.IOUtils.RATE_LIMIT;
import static com.arhimondr.ssh.tunel.IOUtils.closeQuietly;


public class Server {

    private static final String LOCAL_SSH_SERVER_HOST = "localhost";
    private static final int LOCAL_SSH_SERVER_PORT = 22;

    private static final String SERIAL_PORT_VIRTUAL_FILE = "/home/andrii/pipe";

    public static void main(String[] args) {
        Socket sshSocket = null;

        InputStream sshSocketInput = null;
        OutputStream sshSocketOutput = null;

        InputStream pipeInput = null;
        OutputStream pipeOutput = null;

        AFUNIXSocket pipe = null;

        try {
            sshSocket = new Socket(LOCAL_SSH_SERVER_HOST, LOCAL_SSH_SERVER_PORT);
            sshSocket.setKeepAlive(true);

            sshSocketInput = sshSocket.getInputStream();
            sshSocketOutput = sshSocket.getOutputStream();

            pipe = AFUNIXSocket.newInstance();
            pipe.connect(new AFUNIXSocketAddress(new File(SERIAL_PORT_VIRTUAL_FILE)));
            pipe.setKeepAlive(true);

            pipeInput = pipe.getInputStream();
            pipeOutput = pipe.getOutputStream();

            Logger.log("Server started.");

            DuplexPipe duplexPipe = new DuplexPipe(sshSocketInput, pipeOutput, pipeInput, sshSocketOutput, RATE_LIMIT, RATE_LIMIT);
            duplexPipe.startDuplexTransmission();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(sshSocketInput, sshSocketOutput, pipeInput, pipeOutput);
            closeQuietly(sshSocket);
            closeQuietly(pipe);
        }
    }
}
