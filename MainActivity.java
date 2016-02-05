package uk.co.gaik.marsfeed;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    // API key assigned to developer: georgios.aikaterinakis@gmail.com to consume the NASA Open APIs
    private static final String api_key = "N2YU4ML6VHuDUVPYfIl9CvLCmyLPsGamcDTAyFw2";

    // Remove the below line after defining your own ad unit ID.
    private static final String TOAST_TEXT = "Test ads are being shown. "
            + "To show live ads, replace the ad unit ID in res/values/strings.xml with your own ad unit ID.";

    // JSON Node names
    private static final String TAG_PHOTOS = "photos";
    private static final String TAG_ID = "id";
    private static final String TAG_SOL = "sol";
    private static final String TAG_CAMERA = "camera";
    private static final String TAG_NAME = "name";
    private static final String TAG_ROVER_ID = "rover_id";
    private static final String TAG_FULL_NAME = "full_name";
    private static final String TAG_IMAGE = "img_src";
    private static final String TAG_EARTH_DATE = "earth_date";
    private static final String TAG_ROVER = "rover";
    private static final String TAG_LANDING_DATE = "landing_date";
    private static final String TAG_MAX_SOL = "max_sol";
    private static final String TAG_MAX_DATE = "max_date";
    private static final String TAG_TOTAL_PHOTOS = "total_photos";
    private static final String TAG_CAMERAS = "cameras";

    // photos JSONArray
    JSONArray photosJSON = null;

    // Memory LRU Cache
    public static LruCache<String, Bitmap> mMemoryCache;

    public static RequestQueue queue;
    public static GetPhotos getPhotos;

    public static String url;
    public static String maxEarthDate;
    public static String selectedDate;
    public static String selectedRover = "Curiosity";
    public static ArrayList<Photo> photos = new ArrayList<Photo>();

    // UI widgets
    private ProgressDialog pDialog;

    Spinner mRoversSpinner;
    public static Button dateButton;
    ListView photosListView;

    public static PhotoAdapter photoAdapter;

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
        getPhotos = new GetPhotos();

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

        photoAdapter = new PhotoAdapter(getApplicationContext(), R.layout.photo_list_item, photos);
        photosListView.setAdapter(photoAdapter);
        photosListView.setEmptyView(findViewById(R.id.empty_list_item));

        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(this);

        url ="https://api.nasa.gov/mars-photos/api/v1/rovers/"+selectedRover+"/photos?sol=1&api_key=DEMO_KEY";

        ArrayAdapter<CharSequence> roversAdapter = ArrayAdapter.createFromResource(this,
                R.array.rovers_array, android.R.layout.simple_spinner_item);
        roversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRoversSpinner.setAdapter(roversAdapter);
        mRoversSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRover = parent.getSelectedItem().toString();
                selectedDate = "";

                updateList(queue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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
    protected void onResume() {
        super.onResume();
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

    /**
     * Function used to communicate with the NASA Api, get the JSON response and parse it and show it to the user
     **/
    public static void updateList(RequestQueue queue){
        photos.clear();
        photoAdapter.notifyDataSetChanged();

        if(selectedDate.equals("")) {
            url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?sol=1&api_key=DEMO_KEY";
        }
        else{
            url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?earth_date=" + selectedDate + "&api_key=DEMO_KEY";
        }

        final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    getPhotos.execute(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // There is a problem with the connection.
                    Toast.makeText(context, "Communication with Mars failed!", Toast.LENGTH_LONG).show();
                    if(dateButton.getText().toString().equals("")){
                        dateButton.setText("");
                        dateButton.setEnabled(false);
                    }
                }
            });
        queue.add(stringRequest);
    }

    /**
     * Async task class to parse the json response and fill the ArrayList with photos
     * */
    private class GetPhotos extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Contacting NASA...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... arg0) {
            // Creating service handler class instance
            //ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String jsonStr = arg0[0]; //sh.makeServiceCall(url, ServiceHandler.GET);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    photosJSON = jsonObj.getJSONArray(TAG_PHOTOS);

                    // looping through All Contacts
                    for (int i = 0; i < photosJSON.length(); i++) {
                        JSONObject photo = photosJSON.getJSONObject(i);

                        String id = photo.getString(TAG_ID);
                        String sol = photo.getString(TAG_SOL);

                        JSONObject camera = photo.getJSONObject(TAG_CAMERA);
                        String camId = camera.getString(TAG_ID);
                        String camName = camera.getString(TAG_NAME);
                        String camRoverId = camera.getString(TAG_ROVER_ID);
                        String camFullName = camera.getString(TAG_FULL_NAME);
                        Camera cam = new Camera(camId, camName, camRoverId, camFullName);
                        String imgSrc = photo.getString(TAG_IMAGE);
                        String earthDate = photo.getString(TAG_EARTH_DATE);
                        selectedDate = earthDate;

                        JSONObject rover = photo.getJSONObject(TAG_ROVER);
                        String roverId = rover.getString(TAG_ID);
                        String roverName = rover.getString(TAG_NAME);
                        String roverLandingDate = rover.getString(TAG_LANDING_DATE);
                        String roverMaxSol = rover.getString(TAG_MAX_SOL);
                        String roverMaxDate = rover.getString(TAG_MAX_DATE);
                        maxEarthDate = roverMaxDate;
                        String roverTotalPhotos = rover.getString(TAG_TOTAL_PHOTOS);
                        JSONArray roverCameras = rover.getJSONArray(TAG_CAMERAS);
                        ArrayList<RoverCamera> roverCamerasList = new ArrayList<>();
                        for(int j = 0; j < roverCameras.length(); j++){
                            JSONObject c = roverCameras.getJSONObject(j);

                            String cName = c.getString(TAG_NAME);
                            String cFullName = c.getString(TAG_FULL_NAME);

                            roverCamerasList.add(new RoverCamera(cName, cFullName));
                        }
                        Rover r = new Rover(roverId, roverName, roverLandingDate, roverMaxSol, roverMaxDate, roverTotalPhotos, roverCamerasList);
                        Photo p = new Photo(id, sol, cam, imgSrc, earthDate, r);
                        photos.add(p);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            dateButton.setText(selectedDate);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            /**
             * Updating parsed JSON data into ListView
             * */
            photoAdapter.notifyDataSetChanged();
            getPhotos = new GetPhotos();
        }

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
            selectedDate = year+"-"+(month+1)+"-"+day;
            updateList(queue);
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

}

