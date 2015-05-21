package com.arhimondr.ssh.tunel;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static com.arhimondr.ssh.tunel.IOUtils.RATE_LIMIT;
import static com.arhimondr.ssh.tunel.IOUtils.RATE_LIMIT_BODS;
import static com.arhimondr.ssh.tunel.IOUtils.closeQuietly;
import static com.fazecast.jSerialComm.SerialPort.*;

public class Client {

    private static final String COM_PORT_NAME = "COM1";
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 10001;

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        Socket sshSocket = null;

        InputStream sshSocketInput = null;
        OutputStream sshSocketOutput = null;

        InputStream serialPortInput = null;
        OutputStream serialPortOutput = null;

        SerialPort serialPort = null;

        try {
            serverSocket = new ServerSocket(SERVER_PORT, 0, InetAddress.getByName(SERVER_HOST));

            Logger.log("Server started on port " + SERVER_PORT);

            sshSocket = serverSocket.accept();
            sshSocket.setKeepAlive(true);

            sshSocketInput = sshSocket.getInputStream();
            sshSocketOutput = sshSocket.getOutputStream();

            Logger.log("Client connected.");

            serialPort = SerialPort.getCommPort(COM_PORT_NAME);
            serialPort.openPort();
            serialPort.setComPortParameters(RATE_LIMIT_BODS, 8, ONE_STOP_BIT, NO_PARITY);
            serialPort.setFlowControl(FLOW_CONTROL_RTS_ENABLED | FLOW_CONTROL_CTS_ENABLED);
//            serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING | TIMEOUT_WRITE_SEMI_BLOCKING, 100, 100);


            serialPortInput = serialPort.getInputStream();
            serialPortOutput = serialPort.getOutputStream();

            Logger.log("Serial port initialized.");

            DuplexPipe duplexPipe = new DuplexPipe(
                    serialPortInput, sshSocketOutput, RATE_LIMIT,
                    sshSocketInput, serialPortOutput, RATE_LIMIT
            );

            duplexPipe.startDuplexTransmission();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(serialPortInput, serialPortOutput, sshSocketInput, sshSocketOutput);
            closeQuietly(serialPort);
            closeQuietly(sshSocket);
            closeQuietly(serverSocket);
        }


    }
}
