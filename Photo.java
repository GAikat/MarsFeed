package uk.co.gaik.marsfeed;

import android.graphics.Bitmap;

/**
 * Created by Georgios Aikaterinakis on 5/12/2015.
 */
public class Photo {
    private String url;
    private String date;
    private Bitmap photo;

    // Constructor of empty Photo
    public Photo(){
        this.url = null;
        this.date = null;
        this.photo = null;
    }

    // Constructor with data
    public Photo(String u, String d, Bitmap p){
        this.url = u;
        this.date = d;
        this.photo = p;
    }

    // Copy-constructor
    public Photo(Photo p){
        this.url = p.getUrl();
        this.date = p.getDate();
        this.photo = p.getPhoto();
    }

    // Setters
    void setDate(String d){
        this.date = d;
    }
    void setUrl(String u){
        this.url = u;
    }
    void setPhoto(Bitmap p){
        this.photo = p;
    }

    // Getters
    String getDate(){
        return this.date;
    }
    String getUrl(){
        return this.url;
    }
    Bitmap getPhoto(){
        return this.photo;
    }
}
