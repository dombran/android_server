package nio_socket.fs;

import nio_socket.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static nio_socket.utils.Constants.FILE_READ_BUFFER_SIZE_BYTES;

class  AsyncFileReaderImpl extends AsyncFileReader  {

    private static final Logger LOGGER = new Logger(AsyncFileReaderImpl.class.getName());

    private final AsynchronousFileChannel fileChannel;
    private final FileMetadata metadata;

    private InputStream inputStream;
    private AsynchronousByteChannel async_byteChann;
    private int is_size;
    private byte[] is_buf;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(FILE_READ_BUFFER_SIZE_BYTES);
    private volatile int readPos;

    private volatile boolean reading;

    AsyncFileReaderImpl(String filePath) throws IOException {
        fileChannel = AsynchronousFileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
        metadata = new FileMetadata(fileChannel.size(), filePath);
        this.is_size = 0;
    }
    AsyncFileReaderImpl(InputStream inputStream, String filePath) throws IOException {
        fileChannel = null;
        is_size = inputStream.available();
        is_buf = new byte[is_size];
        metadata = new FileMetadata(this.is_size, filePath);

        this.inputStream = inputStream;
        int sz = this.inputStream.read(is_buf);

        readPos = 0;

    }
    @Override
    public void readNextChunk(final ReadHandler handler) {
        // this method is called from a single thread, so volatile field is enough here
        if (reading) {
            return;
        }
        reading = true;

        if(this.inputStream != null){

            int i = 0;
            for( i = 0; ( i + FILE_READ_BUFFER_SIZE_BYTES ) < is_size; i += FILE_READ_BUFFER_SIZE_BYTES ) {
                byte[] data = new byte[FILE_READ_BUFFER_SIZE_BYTES];
                System.arraycopy(is_buf, i, data, 0, FILE_READ_BUFFER_SIZE_BYTES);
                handler.onRead(data);
            }
            byte[] data = new byte[is_size - i];
            System.arraycopy(is_buf, i, data, 0, is_size - i);
            handler.onRead(data);

            handler.onComplete();

            reading = true;

        }else {
            fileChannel.read(readBuffer, readPos, null,
                    new CompletionHandler<Integer, Void>() {
                        @Override
                        public void completed(Integer result, Void attachment) {
                            if (result == -1) {
                                handler.onComplete();
                                return;
                            }

                            readPos += result;
                            readBuffer.flip();
                            byte[] data = new byte[result];
                            System.arraycopy(readBuffer.array(), 0, data, 0, result);
                            readBuffer.clear();

                            handler.onRead(data);

                            if (readPos == metadata.getSize()) {
                                handler.onComplete();
                                return;
                            }

                            reading = false;
                        }

                        @Override
                        public void failed(Throwable e, Void attachment) {
                            handler.onError(e);
                            reading = false;
                        }
                    }
            );
        }
    }

    @Override
    public FileMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void closeSilently() {
        try {
            if(fileChannel != null)
                fileChannel.close();
        } catch (IOException e) {
            LOGGER.warn("Error during closing file channel", e);
        }
    }
}
