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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    // API key assigned to developer: georgios.aikaterinakis@gmail.com to consume the NASA APIs
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

    // UI widgets
    Spinner mRoversSpinner;
    public static Button dateButton;
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

        url ="https://api.nasa.gov/mars-photos/api/v1/rovers/"+selectedRover+"/photos?sol=1&api_key=DEMO_KEY";

        photosListView = (ListView) findViewById(R.id.photosListView);
        mRoversSpinner = (Spinner) findViewById(R.id.rovers_spinner);
        dateButton = (Button) findViewById(R.id.dateButton);

        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Parse max earth date
                    MyStringRequest request = new MyStringRequest();
                    parseMaxDate(response, queue, request);
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // There is a problem with the connection.
                Toast.makeText(getApplicationContext(), "Communication with Mars failed!", Toast.LENGTH_LONG).show();
            }
        });
        queue.add(stringRequest);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.rovers_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRoversSpinner.setAdapter(adapter);
        mRoversSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRover = parent.getSelectedItem().toString();
                photos.clear();
                photoAdapter.notifyDataSetChanged();

                // get the max earth date for this rover
                url ="https://api.nasa.gov/mars-photos/api/v1/rovers/"+selectedRover+"/photos?sol=1&api_key=DEMO_KEY";
                final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                MyStringRequest request = new MyStringRequest();
                                parseMaxDate(response, queue, request);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // There is a problem with the connection.
                        Toast.makeText(getApplicationContext(), "Communication with Mars failed!", Toast.LENGTH_LONG).show();
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


        photoAdapter = new PhotoAdapter(getApplicationContext(), R.layout.photo_list_item, photos);
        photosListView.setAdapter(photoAdapter);
        View footerView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer, null, false);
        photosListView.addFooterView(footerView);
        View emptyView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.empty_list, null, false);
        photosListView.setEmptyView(emptyView);

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

        // split at camera if in need on showing it
        String[] tokens2 = tokens[1].split("img_src\":\"");

        // start from 1 - avoid useless info
        for(int i=1; i<tokens2.length; i++){
            String tok = tokens2[i];
            String[] tokens3 = tok.split("\",\"earth_date\":\"");
            String url = tokens3[0];
            String[] tokens4 = tokens3[1].split("\",\"rover");
            String date  = tokens4[0];

            Photo p = new Photo(url, date, null);
            photos.add(p);
        }
    }

    // Parses the JSON response from the HTTP request
    private void parseMaxDate(String response, RequestQueue queue, MyStringRequest request){
        String[] tokens = response.split("max_date\":\"");
        String[] tokens2 = tokens[1].split("\",\"");
        maxEarthDate = tokens2[0];
        selectedDate = maxEarthDate;
        dateButton.setText(maxEarthDate);
        request.request = new StringRequest(Request.Method.GET, url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?earth_date="+selectedDate+"&api_key=DEMO_KEY",
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
        queue.add(request.request);
    }

    // class that produces a new StringRequest
    class MyStringRequest{
        StringRequest request;
        public MyStringRequest(){
            request = new StringRequest(Request.Method.GET, url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + selectedRover + "/photos?earth_date="+selectedDate+"&api_key=DEMO_KEY",
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
            dateButton.setText(year+"-"+(month+1)+"-"+day);
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

    class SendRequest implements Runnable {

        public SendRequest() {

        }

        @Override
        public void run() {
        }
    }
}

