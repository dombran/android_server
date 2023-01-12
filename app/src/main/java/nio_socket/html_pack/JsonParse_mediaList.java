package nio_socket.html_pack;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonParse_mediaList {
    private String  s_linkFile;
    private String  s_nameFile;
    private String  s_nameThumbnails;
    private int     i_numThumb;

    private String h_linkFile = "linkFile";
    private String h_nameFile = "nameFile";
    private String h_nameThumbnails = "nameThumbnails";
    private String h_numThumb = "numThumb";

    public JsonParse_mediaList(String s_linkFile, String nameFile , String s_nameThumbnails, int i_numThumb){
        this.s_linkFile = s_linkFile;
        this.s_nameFile = nameFile;
        this.s_nameThumbnails = s_nameThumbnails;
        this.i_numThumb = i_numThumb;
    }

    public JsonParse_mediaList(String s_linkFile, String nameFile , String s_nameThumbnails){
        this.s_linkFile = s_linkFile;
        this.s_nameFile = nameFile;
        this.s_nameThumbnails = s_nameThumbnails;
        this.i_numThumb = 0;
    }

    public JSONObject get_JSONObject() throws JSONException {
        JSONObject jb = new JSONObject();
        jb.put(h_linkFile, s_linkFile);
        jb.put(h_nameFile, s_nameFile);
        jb.put(h_nameThumbnails, s_nameThumbnails);
        jb.put(h_numThumb,i_numThumb);
        return jb;
    }

    public String getS_nameFile(){ return this.s_nameFile; }

    public String getS_linkFile() {
        return this.s_linkFile;
    }

    public void setS_linkFile(String s_linkFile) {
        this.s_linkFile = s_linkFile;
    }

    public String getS_nameThumbnails() {
        return this.s_nameThumbnails;
    }

    public void setS_linkThumbnails(String s_linkThumbnails) {
        this.s_nameThumbnails = s_linkThumbnails;
    }

    public int getI_numThumb() {
        return this.i_numThumb;
    }

    public void setI_numThumb(int i_num) {
        this.i_numThumb = i_numThumb;
    }

}
