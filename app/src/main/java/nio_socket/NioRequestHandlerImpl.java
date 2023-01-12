package nio_socket;

import android.app.Activity;
import android.content.Context;
import android.os.LocaleList;

import nio_socket.ServerSettings;
import nio_socket.fs.AsyncFileReader;

import nio_socket.html_pack.HtmlGen_gallery;
import nio_socket.html_pack.HtmlGen_index;
import nio_socket.protocol.Headers;
import nio_socket.protocol.MalformedStatusLineException;
import nio_socket.protocol.MultipartHeadersPart;
import nio_socket.protocol.ProtocolException;
import nio_socket.protocol.RequestStatus;
import nio_socket.protocol.parser.MalformedInputException;
import nio_socket.protocol.parser.Parser;
import nio_socket.protocol.parser.impl.HeadersParser;
import nio_socket.protocol.parser.impl.QueryStringParser;
import nio_socket.protocol.parser.impl.RequestStatusParser;
import nio_socket.request.HttpRequest;
import nio_socket.request.HttpRequestParser;
import nio_socket.request.HttpRequestValidator;
import nio_socket.request.Http_RequestBuilder;
import nio_socket.request.Http_RequestFactory;
import nio_socket.request.RawRequestReader;
import nio_socket.response.HttpResponse;
import nio_socket.response.HttpResponseFactory;
import nio_socket.response.HttpResponseWriter;
import nio_socket.utils.Logger;
import nio_socket.utils.OptimisticLock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import nio_socket.html_pack.HtmlGen;
import nio_socket.utils.StringUtilities;

public class NioRequestHandlerImpl extends NioRequestHandler{

    private static final Logger LOGGER = new Logger(NioRequestHandlerImpl.class.getName());

    //private HttpRequest request;
    private HttpResponse response;
    private Http_RequestFactory facBuilder;
    private Http_RequestBuilder builder_;

    private final HttpResponseWriter responseWriter = new HttpResponseWriter();
    private final OptimisticLock writeLock = new OptimisticLock();
    private AsyncFileReader fileReader;
    private FileReadHandler fileReadHandler;

    private final ServerSettings settings;
    private final int sessionTimeoutMillis;
    private final long creationTimeMillis;
    private final long connectionNum;

    private Context cnt;
    private Activity act;

    HashMap<String, HtmlGen> htmlMap = new HashMap<String, HtmlGen>();

    NioRequestHandlerImpl(Context cnt, Activity act, ServerSettings settings, int connectionNum) {
        this.settings = settings;
        this.sessionTimeoutMillis = settings.getSessionTimeoutSecs() * 1000;
        this.creationTimeMillis = System.currentTimeMillis();
        this.connectionNum = connectionNum;
        this.cnt = cnt;
        this.act = act;
        this.facBuilder = new Http_RequestFactory();

        htmlMap.put("gallery", new HtmlGen_gallery(cnt, act));
        htmlMap.put("index", new HtmlGen_index());
}

    @Override
    public void read(SocketChannel s_channel) throws IOException {
        //ReadableByteChannel channel = (ReadableByteChannel)s_channel;
        //String raw = new RawRequestReader().readRaw(channel);

        //request = new HttpRequestParser().parse(raw);
        //LOGGER.info("Parsed incoming HTTP request: " + request);

        HttpRequestValidator validator = new HttpRequestValidator(sessionTimeoutMillis, creationTimeMillis,
                connectionNum, settings.getMaxConnections());

        builder_ = facBuilder.ParseRequest(s_channel);

        response = validator.validate();//request);
        if (response != null) {
            //LOGGER.warn("Invalid incoming HTTP request: " + request + ", response: " + response);
            LOGGER.warn("Invalid incoming HTTP request: " + builder_.getQueryString() + ", response: " + response);
        }
    }

    @Override
    public void write(SocketChannel s_channel) throws IOException {
        WritableByteChannel channel =(WritableByteChannel) s_channel;

        if (builder_ == null) {//request == null) {
            throw new IllegalStateException("Request is not initialized");
        }

        initFileResponse();
        responseWriter.writeHeaders(channel, response);
        validateSessionTimeout();
        writePendingContent(channel);
        scheduleFileForRead();
    }



    private void initFileResponse() {
        // an error response may be already there
        if (response != null) {
            return;
        }

        HttpResponseFactory httpResponseFactory = new HttpResponseFactory();
        //try {
            //fileReader = AsyncFileReader.open(settings.getWwwRoot(), request.getPath());
            //fileReadHandler = new FileReadHandler();
            //response = httpResponseFactory.buildFileResponse(fileReader.getMetadata());
            //LOGGER.info("Started reading file for request: " + request);
        //} catch (IOException e) {
        //    LOGGER.warn("Could not read file for request: " + request);
        //    response = httpResponseFactory.buildNotFound("Could not reavd file");
        //}



        String str_req = builder_.getPathTranslated();//request.getPath();

        RequestStatus status;
        Map<String, String> getParameters;



        Headers header = builder_.getHeaders();

        switch(str_req){
            case "":
            case "/":
            case "/home":
                response = httpResponseFactory.buildCodeResponse( htmlMap.get("index").init(builder_) );
                break;
            case "/gallery":
                response = httpResponseFactory.buildCodeResponse( htmlMap.get("gallery").init(builder_) );
                break;
            default:

                try {
                    String stream_path = makePathReAssembly(str_req);
                    if( !stream_path.equals("") ) {
                        fileReader = AsyncFileReader.open(cnt.getAssets().open(stream_path), builder_.getPathTranslated());//request.getPath());
                        fileReadHandler = new FileReadHandler();
                        response = httpResponseFactory.buildFileResponse(fileReader.getMetadata());
                    }else {
                        String str = StringUtilities.urlDecode(builder_.getPathTranslated());
                        String alt_req = StringUtilities.urlDecode(builder_.getQueryString());
                        if(alt_req.equals(""))
                            fileReader = AsyncFileReader.open( str );
                        else
                            fileReader = AsyncFileReader.open( alt_req );
                        fileReadHandler = new FileReadHandler();
                        response = httpResponseFactory.buildFileResponse(fileReader.getMetadata());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    response = httpResponseFactory.buildNotFound("Could not read file");
                }

                break;
        }

    }

    private String makePathReAssembly(String str) {
        String stream_path = "";

        File fl = new File(str);
        String sExt = getFileExtension(fl);
        switch(sExt){
            case ".css":
                stream_path = "public/" + "css/" + fl.getName();
                break;
            case ".eot":
            case ".svg":
            case ".ttf":
            case ".woff":
            case ".woff2":
                stream_path += "public/" + "fonts/" + fl.getName();
                break;
            case ".png":
                stream_path += "public/" + "img/" + fl.getName();
                break;
            case ".js":
                stream_path += "public/" + "js/" + fl.getName();
                break;
            case ".ico":
                stream_path += "public/" + fl.getName();
                break;

            default:
                break;
        }

        return stream_path;
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

    private void validateSessionTimeout() {
        long time = System.currentTimeMillis();
        if (time - creationTimeMillis > sessionTimeoutMillis) {
            writeLock.lock();

            try {
                LOGGER.warn("Session timeout exceeded for request: " + builder_.getRequestURI());//request);
                response.markAsComplete();
                // get rid of buffered pending data
                response.flushPendingContent();
            } finally {
                writeLock.unlock();
            }
        }
    }

    private void writePendingContent(WritableByteChannel channel) throws IOException {
        // write only if there is an opportunity, otherwise - write on next tick
        boolean responseLocked = writeLock.tryLock();
        if (responseLocked) {
            try {
                responseWriter.writeContent(channel, response);
            } finally {
                writeLock.unlock();
            }
        }
    }

    private void scheduleFileForRead() {
        if (fileReader == null) {
            return;
        }
        fileReader.readNextChunk(fileReadHandler);
    }

    @Override
    public boolean hasNothingToWrite() {
        boolean responseLocked = writeLock.tryLock();
        if (responseLocked) {
            try {
                return response.isComplete() && !response.hasPendingContent();
            } finally {
                writeLock.unlock();
            }
        }
        return false;
    }

    @Override
    public void releaseSilently() {
        if (fileReader != null) {
            fileReader.closeSilently();
        }
    }

    private class FileReadHandler implements AsyncFileReader.ReadHandler {
        @Override
        public void onRead(byte[] data) {
            writeLock.lock();
            try {
                if (!response.isComplete()) {
                    response.addContentChunk(data);
                }
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public void onComplete() {
            LOGGER.info("Finished reading file for request: " + builder_.getRequestURI());
            writeLock.lock();
            try {
                response.markAsComplete();
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public void onError(Throwable e) {
            LOGGER.error("Error during reading file for request: " + builder_.getRequestURI(), e);
            writeLock.lock();
            try {
                response.markAsComplete();
            } finally {
                writeLock.unlock();
            }
        }
    }

}
