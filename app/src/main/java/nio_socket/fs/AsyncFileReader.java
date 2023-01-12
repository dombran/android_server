package nio_socket.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class AsyncFileReader {

    private static String assets_path = "public";

    public static AsyncFileReader open(String root, String path) throws IOException {
        String filePath = (root + path).replace("/", File.separator);
        return new AsyncFileReaderImpl(filePath);
    }
    public static AsyncFileReader open(String path) throws IOException {
        //String filePath = (assets_path + path).replace("/", File.separator);
        return new AsyncFileReaderImpl(path);
    }
    public static AsyncFileReader open(InputStream inputStream, String filePath) throws IOException {
        return new AsyncFileReaderImpl(inputStream, filePath);
    }
    public abstract void readNextChunk(ReadHandler handler);

    public abstract FileMetadata getMetadata();

    public abstract void closeSilently();

    public interface ReadHandler {

        void onRead(byte[] data);

        void onComplete();

        void onError(Throwable e);

    }

}
