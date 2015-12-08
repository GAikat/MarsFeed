package uk.co.gaik.marsfeed;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.renderscript.ScriptGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by George on 5/12/2015.
 */
public class PhotoAdapter extends ArrayAdapter<Photo> {
    Context context;
    int layoutResourceId;
    ArrayList<Photo> data = null;

    View row;

    public PhotoAdapter(Context context, int layoutResourceId, ArrayList<Photo> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        row = convertView;
        LayoutInflater inflater;

        if(row == null){
            inflater = LayoutInflater.from(context);
             //= ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
        }

        TextView date = (TextView) row.findViewById(R.id.dateTextView);
        ImageView image = (ImageView) row.findViewById(R.id.imageView);

        date.setText(data.get(position).getDate());
        loadBitmap(data.get(position).getUrl(), image);


        return row;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String url;

        public DownloadImageTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        protected Bitmap doInBackground(String... params) {
            url = params[0];
            final Bitmap bitmap = decodeSampledBitmapFromResource(url, MainActivity.screenWidth, MainActivity.screenHeight);
            addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final DownloadImageTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(String url, int reqWidth, int reqHeight) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            InputStream is2 = (InputStream) new URL(url).getContent();
            return BitmapFactory.decodeStream(is2, null, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        // Only width is important since the app is shown only on portrait mode
        if (height > reqHeight || width > reqWidth){
        //if (width > reqWidth){
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight/inSampleSize) > reqHeight && (halfWidth/inSampleSize) > reqWidth){
            //while ((halfWidth/inSampleSize) > reqWidth){
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<DownloadImageTask> imageWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             DownloadImageTask bitmapWorkerTask) {
            super(res, bitmap);
            imageWorkerTaskReference =
                    new WeakReference<DownloadImageTask>(bitmapWorkerTask);
        }

        public DownloadImageTask getBitmapWorkerTask() {
            return imageWorkerTaskReference.get();
        }
    }

    public void loadBitmap(String url, ImageView imageView) {
        final Bitmap bitmap = getBitmapFromMemCache(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            if (cancelPotentialWork(url, imageView)) {
                final DownloadImageTask task = new DownloadImageTask(imageView);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(MainActivity.context.getResources(), BitmapFactory.decodeResource(MainActivity.context.getResources(), R.mipmap.ic_launcher), task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(url);
            }
        }
    }

    public static boolean cancelPotentialWork(String url, ImageView imageView) {
        final DownloadImageTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapUrl = bitmapWorkerTask.url;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapUrl == null || bitmapUrl != url) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static DownloadImageTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    // Cache functions
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            MainActivity.mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return MainActivity.mMemoryCache.get(key);
    }
}

