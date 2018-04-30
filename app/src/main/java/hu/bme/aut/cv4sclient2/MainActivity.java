package hu.bme.aut.cv4sclient2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
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
import cz.msebera.android.httpclient.params.CoreConnectionPNames;

import static java.text.DateFormat.getDateInstance;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    String mCurrentPhotoPath;
    File photoFile = null;
    private static int curId=0;
    EditText ipET;
    TextView descriptorET;
    Button sendBtn;

    private File createImageFile(String imageFileName) throws IOException {
        // Create an image file name
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
            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("File name");
                final EditText editText=new EditText(getApplicationContext());
                LinearLayout.LayoutParams layoutParams= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                editText.setLayoutParams(layoutParams);
                builder.setView(editText);
                editText.setText("JPEG_" + getDateInstance().format(new Date()) + "_");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            photoFile = createImageFile(editText.getText().toString());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                    "com.example.android.fileprovider",
                                    photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            Log.d("uri", photoURI.toString());
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                });
                builder.create().show();
            }
        };
    });
        ipET=(EditText) findViewById(R.id.ipET);
        descriptorET=(TextView) findViewById(R.id.descriptorTV);
        sendBtn=(Button) findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postFileAsync();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            sendBtn.setVisibility(View.VISIBLE);
        }
    }

    private void postFileAsync()
    {
        new AsyncTask<Void, Void, String>() {
            final String ipText = ipET.getText().toString();
            final Context context=getApplicationContext();
            IOException exception=null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return postFile(ipText, photoFile.getPath(), curId++);
                } catch (IOException e) {
                    this.exception=e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String json) {
                super.onPostExecute(json);
                if(exception==null)
                    descriptorET.setText(json);
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public static String postFile(String url, String filePath, int id) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
        HttpPost httpPost = new HttpPost(url);
        File file = new File(filePath);
        MultipartEntityBuilder mpEntityBuilder=MultipartEntityBuilder.create();
        StringBody stringBody= null;
        stringBody = new StringBody(id+"");
        mpEntityBuilder.addBinaryBody("image", file);
        mpEntityBuilder.addPart("id",stringBody);
        httpPost.setEntity(mpEntityBuilder.build());
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity resEntity = response.getEntity();

        BufferedReader r = new BufferedReader(new InputStreamReader(resEntity.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
        }

        String result=sb.toString();
        result=result.replace("\\", "").replace("\"{", "{").replace("}\"", "}")
                .replace(",", ",\n").replace("[", "\n[\n").replace("]", "\n]\n").replace("{", "{\n").replace("}", "\n}");
        Log.d("result", result);

        /*JsonReader jsonReader=new JsonReader(new StringReader(result));
        jsonReader.beginObject();

        JsonObject jsonObject=new JsonObject();

        while(jsonReader.hasNext())
        {
            String tag=jsonReader.nextName();
            if (tag.equals("message"))
                jsonObject.addProperty("message", jsonReader.nextString());
            else if (tag.equals("data"))
                jsonObject.addProperty("data", jsonReader.nextString());
        }
        jsonReader.endObject();*/

        return result;
    }
}
