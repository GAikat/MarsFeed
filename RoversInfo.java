package uk.co.gaik.marsfeed;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by George on 9/12/2015.
 */
public class RoversInfo extends AppCompatActivity {
    private Spinner rovers;
    private TextView landingDate, lastPhoto, martianDays, numberPhotos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rovers_info);

        rovers = (Spinner) findViewById(R.id.roversSpinner);
        landingDate = (TextView) findViewById(R.id.landingDateTextView);
        lastPhoto = (TextView) findViewById(R.id.lastPhotoTextView);
        martianDays = (TextView) findViewById(R.id.solTextView);
        numberPhotos = (TextView) findViewById(R.id.numberPhotosTextView);
    }
}
