package nio_socket.html_pack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class JsonParse_mediaList_ext  {
    private ArrayList<JsonParse_mediaList> mList = new ArrayList<>();
    private int i_max_numThumb = 0;
    private String nameFile;
    private JSONObject json_obj;
    private int item_num = 0;

    public JsonParse_mediaList_ext(String s_json_name) throws IOException, JSONException {

        File fl = new File(s_json_name);
        nameFile = s_json_name;

        if(!fl.exists()){
            //fl.createNewFile();
            save_toFile(" { \"thumb\" : [] } ");
        }
            String str_json = getFileContent( new FileInputStream(fl), StandardCharsets.UTF_8.name());

            if(str_json == ""){
                str_json = " { \"thumb\" : [] } ";
            }

            json_obj = new JSONObject(str_json);

            JSONArray arr = json_obj.getJSONArray("thumb");
            item_num = arr.length() - 1 ;
            for(int i=0; i<item_num; i++){
                if(i_max_numThumb < arr.getJSONObject(i).getInt("numThumb"))
                    i_max_numThumb = arr.getJSONObject(i).getInt("numThumb");

                mList.add( new JsonParse_mediaList(
                        arr.getJSONObject(i).getString("linkFile"),
                        arr.getJSONObject(i).getString("nameFile"),
                        arr.getJSONObject(i).getString("nameThumbnails"),
                        arr.getJSONObject(i).getInt("numThumb")
                ) );
            }

    }

    public void add_item_once(JsonParse_mediaList ml) {
        File fl = new File(nameFile);

        try (FileOutputStream fos = new FileOutputStream(fl)) {

            JSONArray arr = json_obj.getJSONArray("thumb");

            if(ml.getI_numThumb() == 0 )
                ml.setI_numThumb(i_max_numThumb + 1);

            arr.put(ml.get_JSONObject());

            item_num = arr.length();

            if(json_obj.has("thumb"))
                json_obj.remove("thumb");
            json_obj.put("thumb",arr);

            String text = json_obj.toString(2);
            byte[] mybytes = text.getBytes();

            fos.write(mybytes);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void add_next(JsonParse_mediaList ml){
        try {
            JSONArray arr = json_obj.getJSONArray("thumb");

            if(ml.getI_numThumb() == 0 )
                ml.setI_numThumb(i_max_numThumb + 1);

            arr.put(ml.get_JSONObject());

            item_num = arr.length();

            //if(json_obj.has("thumb"))
            //    json_obj.remove("thumb");
            //json_obj.put("thumb",arr);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void save_toFile(){
        File fl = new File(nameFile);

        if(fl.exists())
            fl.delete();

        try (FileOutputStream fos = new FileOutputStream(fl)) {
            String text = json_obj.toString(2);
            byte[] mybytes = text.getBytes();

            fos.write(mybytes);
            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void save_toFile(String str){
        File fl = new File(nameFile);
        if(fl.exists())
            fl.delete();

        try (FileOutputStream fos = new FileOutputStream(fl)) {
            String text = str;
            byte[] mybytes = text.getBytes();

            fos.write(mybytes);
            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileContent( FileInputStream fis, String encoding ) throws IOException
    {
        try( BufferedReader br = new BufferedReader( new InputStreamReader(fis, encoding )))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while(( line = br.readLine()) != null ) {
                sb.append( line );
                sb.append( '\n' );
            }
            return sb.toString();
        }
    }

    public int getI_max_numThumb(){
        return this.i_max_numThumb;
    }
    public void setI_max_numThumb(int i_num){
        this.i_max_numThumb = i_num;
    }

    public int getI_itemNum() {
        return item_num ;
    }
    public ArrayList<JsonParse_mediaList> get_Object(){
        return this.mList;
    }

    public void add(JsonParse_mediaList l){
        mList.add( l );
    }

    public int get_size(){
        return this.mList.size();
    }

    public JsonParse_mediaList get(int i){
        return mList.get(i);
    }

    public JsonParse_mediaList getBegin(){
        JsonParse_mediaList jp = mList.get(0);
        mList.remove(0);
        return jp;
    }

}
