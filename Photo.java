package uk.co.gaik.marsfeed;

/**
 * Created by Georgios Aikaterinakis on 5/12/2015.
 */
public class Photo {
    String id;
    String sol;
    Camera cam;
    String imgSrc;
    String earthDate;
    Rover rover;

    public Photo(String id, String sol, Camera cam, String imgSrc, String earthDate, Rover rover){
        this.id = id;
        this.sol = sol;
        this.cam = cam;
        this.imgSrc = imgSrc;
        this.earthDate = earthDate;
        this.rover = rover;
    }
}
