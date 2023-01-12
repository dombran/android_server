package nio_socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import nio_socket.utils.Logger;

import static nio_socket.utils.Constants.SHUTDOWN_TIMEOUT_MILLIS;

import android.app.Activity;
import android.content.Context;

public final class NioServer implements Runnable {

    private static final Logger LOGGER = new Logger(NioServer.class.getName());

    private final ServerSettings settings;

    private ServerSocketChannel serverChannel;
    private Selector selector;

    private int connectionsNum;
    private volatile long shutdownSignalTime = -1L;

    private Context cnt;
    private Activity act;

    public NioServer(Context cnt, Activity act, ServerSettings settings) {
        this.settings = settings;
        this.cnt = cnt;
        this.act = act;
    }

    @Override
    public void run() {
        try {
            init();
            startLoop();
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred. Stopping server", e);
        } finally {
            stop();
        }
    }

    private void init() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress((InetAddress) null, settings.getPort()));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // register a simple graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void start() {
                try {
                    LOGGER.info("Got shutdown signal. Switching to shutdown mode");
                    // can't use Thread#interrupt here because this affects Channel#write
                    NioServer.this.shutdownSignalTime = System.currentTimeMillis();
                    Thread.sleep(SHUTDOWN_TIMEOUT_MILLIS);
                } catch (Exception e) {
                    LOGGER.warn("Error during shutdown. Forced shutdown", e);
                } finally {
                    LOGGER.info("Stopped");
                    Logger.resetFinally();
                }
            }
        });

        LOGGER.info("Server is now listening on port: " + settings.getPort());
    }

    private void stop() {
        try {
            LOGGER.info("Stopping server");
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            LOGGER.warn("Error during stopping server. Ignoring", e);
        }
    }

    private void startLoop() throws IOException {
        boolean needToStop = false;
        while (!needToStop) {
            boolean shutdownMode = shutdownSignalTime > 0;
            if (shutdownMode) {
                needToStop = System.currentTimeMillis() - shutdownSignalTime >= SHUTDOWN_TIMEOUT_MILLIS;
            }
            handleLoopTick(shutdownMode);
        }
    }

    private void handleLoopTick(boolean shutdownMode) throws IOException {
        selector.select();
        Set<SelectionKey> keys = selector.selectedKeys();

        Iterator<SelectionKey> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            keyIterator.remove();
            try {
                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    if (shutdownMode) {
                        continue;
                    }
                    accept();
                } else if (key.isReadable()) {
                    if (shutdownMode) {
                        continue;
                    }
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            } catch (Exception e) {
                LOGGER.error("Closing channel: error while handling selection key. Channel: " + key.channel(), e);
                closeChannelSilently(key);
            }
        }
    }

    private void accept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) {
            LOGGER.warn("No connection is available. Skipping selection key");
            return;
        }

        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        NioRequestHandler handler = (NioRequestHandler) key.attachment();
        if (handler == null) {
            connectionsNum++;
            LOGGER.info("Got new connection handler for channel: " + clientChannel
                    + ", connection #: " + connectionsNum);
            handler = NioRequestHandler.build(cnt, act, settings, connectionsNum);
            key.attach(handler);
            return;
        }

        handler.read(clientChannel);

        // switch to write mode
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException {
        NioRequestHandler handler = (NioRequestHandler) key.attachment();
        if (handler == null) {
            throw new IOException("Handler is missing for the channel: " + key.channel());
        }

        SocketChannel clientChannel = (SocketChannel) key.channel();
        handler.write(clientChannel);

        if (handler.hasNothingToWrite()) {
            closeChannelSilently(key);
        } else {
            // keep writing
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void closeChannelSilently(SelectionKey key) {
        connectionsNum--;

        SocketChannel channel = (SocketChannel) key.channel();
        key.cancel();
        LOGGER.info("Closing connection for channel: " + channel + ", active connections: " + connectionsNum);

        NioRequestHandler handler = (NioRequestHandler) key.attachment();
        if (handler != null) {
            handler.releaseSilently();
        }

        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.warn("Error during closing channel: " + channel, e);
        }
    }

}
