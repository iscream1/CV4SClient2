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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import hu.bme.aut.cv4sclient2.controller.NetworkController;

import static java.text.DateFormat.getDateInstance;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    File photoFile = null;
    private static int curId = 0;
    EditText ipET;
    TextView descriptorET;
    Button sendBtn;

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
                                photoFile = File.createTempFile(editText.getText().toString(),".jpg",getExternalFilesDir(Environment.DIRECTORY_PICTURES));
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

        updateGetSpinner(Collections.singletonList("(List not loaded)"));

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

    private void updateGetSpinner(List<String> list) {
        Spinner spinner = (Spinner) findViewById(R.id.getSpinner);

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
                    getFromServiceAsync(idToGet, url, context);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void updateResultSpinner(final JsonObject jsonObject) {
        final Spinner spinner = (Spinner) findViewById(R.id.resultSpinner);
        spinner.setVisibility(View.VISIBLE);

        List<String> keyList=new ArrayList<>();
        keyList.add("(Choose from result elements)");
        for (Map.Entry e:jsonObject.entrySet()) {
            keyList.add((String)e.getKey());
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, keyList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, final View selectedItemView, final int position, long id) {
                if (id == 0) {
                    descriptorET.setText("");
                }
                else{
                    descriptorET.setText(jsonObject.get(spinner.getSelectedItem().toString()).toString()
                            .replace("\\", "").replace("\"{", "{").replace("}\"", "}")
                            .replace(",", ",\n").replace("[", "\n[\n").replace("]", "\n]\n").replace("{", "{\n").replace("}", "\n}"));
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
        new AsyncTask<Void, Void, JsonObject>() {
            IOException exception = null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected JsonObject doInBackground(Void... voids) {
                try {
                    return NetworkController.postFile(url, photoFile.getPath(), curId++);
                } catch (IOException e) {
                    this.exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(JsonObject jsonObject) {
                super.onPostExecute(jsonObject);
                if (exception == null)
                    updateResultSpinner(jsonObject);
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
                    String result = NetworkController.getFromService(url);

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
                    updateGetSpinner(list);
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
                Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String result = NetworkController.getFromService(url + "/" + id);
                    result = result.replace("\\", "").replace("\"{", "{").replace("}\"", "}")
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
                if(exception==null) {
                    findViewById(R.id.resultSpinner).setVisibility(View.INVISIBLE);
                    descriptorET.setText(str);
                }
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
