package com.qiao.nonblocking;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

public class NonBlockingServer {
    public Selector sel = null;
    public ServerSocketChannel server = null;
    public SocketChannel socket = null;
    public int port = 4900;
    String result = null;

    public NonBlockingServer() {
        System.out.println("Inside default ctor");
    }
/**
用于指定连接的java.net.InetSocketAddress和用于操作读写的java.nio.channels.SocketChannel类。
Selector在Reactor模式中提供注册功能，Selector在几个SelelctableChannels上实现多路复用。
同时Channel在Selelctor上注册事件。当客户端请求时，Selector解复用请求，并分派请求给对应的通道处理。
 */
    public void initializeOperations() throws IOException {
        System.out.println("Inside initialization");
        sel = Selector.open();
        /**
         * Channel为客户端请求提供服务的通道，首先创建连接,创建ServerSocketChannel服务，并绑定了本地端口：
         */
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        InetAddress ia = InetAddress.getLocalHost();
        InetSocketAddress isa = new InetSocketAddress(ia, port);
        server.socket().bind(isa);
    }

    public void startServer() throws IOException {
        System.out.println("Inside startserver");
        initializeOperations();
        System.out.println("Abt to block on select()");
        //在Selector上根据将要处理的事件注册自己,接受连接的通带.SelectionKey对象代表通道与选择器的注册。
        SelectionKey acceptKey = server.register(sel, SelectionKey.OP_ACCEPT);
        /**在Selector上注册的ServerSocketChannel接收全部传入的连接。
         * 服务注册后，通过迭代每一个在选择器上注册的Key，处理事件。当key被执行后，从列表中移除。
         * 如果key可用，并且连接允许，可以在channel上注册进一步的操作，如读或写。
         * 如果key是可读或者可写的，可以在连接后，进行读或写操作。
         */
        while (acceptKey.selector().select() > 0) {
            Set readyKeys = sel.selectedKeys();
            Iterator it = readyKeys.iterator();

            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                it.remove();

                if (key.isAcceptable()) {
                    System.out.println("Key is Acceptable");
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    //等待客户请求
                    socket = (SocketChannel) ssc.accept();
                    socket.configureBlocking(false);
                    SelectionKey another = socket.register(sel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                }
                if (key.isReadable()) {
                    System.out.println("Key is readable");
                    String ret = readMessage(key);
                    if (ret.length() > 0) {
                        writeMessage(socket, ret);
                    }
                }
                if (key.isWritable()) {
                    System.out.println("THe key is writable");
                    String ret = readMessage(key);
                    socket = (SocketChannel) key.channel();
                    if (result.length() > 0) {
                        writeMessage(socket, ret);
                    }
                }
            }
        }
    }

    public void writeMessage(SocketChannel socket, String ret) {
        System.out.println("Inside the loop");
        if (ret.equals("quit") || ret.equals("shutdown")) {
            return;
        }
        File file = new File(ret);
        try {
            RandomAccessFile rdm = new RandomAccessFile(file, "r");
            FileChannel fc = rdm.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            fc.read(buffer);
            buffer.flip();

            Charset set = Charset.forName("us-ascii");
            CharsetDecoder dec = set.newDecoder();
            CharBuffer charBuf = dec.decode(buffer);
            System.out.println(charBuf.toString());
            buffer = ByteBuffer.wrap((charBuf.toString()).getBytes());
            int nBytes = socket.write(buffer);
            System.out.println("nBytes = " + nBytes);
            result = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readMessage(SelectionKey key) {
        socket = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        try {
            socket.read(buf);
            buf.flip();
            Charset charset = Charset.forName("us-ascii");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buf);
            result = charBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String args[]) {
        NonBlockingServer nb = new NonBlockingServer();
        try {
            nb.startServer();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}