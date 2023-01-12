package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;


import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;
import java.util.Enumeration;

import android.content.Context;

import static nio_socket.utils.Constants.SETTINGS_FILE_DEFAULT;

import nio_socket.NioServer;
import nio_socket.ServerSettings;
import nio_socket.ServerSettingsLoader;

public class MainActivity extends AppCompatActivity {

    HtmlServer hServer;

    //int HttpServerPORT = 8888;

    EditText welcomeMsg;
    TextView infoIp;
    TextView infoMsg;
    String msgLog = "";

    Context cnt;
    Activity act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cnt = getApplicationContext();
        act = this;

        String settingsPath =  SETTINGS_FILE_DEFAULT;
        ServerSettings settings = new ServerSettingsLoader().load(cnt, settingsPath);

        NioServer server = new NioServer(cnt, act, settings);
        new Thread(server, "http-server").start();

        infoIp = (TextView) findViewById(R.id.infoip);
        infoIp.setText(getIpAddress() + ":" + settings.getPort() +  settings.getWwwRoot() + "\n");

/*        welcomeMsg = (EditText) findViewById(R.id.welcomemsg);
        //welcomeMsg.setText("Welcome from Android-er");

        infoIp = (TextView) findViewById(R.id.infoip);

        infoIp.setText(getIpAddress() + ":" + String.valueOf( HttpServerPORT ) + "\n");

        infoMsg = (TextView) findViewById(R.id.msg);

        hServer = new HtmlServer();
        new Thread(new Runnable() {
            @Override
            public void run() {
                hServer.startServer(HttpServerPORT );
            }
        }).start();
*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }


    public class HtmlServer {



        private String htmlStr_1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n" +
                "<html > \n" +
                "<head> \n" +
                "<title>Directory listing for /</title>\n" +
                "</head> \n" +
                "<body>\n" +
                "<h2>Welcome to NIO Server</h2>\n" +
                "<hr>\n" +
                "<ul>\n" +
                "<li><a href=\"hi.txt\">hi.txt</a>\n" +
                "</li> \n" +
                "</ul>\n" +
                "<hr>\n";

        private String htmlStr_2 = "</body>\n" +
                "</html>\n";

        public void startServer(int port) {

            try {
                Selector selector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                // Установлен на не блокировку
                serverSocketChannel.configureBlocking(false);
                // зарегистрируйте событие, которое получает соединение
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println ("Сервер запуска монитора " + port + " порт ..."); // С непрерывным выбором события
                constantSelect(selector);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void constantSelect(Selector selector) {
            while (true) {
                try {
                    if (selector.select() > 0) {
                        System.out.println ("текущее количество соединений: " + selector.keys().size() );
                        System.out.println ("Обнаружено активное соединение: " + selector.selectedKeys().size() );
                        Set<SelectionKey> keys = selector.selectedKeys();
                        Iterator<SelectionKey> it = keys.iterator();
                        while (it.hasNext()) {
                            SelectionKey key = it.next();
                            if (key.isAcceptable()) {
                                // Процесс время подключения приема
                                boolean succ = dealAccept(selector, key);
                                if (succ)
                                    it.remove();
                            }
                            if (key.isReadable()) {
                                // лечить событие чтения
                                boolean succ = dealRead(key);
                                if (succ)
                                    it.remove();
                            }

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

        private boolean dealRead(SelectionKey key) {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buf = ByteBuffer.allocate(10);
            // Решите, что один байт читается, вернитесь на неподвижную страницу
            try {
                int len = channel.read(buf);
                if (len <= 0) {
                    // Подключение обработки было отключено
                    return false;
                }

                // callback to activity
                msgLog += "Request of " + buf.toString() + " from " + channel.getRemoteAddress().toString() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMsg.setText(msgLog);
                    }
                });

                String str = htmlStr_1 + "<h1>" + welcomeMsg.getText().toString() + "</h1>" + htmlStr_2;
                ByteBuffer htmlBuf = ByteBuffer.allocate(str.getBytes().length);
                
                htmlBuf.put(str.getBytes());

                htmlBuf.flip();
                int outLen = channel.write(htmlBuf);


            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    channel.shutdownInput();
                    channel.shutdownOutput();
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return true;
        }


        private boolean dealAccept(Selector selector, SelectionKey key) {

            try {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                // После получения соединения зарегистрируйте событие чтения на селектор
                socketChannel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

    }


}

