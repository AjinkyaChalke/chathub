package edu.sfsu.csc780.chathub;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by cjkriese on 3/2/18.
 */

public class ImageUtil {
    private static final String TAG = "ImageUtil";
    public static final double MAX_LINEAR_DIMENSION = 200.0;
    public static final String IMAGE_FILE_NAME_PREFIX = "chathub-";

    public static Bitmap getBitmapForUri(Uri imageUri, Context context) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static String getExtensionForUri(Uri uri, Context context){
        String fileName;
        String[] filePathColumn = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn,
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            fileName = cursor.getString(columnIndex);
        }
        else{
            fileName = "";
        }
        if(cursor != null){
            cursor.close();
        }
        return fileName;
    }

    public static Bitmap scaleImage(Bitmap bitmap) {
        int originalHeight = bitmap.getHeight();
        int originalWidth = bitmap.getWidth();
        double scaleFactor = MAX_LINEAR_DIMENSION / (double)(originalHeight + originalWidth);
        // We only want to scale down images, not scale upwards
        if (scaleFactor < 1.0) {
            int targetWidth = (int) Math.round(originalWidth * scaleFactor);
            int targetHeight = (int) Math.round(originalHeight * scaleFactor);
            return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        } else {
            return bitmap;
        }
    }

    public static Uri savePhotoImage(Bitmap imageBitmap, Context context) {
        File photoFile = null;
        try {
            photoFile = createFile(context, ".jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (photoFile == null) {
            Log.d(TAG, "Error creating media file");
            return null;
        }
        try {
            FileOutputStream fos = new FileOutputStream(photoFile);
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return Uri.fromFile(photoFile);
    }

    static File createFile(Context context, String extension) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date());
        String imageFileNamePrefix = IMAGE_FILE_NAME_PREFIX + timeStamp;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileNamePrefix, /* prefix */
                extension, /* suffix */
                storageDir /* directory */
        );
    }

    public static Uri saveCustomFile(Uri uri, Context context, String extension){
        File webpFile = null;
        try {
            webpFile = createFile(context, extension);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (webpFile == null) {
            Log.d(TAG, "Error creating "+extension+" file");
            return null;
        }
        try {


            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream fileOutputStream = new FileOutputStream(webpFile);
            byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;

            if (inputStream != null) {
                while ((read = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                }
            }
            if (inputStream != null) {
                inputStream.close();
            }
            fileOutputStream.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(webpFile);
    }
}
