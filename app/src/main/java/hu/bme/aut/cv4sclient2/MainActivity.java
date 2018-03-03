package hu.bme.aut.cv4sclient2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.ContentBody;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;
import cz.msebera.android.httpclient.entity.mime.content.StringBody;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    String mCurrentPhotoPath;
    File photoFile = null;
    private static int curId=0;
    EditText ipET;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ((Button)findViewById(R.id.takeBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            ex.printStackTrace();
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                    "com.example.android.fileprovider",
                                    photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            Log.d("uri", photoURI.toString());
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
            }
        };
    });
        ipET=(EditText) findViewById(R.id.ipET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            /*Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");*/
            final String ipText=ipET.getText().toString();
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... voids) {
                    postFile(ipText, photoFile.getPath(), curId++);
                    return null;
                }
            }.execute();
        }
    }

    public static JSONObject postFile(String url, String filePath, int id){
        String result="";
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        File file = new File(filePath);
        MultipartEntityBuilder mpEntityBuilder=MultipartEntityBuilder.create();
        ContentBody cbFile = new FileBody(file, "image/jpeg");
        StringBody stringBody= null;
        JSONObject responseObject=null;
        try {
            stringBody = new StringBody(id+"");
            mpEntityBuilder.addPart("file", cbFile);
            mpEntityBuilder.addPart("id",stringBody);
            httpPost.setEntity(mpEntityBuilder.build());
            System.out.println("executing request " + httpPost.getRequestLine());
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();
            result=resEntity.toString();
            responseObject=new JSONObject(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseObject;
    }
}
