package com.jeremyfox.PhotoMojo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.jeremyfox.PhotoMojo.Activities.BaseActivity;
import com.jeremyfox.PhotoMojo.Helpers.PhotoEditorHelper;

public class MainActivity extends BaseActivity {

    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                }
            };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mImageView = (ImageView) findViewById(R.id.background);

        Button takePhotoButton = (Button) findViewById(R.id.take_photo_button);
        setBtnListenerOrDisable(
                takePhotoButton,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        Button importPhotoButton = (Button) findViewById(R.id.import_from_lib_button);
        importPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
            }
        });

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        Uri uri = null;
        if (null != bundle) {
            uri = (Uri)bundle.get(Intent.EXTRA_STREAM);
            if (null != uri) {
                mCurrentPhotoPath = getRealPathFromURI(uri);
                setPic();
                PhotoEditorHelper.launchEditorWithImageAtUri(this, uri);
            }
        }
    }
}
