package nio_socket;

import android.app.Activity;
import android.content.Context;

import nio_socket.ServerSettings;
import nio_socket.fs.AsyncFileReader;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public abstract class NioRequestHandler {

    public abstract void read(SocketChannel channel) throws IOException;

    public abstract void write(SocketChannel channel) throws IOException;

    public abstract boolean hasNothingToWrite();

    public abstract void releaseSilently();

    public static NioRequestHandler build(Context cnt, Activity act, ServerSettings settings, int connectionNum) {
        return new NioRequestHandlerImpl(cnt, act, settings, connectionNum);
    }

}
