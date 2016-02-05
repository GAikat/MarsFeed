package uk.co.gaik.marsfeed;

import java.util.ArrayList;

/**
 * Created by George on 4/2/2016.
 */
public class Rover {
    String id;
    String name;
    String landingDate;
    String maxSol;
    String maxDate;
    String totalPhotos;
    ArrayList<RoverCamera> cameras;

    public Rover(String id, String name, String landingDate, String maxSol, String maxDate, String totalPhotos, ArrayList<RoverCamera> cameras){
        this.id = id;
        this.name = name;
        this.landingDate = landingDate;
        this.maxSol = maxSol;
        this.maxDate = maxDate;
        this.totalPhotos = totalPhotos;
        this.cameras = cameras;
    }
}
