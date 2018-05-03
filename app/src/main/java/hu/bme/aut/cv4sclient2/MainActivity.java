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
import hu.bme.aut.cv4sclient2.controller.StringHandler;
import hu.bme.aut.cv4sclient2.model.Functor;

import static java.text.DateFormat.getDateInstance;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    File photoFile = null;
    EditText ipET;
    TextView descriptorET;
    Button sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        (findViewById(R.id.pickBtn)).setOnClickListener(new View.OnClickListener() {
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
                final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle("File name");
                    final EditText editText = new EditText(getApplicationContext());
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    editText.setLayoutParams(layoutParams);
                    builder.setView(editText);
                    editText.setText(getDateInstance().format(new Date()) + "_");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                photoFile = File.createTempFile(editText.getText().toString(),".jpg",getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            if (photoFile != null) {
                                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                                        "com.example.android.fileprovider",
                                        photoFile);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
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
                NetworkController.getListFromServiceAsync(ipET.getText().toString(), getApplicationContext(), new Functor() {
                    @Override
                    public void run(Object param) {
                        List<String> list=(List<String>)param;
                        list.add(0, "(Choose)");
                        updateGetSpinner((List<String>)list);
                    }
                });
            }
        });

        updateGetSpinner(Collections.singletonList("(List not loaded)"));

        ipET = (EditText) findViewById(R.id.ipET);
        descriptorET = (TextView) findViewById(R.id.descriptorTV);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetworkController.postFileAsync(ipET.getText().toString(), photoFile.getPath(), getApplicationContext(), new Functor() {
                    @Override
                    public void run(Object jsonObject) {
                        updateResultSpinner((JsonObject)jsonObject);
                    }
                });
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
                    NetworkController.getFromServiceAsync(idToGet, url, context, new Functor() {
                        @Override
                        public void run(Object str) {
                            findViewById(R.id.resultSpinner).setVisibility(View.INVISIBLE);
                            descriptorET.setText((String)str);
                        }
                    });
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
                    descriptorET.setText(StringHandler.formatToDisplay(jsonObject.get(spinner.getSelectedItem().toString()).toString()));
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
            createFileWithName(data.getData());
        }
    }

    private void createFileWithName(Uri uri) {
        try {
            InputStream inputStream = MainActivity.this.getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            if (photoFile == null)
                photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + getFilenameFromUri(uri));
            OutputStream outputStream = new FileOutputStream(photoFile);
            outputStream.write(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
