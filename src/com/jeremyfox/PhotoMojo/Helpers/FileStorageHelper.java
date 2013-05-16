package com.jeremyfox.PhotoMojo.Helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.jeremyfox.PhotoMojo.R;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 5/13/13
 * Time: 10:17 PM
 */
public abstract class FileStorageHelper {

    private static final String FILE_NAME = "PhotoMojoPicture.jpg";

    /**
     * Gets album name.
     *
     * @param context the context
     * @return the album name
     */
    public static String getAlbumName(Context context) {
        return context.getString(R.string.album_name);
    }

    /**
     * Saves the latest photo.
     *
     * @param context the context
     * @param image the image
     * @return the boolean
     */
    public static boolean saveLatestPhoto(Context context, Bitmap image) {
        File path = context.getExternalFilesDir(null);
        String fullPath = path.getAbsolutePath() + getAlbumName(context);
        File file = new File(fullPath, FILE_NAME);

        try {

            deleteLatestPhoto(context);
            File pathTemp = new File(fullPath);
            if (!pathTemp.exists()) {
                pathTemp.mkdirs();
            }
            pathTemp = null;

            OutputStream outputStream = null;
            file.createNewFile();
            outputStream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 80, outputStream);
            outputStream.flush();
            outputStream.close();

            return true;

        } catch (Exception e) {
            Log.w("ExternalStorage", "Error writing " + file, e);

            return false;
        }
    }

    /**
     * Gets latest photo.
     *
     * @param context the context
     * @return the latest photo
     */
    public static Bitmap getLatestPhoto(Context context) {
        File path = context.getExternalFilesDir(null);
        String fullPath = path.getAbsolutePath() + getAlbumName(context);
        File file = new File(fullPath, FILE_NAME);
        Bitmap latestPhoto = null;

        try {
            if (hasLatestPhoto(context) == true) {
                FileInputStream fis = new FileInputStream(file);
                latestPhoto = BitmapFactory.decodeStream(fis);
                fis.close();
            }
        } catch (Exception e) {
            Log.e("getLatestPhoto", e.getMessage());
        }

        return latestPhoto;
    }

    /**
     * Deletes the latest photo. If external
     * storage is not currently mounted this will fail.
     *
     * @param context the context
     */
    public static void deleteLatestPhoto(Context context) {
        File path = context.getExternalFilesDir(null);
        String fullPath = path.getAbsolutePath() + getAlbumName(context);
        File file = new File(fullPath, FILE_NAME);
        if (file != null) {
            file.delete();
        }
    }

    /**
     * Determines if there is an existent "latest photo". If external
     * storage is not currently mounted this will fail.
     *
     * @param context the context
     * @return the boolean
     */
    public static boolean hasLatestPhoto(Context context) {
        File path = context.getExternalFilesDir(null);
        String fullPath = path.getAbsolutePath() + getAlbumName(context);
        File file = new File(fullPath, FILE_NAME);
        if (file != null) {
            return file.exists();
        }
        return false;
    }

    public static String getLatestPhotoURL(Context context) {
        File path = context.getExternalFilesDir(null);
        String fullPath = path.getAbsolutePath() + getAlbumName(context) + "/" + FILE_NAME;
        return fullPath;
    }

}
