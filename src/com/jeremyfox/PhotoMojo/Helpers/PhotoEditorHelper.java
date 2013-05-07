package com.jeremyfox.PhotoMojo.Helpers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.aviary.android.feather.FeatherActivity;
import com.jeremyfox.PhotoMojo.Activities.BaseActivity;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 5/6/13
 * Time: 9:49 PM
 */
public abstract class PhotoEditorHelper {

    public static final String API_KEY = "13iOH2xfbkSGJ_9MUBpxEQ";

    public static void launchEditorWithImageAtUri(Activity activity, Uri imageUri) {
        if (null != activity && null != imageUri) {
            Intent newIntent = new Intent( activity, FeatherActivity.class );
            newIntent.setData( imageUri );
            newIntent.putExtra( "API_KEY", API_KEY );

            // pass the uri of the destination image file (optional)
            // This will be the same uri you will receive in the onActivityResult
            // newIntent.putExtra( "output", Uri.parse( "file://" + mOutputFile.getAbsolutePath() ) );

            // format of the destination image (optional)
            // newIntent.putExtra( "output-format", Bitmap.CompressFormat.JPEG.name() );

            // output format quality (optional)
            // newIntent.putExtra( "output-quality", 85 );

            // you can force feather to display only a certain tools
            // newIntent.putExtra( "tools-list", new String[]{"ADJUST", "BRIGHTNESS" } );

            // enable fast rendering preview
            newIntent.putExtra( "effect-enable-fast-preview", true );

            // limit the image size
            // You can pass the current display size as max image size because after
            // the execution of Aviary you can save the HI-RES image so you don't need a big
            // image for the preview
            // newIntent.putExtra( "max-image-size", 800 );

            // HI-RES
            // You need to generate a new session id key to pass to Aviary feather
            // this is the key used to operate with the hi-res image ( and must be unique for every new instance of Feather )
            // The session-id key must be 64 char length
            // String mSessionId = StringUtils.getSha256(System.currentTimeMillis() + API_KEY);
            // newIntent.putExtra( "output-hires-session-id", mSessionId );

            // you want to hide the exit alert dialog shown when back is pressed
            // without saving image first
            // newIntent.putExtra( "hide-exit-unsave-confirmation", true );

            // -- VIBRATION --
            // Some aviary tools use the device vibration in order to give a better experience
            // to the final user. But if you want to disable this feature, just pass
            // any value with the key "tools-vibration-disabled" in the calling intent.
            // This option has been added to version 2.1.5 of the Aviary SDK
            // newIntent.putExtra( Constants.EXTRA_TOOLS_DISABLE_VIBRATION, true );

            // ..and start feather
            activity.startActivityForResult(newIntent, BaseActivity.EDIT_PHOTO);
        }
    }

}
