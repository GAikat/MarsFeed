package uk.co.gaik.marsfeed;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.LruCache;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    // API key assigned to developer: georgios.aikaterinakis@gmail.com to consume the NASA Open APIs
    private static final String api_key = "N2YU4ML6VHuDUVPYfIl9CvLCmyLPsGamcDTAyFw2";

    // Remove the below line after defining your own ad unit ID.
    private static final String TOAST_TEXT = "Test ads are being shown. "
            + "To show live ads, replace the ad unit ID in res/values/strings.xml with your own ad unit ID.";

    // Memory LRU Cache
    public static LruCache<String, Bitmap> mMemoryCache;

    public static String url;
    public static String maxEarthDate;
    public static String selectedDate;
    public static String selectedRover = "Curiosity";
    ArrayList<Photo> photos = new ArrayList<Photo>();
    //ArrayList<CharSequence> cameras = new ArrayList<>();

    // UI widgets
    Spinner mRoversSpinner;
    public static Button dateButton;
    //Spinner mCameraSpinner;
    ListView photosListView;

    PhotoAdapter photoAdapter;

    public static int screenWidth;
    public static int screenHeight;
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Getting display width and height in pixels
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // saving the application context for use in the PhotoAdapter
        context = getApplicationContext();

        // Get the max available VM memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use half of available memory for the Cache
        final int cacheSize = maxMemory / 2;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        /* Check if there are saved data to load, else send one request for each rover to get their info. */
        /* Also get info for Mars to show in a different page */

        photosListView = (ListView) findViewById(R.id.photosListView);
        mRoversSpinner = (Spinner) findViewById(R.id.roversSpinner);
        dateButton = (Button) findViewById(R.id.dateButton);
        //mCameraSpinner = (Spinner) findViewById(R.id.cameraSpinner);

        photoAdapter = new PhotoAdapter(getApplicationContext(), R.layout.photo_list_item, photos);
        photosListView.setAdapter(photoAdapter);
        View footerView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer, null, false);
        photosListView.addFooterView(footerView);
        View emptyView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.empty_list, null, false);
        photosListView.setEmptyView(emptyView);

        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);

        url ="https://api.nasa.gov/mars-photos/api/v1/rovers/"+selectedRover+"/photos?sol=1&api_key=DEMO_KEY";

        // Request a string response from the provided URL.
        /* mallon peritto
        final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Parse max earth date
                    parseMaxDate(response, queue);
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // There is a problem with the connection.
                Toast.makeText(getApplicationContext(), "Communication with Mars failed!", Toast.LENGTH_LONG).show();
                if(dateButton.getText().toString().equals("")){
                    dateButton.setText("Select Date");
                }
                View footerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer, null, false);
                photosListView.removeFooterView(footerView);
            }
        });
        queue.add(stringRequest); */


        ArrayAdapter<CharSequence> roversAdapter = ArrayAdapter.createFromResource(this,
                R.array.rovers_array, android.R.layout.simple_spinner_item);
        roversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRoversSpinner.setAdapter(roversAdapter);
        mRoversSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRover = parent.getSelectedItem().toString();
                photos.clear();
                photoAdapter.notifyDataSetChanged();

                // get the max earth date for this rover
                url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?sol=1&api_key=DEMO_KEY";
                final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            parseMaxDate(response, queue);
                        }
                    }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // There is a problem with the connection.
                        Toast.makeText(getApplicationContext(), "Communication with Mars failed!", Toast.LENGTH_LONG).show();
                        if(dateButton.getText().toString().equals("")){
                            dateButton.setText("");
                            dateButton.setEnabled(false);
                        }
                        //View footerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer, null, false);
                        //photosListView.removeFooterView(footerView);
                    }
                });
                queue.add(stringRequest);

                // use the max earth date to send a request and populate the list
                //url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?earth_date="+maxEarthDate+"&api_key=DEMO_KEY";
                //MyStringRequest request = new MyStringRequest();
                //queue.add(request.request);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /*ArrayAdapter<CharSequence> cameraAdapter = ArrayAdapter.createFromResource(this,
                , android.R.layout.simple_spinner_item);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCameraSpinner.setAdapter(cameraAdapter);
        mCameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });*/

        // Load an ad into the AdMob banner view.
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Parses the JSON response from the HTTP request
    private void parse(String response){
        String[] tokens = response.split("\\{\"photos\":\\[");

        // contains No Results
        if(tokens[0].contains("error") || (tokens.length > 1 && tokens[1].equals("]}"))){
            // Do nothing - Go to the last part of the function
        }
        else {
            // split at camera if in need on showing it
            String[] tokens2 = tokens[1].split("\"\\},\"img_src\":\"");

            String[] cameraTokens = tokens2[0].split("camera");
            String[] cameraTokens2 = cameraTokens[cameraTokens.length-1].split("full_name\":\"");
            String camera = cameraTokens2[cameraTokens2.length-1];
            Photo p = new Photo();
            p.setCamera(camera);
            photos.add(p);

            // start from 1 - avoid useless info
            for (int i = 1; i < tokens2.length; i++) {
                String tok = tokens2[i];
                String[] tokens3 = tok.split("\",\"earth_date\":\"");
                String url = tokens3[0];
                String[] tokens4 = tokens3[1].split("\",\"rover");
                String date = tokens4[0];

                p = photos.get(photos.size()-1);
                photos.set(photos.size()-1, p);
            }
        }

        // Add/remove footer and empty view
        if(photos.size()%25 != 0 || photos.size() == 0){
            View footerView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer, null, false);
            photosListView.removeFooterView(footerView);
        }
    }

    // Parses the JSON response from the HTTP request
    private void parseMaxDate(String response, RequestQueue queue){
        String[] tokens = response.split("max_date\":\"");
        String[] tokens2 = tokens[1].split("\",\"");
        maxEarthDate = tokens2[0];
        selectedDate = maxEarthDate;
        dateButton.setText(maxEarthDate);
        StringRequest request = new StringRequest(Request.Method.GET, url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?earth_date="+selectedDate+"&api_key=DEMO_KEY",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Parser
                        parse(response);
                        photoAdapter.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // There is a problem with the connection.
                Toast.makeText(MainActivity.context, "Communication with Mars failed!", Toast.LENGTH_LONG).show();
            }
        });
        queue.add(request);
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            dateButton.setText(year+"-"+(month+1)+"-"+day);
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

}

