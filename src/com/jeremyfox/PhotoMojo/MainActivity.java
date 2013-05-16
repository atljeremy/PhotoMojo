package com.jeremyfox.PhotoMojo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.jeremyfox.PhotoMojo.Activities.BaseActivity;
import com.jeremyfox.PhotoMojo.Helpers.FileStorageHelper;
import com.jeremyfox.PhotoMojo.Helpers.NotificationHelper;
import com.jeremyfox.PhotoMojo.Helpers.PhotoEditorHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/LANENAR_.ttf");
        ((TextView)findViewById(R.id.photoMojoLogo)).setTypeface(typeface);

        mImageView = (ImageView) findViewById(R.id.background);
        if (FileStorageHelper.hasLatestPhoto(this)) {
            Bitmap latestPhoto = FileStorageHelper.getLatestPhoto(this);
            if (null != latestPhoto) mImageView.setImageBitmap(latestPhoto);
        }

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

    @Override
    public void onStop() {
        super.onStop();
        NotificationHelper.setNotificationTimer(this.getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.share_note:
                Bitmap photo = FileStorageHelper.getLatestPhoto(this);
                if (null != photo) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My New PhotoMojo Photo");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my new Photo edited with the PhotoMojo Android app.");
                    shareIntent.setType("image/jpeg");
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    File f = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.jpg");
                    try {
                        f.createNewFile();
                        FileOutputStream fo = new FileOutputStream(f);
                        fo.write(bytes.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/temporary_file.jpg"));
                    startActivity(Intent.createChooser(shareIntent, "Share Photo"));
                } else {
                    Toast.makeText(this, "Please take or import a photo before trying to share.", Toast.LENGTH_LONG).show();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
