package com.qiao.nonblocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class Client {
    public SocketChannel client = null;
    public InetSocketAddress isa = null;
    public RecvThread rt = null;

    public Client() {
    }

    public void makeConnection() {
        try {
            client = SocketChannel.open();
            isa = new InetSocketAddress(InetAddress.getLocalHost(), 4900);
            client.connect(isa);
            client.configureBlocking(false);
            receiveMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (sendMessage() != -1) {
        }

        try {
            client.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int sendMessage() {
        System.out.println("Inside SendMessage");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String msg = null;
        ByteBuffer bytebuf;
        int nBytes = 0;
        try {
            msg = in.readLine();
            System.out.println("msg is " + msg);
            bytebuf = ByteBuffer.wrap(msg.getBytes());
            nBytes = client.write(bytebuf);
            System.out.println("nBytes is " + nBytes);
            if (msg.equals("quit") || msg.equals("shutdown")) {
                System.out.println("time to stop the client");
                interruptThread();
                Thread.sleep(5000);
                client.close();
                return -1;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Wrote " + nBytes + " bytes to the server");
        return nBytes;
    }

    public void receiveMessage() {
        rt = new RecvThread("Receive THread", client);
        rt.start();
    }

    public void interruptThread() {
        rt.val = false;
    }

    public static void main(String args[]) {
        Client cl = new Client();
        cl.makeConnection();
    }

    public class RecvThread extends Thread {
        public SocketChannel sc = null;
        public boolean val = true;

        public RecvThread(String str, SocketChannel client) {
            super(str);
            sc = client;
        }

        public void run() {
            System.out.println("Inside receivemsg");
            ByteBuffer buf = ByteBuffer.allocate(2048);
            try {
                while (val) {
                    while (client.read(buf) > 0) {
                        buf.flip();
                        Charset charset = Charset.forName("us-ascii");
                        CharsetDecoder decoder = charset.newDecoder();
                        CharBuffer charBuffer = decoder.decode(buf);
                        String result = charBuffer.toString();
                        System.out.println(result);
                        buf.flip();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}