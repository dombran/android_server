package nio_socket.html_pack;

import static java.net.URLDecoder.decode;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import nio_socket.html_pack.HtmlGen;
import nio_socket.request.HttpRequest;
import nio_socket.request.Http_RequestBuilder;
import nio_socket.response.HttpResponse;
import nio_socket.response.HttpResponseFactory;
import nio_socket.utils.FileIconMapper;
import nio_socket.utils.FileUtilities;
import nio_socket.utils.StringUtilities;

public class HtmlGen_gallery extends  HtmlGen{



    private static final FileIconMapper MAPPER = new FileIconMapper();
    private static final String ADMIN_DRIVE_ACCESS_ENABLED = "admin.driveAccess.enabled";
    private static final String THUMB_IMG_FOLDER = "thum_imgs";

    //private static final String thumbnailsFolder = "thumbnail";

    private File ex_StorageDirectory;
    private Activity _act;
    private Context _cnt;
    Uri collection;

    private JsonParse_mediaList_ext jp_mediaList;
    private static final String s_thumDBObj_name = "thumDB.json";

    class Images {
        private final Uri uri;
        private final String name;

        private final int size;

        private final String path;
        private final Long date;
        private final String relativePath;
        private final String mime;

        public Images(Uri uri, String path, Long date, String s_relPath, String mime, String name, int size) {
            this.uri = uri;
            this.name = name;
            this.size = size;

            this.path = path;
            this.date = date;
            this.mime = mime;
            this.relativePath = s_relPath;
        }

    }
    List<Images> video_List = new ArrayList<Images>();
    Vector<String> dirList = new Vector<String>();

    public HtmlGen_gallery(Context cnt, Activity act)  {
        String jpegMime = "image/jpeg";
        this._cnt = cnt;
        this._act = act;
        //exStorageDirectory = ((Activity) getServletContext() // /data/user/0/ro.polak.webserver/files
        //        .getAttribute("android.content.Context")).getFilesDir();//.getAbsoluteFile();

        try {
            ex_StorageDirectory = _act.getFilesDir().getCanonicalFile(); // /data/data/com.example.myapplication/files

        } catch (IOException e) {
            e.printStackTrace();
        }

        // create htumbnails directories if apsent
        File thum_dir = new File(ex_StorageDirectory.toString() + File.separator + THUMB_IMG_FOLDER);
        if( !thum_dir.exists() ){
            thum_dir.mkdir();
        }
        // making thumbnails DB object
        try{
            jp_mediaList = new JsonParse_mediaList_ext(thum_dir.toString() + File.separator + s_thumDBObj_name);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(jp_mediaList.getI_itemNum() == 0) {

            Uri collection; // content://media/external/file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Files.getContentUri("external");
            }

            String[] projection = new String[]{
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.SIZE
            };

            try (Cursor cursor = _act.getContentResolver().query(
                    collection, // content://media/external/file
                    projection,
                    null,
                    null,
                    null
            )) {
                // Cache column indices.
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int pathCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                int nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int relPath = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH);
                int dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED);
                int mimeType = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
                int sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);


                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    long id = cursor.getLong(idCol);
                    String path = cursor.getString(pathCol);
                    String name = cursor.getString(nameCol);
                    String s_relPath = cursor.getString(relPath);//new File(path).getParent();
                    long date = cursor.getLong(dateCol);
                    int size = cursor.getInt(sizeCol);
                    String mime = cursor.getString(mimeType);

                    if (mime == null)
                        continue;
                    else if (!mime.equals(jpegMime)) {
                        continue;
                    }


                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.

                    video_List.add(new Images(contentUri, path, date, s_relPath, mime, name, size));
                }
            }

            // find different beetwin list and thumb
            int i_thumbNum = 1;
            for(Images li: video_List){
                String s_thName = "";//"thumb_" + i_thumbNum + ".png";
                jp_mediaList.add_next(new JsonParse_mediaList( li.path, li.name, s_thName, i_thumbNum++ ));

            }
            jp_mediaList.save_toFile();
        }

        // making directories list
        ArrayList<JsonParse_mediaList> al = jp_mediaList.get_Object();
        for(JsonParse_mediaList ml: al){
            File fll = new File(new File(ml.getS_linkFile()).getParent());

            if( !dirList.contains( fll.toString() ) ) {
                dirList.add( fll.toString() );
            }
        }



       /* String str_folder = "";
        for(Images li: videoList){
            File fll = new File(new File(li.path).getParent());
            str_folder = fll.getName();
            if( !dirList.contains( str_folder ) ) {
                dirList.add(str_folder);
            }
            li.setThumbFolder(str_folder);
        }

        // making new dir
        for(String li: dirList){
            File fl2 = new File(exStorageDirectory + "/" + li);
            if(!fl2.exists()) {
                fl2.mkdir();
            }
        }*/

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

    /**
     * {@inheritDoc}
     */
    @Override
    public HTMLDocument init(Http_RequestBuilder builder_) {//final HttpServletRequest request, final HttpServletResponse response) {
        //ServerConfig serverConfig = (ServerConfig) getServletContext().getAttribute(ServerConfig.class.getName());

        HTMLDocument doc = new HTMLDocument("Drive Access");
        doc.setOwnerClass(getClass().getSimpleName());

        doc.writeln("<div class=\"page-header\"><h1>Gallery</h1></div>");

        //if (!serverConfig.getAttribute(ADMIN_DRIVE_ACCESS_ENABLED).equals(ServerConfigImpl.TRUE)) {
        //    renderFunctionDisabled(response, doc);
        //    return doc;
        //}

        String path = StringUtilities.urlDecode(builder_.getQueryString());
        //if ("".equals(path)) {
        //    path = "/";
        //}
        renderBreadcrubms(doc, StringUtilities.urlDecode(builder_.getPathTranslated()) );//hRequest.getPath());

        boolean f_path = false;
        for(String li: dirList){
            if(li.equals(path)) {
                f_path = true;
                break;
            }else{
                f_path = false;
            }
        }

        if(f_path){ // query enter to directories

            ArrayList<JsonParse_mediaList> al = jp_mediaList.get_Object();
            for(JsonParse_mediaList jp: al){
                File fll = new File(new File(jp.getS_linkFile()).getParent());

                if(path.equals(fll.toString())) {
                    if (jp.getS_nameThumbnails().equals("")) { //thumbnails is absent
                        String thumbStr = thumbnailToDisk(Uri.parse(jp.getS_linkFile()), jp);
                        jp.setS_linkThumbnails(thumbStr);
                    }

                    pic_Images(doc, jp);
                }

            }

            jp_mediaList.save_toFile();
        }else { // need to open directories list
            for(String li: dirList) {
                renderSetDirectory(doc, li);
            }
        }

        return doc;
    }

    private String thumbnailToDisk( Uri link, JsonParse_mediaList jp) {
        String str = "thum";
        String file_TName = str + "_" + jp.getS_nameFile();
        String thumbPath = ex_StorageDirectory + "/" + THUMB_IMG_FOLDER;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

            File fl = new File(thumbPath + "/" + file_TName);
            if(!fl.exists()) {  // if file is missing

                try  {
                    File fil = new File(thumbPath + "/" + file_TName);
                    fil.createNewFile();

                    FileOutputStream out = new FileOutputStream(fil);
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(link.toString() ),
                            320, 240);


                    thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();

                } catch (Exception e) {

                }

            }
        }

        return file_TName;
    }

    private void renderBreadcrubms(final HTMLDocument doc, final String path) {
        doc.writeln("<ol class=\"breadcrumb\">");
        doc.writeln("<li><a href=\"/gallery?"
                + StringUtilities.urlEncode("/")
                + "\"><img src=\"/public/img/home.png\" alt=\"home\"></a></li>");
        StringTokenizer st = new StringTokenizer(path.replace('\\', '/'), "/");
        String currentPath = "/";
        while (st.hasMoreTokens()) {
            String directory = st.nextToken();
            currentPath += directory + "/";
            doc.writeln("<li><a href=\"/gallery?"
                    + StringUtilities.urlEncode(currentPath)
                    + "\">"
                    + directory
                    + "</a></li>");
        }

        doc.writeln("</ol>");
    }



    private void listImages(final HTMLDocument doc, final List<Images> li){
        StringBuilder filesString = new StringBuilder();

        if(li.size() == 0){
            doc.writeln("<div class=\"alert alert-danger\" role=\"alert\">"
                    + "<strong>Oh snap!</strong> Unable to read files.</div>");
        }else {
            for(Images im: li){
                if(im.size == 0){
                    doc.writeln("<div class=\"alert alert-danger\" role=\"alert\">"
                            + "<strong>Oh snap!</strong> Unable to read files.</div>");
                }else {
                    File file = new File(im.uri.toString());
                    filesString.append("<p class=\"filemanager\"><img src=\" "
                            + StringUtilities.urlEncode(ex_StorageDirectory + "/" + THUMB_IMG_FOLDER)//MAPPER.getIconRelativePath(FileUtilities.getExtension(file.getName()))
                            + "\" alt=\"file\" /> <a href=\"/GetFile?"
                            + StringUtilities.urlEncode(im.path )
                            + "\">"
                            + im.name
                            + "</a> "
                            + FileUtilities.fileSizeUnits(file.length())
                            + "</p>");
                }
            }
        }

        doc.write(filesString.toString());
    }

    private void renderSetDirectory(final HTMLDocument doc, final String path){
        StringBuilder directories = new StringBuilder();

        directories
                .append("<p class=\"filemanager\"><img src=\"/public/img/folder.png\""
                        + " alt=\"folder\" /> <a href=\"/gallery?"
                        + StringUtilities.urlEncode(path )
                        + "\">"
                        + path + "</a></p>");

        doc.write(directories.toString());
    }
    private void pic_Images(final HTMLDocument doc, final JsonParse_mediaList jp){
        StringBuilder filesString = new StringBuilder();
        String sttr = ex_StorageDirectory + File.separator + THUMB_IMG_FOLDER + "/" + jp.getS_nameThumbnails();
        File fal = new File (sttr);
        if(!fal.exists()){
            sttr = "_a" ;
        }
        filesString.append("<p class=\"filemanager\"><img src=\""
                + StringUtilities.urlEncode(sttr)
                + "\" alt=\"file\" /> <a href=\"/GetFile?"
                + StringUtilities.urlEncode( jp.getS_linkFile() )
                + "\">"
                + jp.getS_nameThumbnails()
                + "</a> "
                + "</p>");

        doc.write(filesString.toString());
    }

    private void renderDirectoryList(final HTMLDocument doc, final String path, final File baseDirectory) {
        StringBuilder filesString = new StringBuilder();
        StringBuilder directories = new StringBuilder();
        File[] files = baseDirectory.listFiles();
        if (files == null) {
            doc.writeln("<div class=\"alert alert-danger\" role=\"alert\">"
                    + "<strong>Oh snap!</strong> Unable to read files.</div>");
        } else {
            if (files.length == 0) {
                doc.writeln("<div class=\"alert alert-info\" role=\"alert\">There are no files in this directory.</div>");
            } else {
                for (File file : files) {
                    if (file.isDirectory()) {
                        directories
                                .append("<p class=\"filemanager\"><img src=\"/public/img/folder.png\""
                                        + " alt=\"folder\" /> <a href=\"/gallery?"
                                        + StringUtilities.urlEncode(path
                                        + file.getName() + "/")
                                        + "\">"
                                        + file.getName() + "</a></p>");
                    } else {
                        //filesString.append("<p class=\"filemanager\"><img src=\"/assets/img/"
                        //        + MAPPER.getIconRelativePath(FileUtilities.getExtension(file.getName()))
                        //        + "\" alt=\"file\" /> <a href=\"/admin/GetFile?"
                        //        + StringUtilities.urlEncode(path + file.getName())
                        //        + "\">"
                        //        + file.getName()
                        //        + "</a> "
                        //        + FileUtilities.fileSizeUnits(file.length())
                        //        + "</p>");
                    }
                }
            }
        }
        doc.write(directories.toString());
        doc.write(filesString.toString());
    }

    private void renderPathNotAvailable(final HTMLDocument doc) {
        doc.writeln("<div class=\"alert alert-danger\" role=\"alert\"><strong>Oh snap!</strong> "
                + "Path does not exist or drive not mounted.</div>");
    }
}
