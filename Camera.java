package uk.co.gaik.marsfeed;

/**
 * Created by George on 4/2/2016.
 */
public class Camera {
    String id;
    String name;
    String rover_id;
    String full_name;

    public Camera(String id, String name, String rover_id, String full_name){
        this.id = id;
        this.name = name;
        this.rover_id = rover_id;
        this.full_name = full_name;
    }
}
