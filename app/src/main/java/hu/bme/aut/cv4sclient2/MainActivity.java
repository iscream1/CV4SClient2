package hu.bme.aut.cv4sclient2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.StringBody;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.CoreConnectionPNames;

import static java.text.DateFormat.getDateInstance;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    String mCurrentPhotoPath;
    File photoFile = null;
    private static int curId = 0;
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

        ((Button) findViewById(R.id.pickBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                startActivityForResult(Intent.createChooser(intent, "Select from file system"), REQUEST_PICK_IMAGE);
            }
        });

        ((Button) findViewById(R.id.takeBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle("File name");
                    final EditText editText = new EditText(getApplicationContext());
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
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
            }

            ;
        });

        ((Button) findViewById(R.id.loadBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getListFromServiceAsync(ipET.getText().toString(), getApplicationContext());
            }
        });

        updateSpinner(Collections.singletonList("(List not loaded)"));

        ipET = (EditText) findViewById(R.id.ipET);
        descriptorET = (TextView) findViewById(R.id.descriptorTV);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postFileAsync(ipET.getText().toString(), getApplicationContext());
            }
        });
    }

    private void updateSpinner(List<String> list) {
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, final View selectedItemView, final int position, long id) {
                if (id != 0) {
                    final long idToGet = id - 1;
                    final Context context=getApplicationContext();
                    final String url=ipET.getText().toString();
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            Log.d("spinner", "" + position);
                            getFromServiceAsync(idToGet, url, context);
                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

    }

    public String getFilenameFromUri(Uri uri) {
        String name = null;
        try (Cursor cursor = MainActivity.this.getContentResolver().query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return name;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            sendBtn.setVisibility(View.VISIBLE);
        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            sendBtn.setVisibility(View.VISIBLE);
            try {
                InputStream inputStream = MainActivity.this.getContentResolver().openInputStream(data.getData());
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                if (photoFile == null)
                    photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + getFilenameFromUri(data.getData()));
                OutputStream outputStream = new FileOutputStream(photoFile);
                outputStream.write(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void postFileAsync(final String url, final Context context) {
        new AsyncTask<Void, Void, String>() {
            IOException exception = null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return postFile(url, photoFile.getPath(), curId++);
                } catch (IOException e) {
                    this.exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String json) {
                super.onPostExecute(json);
                if (exception == null)
                    descriptorET.setText(json);
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void getListFromServiceAsync(final String url, final Context context) {
        new AsyncTask<Void, Void, List<String>>() {
            IOException exception = null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected List<String> doInBackground(Void... voids) {
                try {
                    String result = getFromService(url);

                    result = result.replace("\\\"", "'");
                    result = result.substring(1, result.length() - 1);
                    JsonArray jsonArray = new JsonParser().parse(result).getAsJsonArray();
                    List<String> retList = new ArrayList<>();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        retList.add(jsonArray.get(i).toString());
                    }
                    return retList;
                } catch (IOException e) {
                    this.exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<String> list) {
                super.onPostExecute(list);
                if (exception == null)
                {
                    list.add(0, "(Choose filename)");
                    updateSpinner(list);
                }
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void getFromServiceAsync(final long id, final String url, final Context context) {
        new AsyncTask<Void, Void, String>() {
            IOException exception = null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String result = getFromService(url + "/" + id);
                    result = result = result.replace("\\", "").replace("\"{", "{").replace("}\"", "}")
                            .replace(",", ",\n").replace("[", "\n[\n").replace("]", "\n]\n").replace("{", "{\n").replace("}", "\n}");
                    return result;
                } catch (IOException e) {
                    this.exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String str) {
                super.onPostExecute(str);
                descriptorET.setText(str);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String getFromService(String url) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
        HttpGet httpGet = new HttpGet(url);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity resEntity = httpResponse.getEntity();

        BufferedReader r = new BufferedReader(new InputStreamReader(resEntity.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
        }

        String result = sb.toString();
        return result;
    }

    public static String postFile(String url, String filePath, int id) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
        HttpPost httpPost = new HttpPost(url);
        File file = new File(filePath);
        MultipartEntityBuilder mpEntityBuilder = MultipartEntityBuilder.create();
        StringBody stringBody = null;
        stringBody = new StringBody(id + "");
        mpEntityBuilder.addBinaryBody("image", file);
        mpEntityBuilder.addPart("id", stringBody);
        httpPost.setEntity(mpEntityBuilder.build());
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity resEntity = response.getEntity();

        BufferedReader r = new BufferedReader(new InputStreamReader(resEntity.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
        }

        String result = sb.toString();
        result = result.replace("\\", "").replace("\"{", "{").replace("}\"", "}")
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
