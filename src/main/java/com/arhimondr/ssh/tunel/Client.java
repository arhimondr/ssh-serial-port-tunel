package com.arhimondr.ssh.tunel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static com.arhimondr.ssh.tunel.IOUtils.closeQuietly;

public class Client {

    private static final String COM_PORT_NAME = "COM1";
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 10001;

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        Socket sshSocket = null;

        InputStream sshSocketInput = null;
        OutputStream sshSocketOutput = null;

        try {
            serverSocket = new ServerSocket(SERVER_PORT, 0, InetAddress.getByName(SERVER_HOST));

            Logger.log("Server started on port " + SERVER_PORT);

            sshSocket = serverSocket.accept();
            sshSocket.setKeepAlive(true);

            sshSocketInput = sshSocket.getInputStream();
            sshSocketOutput = sshSocket.getOutputStream();

            System.out.println("Client connected.");

            SerialPortTransmitter serialPortTransmitter = new SerialPortTransmitter(
                    COM_PORT_NAME, sshSocketInput, sshSocketOutput
            );

            serialPortTransmitter.startDuplexTransmission();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(sshSocketInput, sshSocketOutput);
            closeQuietly(sshSocket);
            closeQuietly(serverSocket);
        }


    }
}
