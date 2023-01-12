package nio_socket.html_pack;

import nio_socket.request.HttpRequest;
import nio_socket.request.Http_RequestBuilder;
import nio_socket.response.HttpResponse;
import nio_socket.response.HttpResponseFactory;

public class HtmlGen {

    public HTMLDocument init( Http_RequestBuilder builder_) {

        return renderDocument();
    }

    private HTMLDocument renderDocument() {
        HTMLDocument doc = new HTMLDocument("About");
        doc.setOwnerClass(getClass().getSimpleName());

        doc.writeln("<div class=\"page-header\"><h1>About</h1></div>");
        doc.write("<p>" + "Android HTTP server" + " running.</p>");
        doc.write("<p>Small multithread web server in Java. ");
        doc.write("Implements most of the HTTP 1.1 specification. Handling dynamic pages. ");
        doc.write("Supports cookies, sessions, file uploads.</p>");
        doc.write("<p>Written by Fedor.</p>");
        return doc;
    }


}
