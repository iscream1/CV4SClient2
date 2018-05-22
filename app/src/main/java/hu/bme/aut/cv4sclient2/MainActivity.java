package hu.bme.aut.cv4sclient2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import hu.bme.aut.cv4sclient2.controller.NetworkController;
import hu.bme.aut.cv4sclient2.controller.StringHandler;
import hu.bme.aut.cv4sclient2.model.Functor;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    File photoFile = null;
    EditText ipET;
    TextView descriptorTV;
    Button sendBtn;

    Functor toastDisplayFunctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPickBtn();

        initTakeBtn();

        initLoadBtn();

        initToastDisplayFunctor();

        initSendBtn();

        initGetSpinner();

        initIpET();

        initDescriptorTV();
    }

    private void initToastDisplayFunctor() {
        toastDisplayFunctor=new Functor() {
            @Override
            public void run(Object param) {
                Toast.makeText(getApplicationContext(), (String)param, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void initIpET() {
        ipET = (EditText) findViewById(R.id.ipET);
    }

    private void initDescriptorTV() {
        descriptorTV = (TextView) findViewById(R.id.descriptorTV);
    }

    private void initSendBtn() {
        sendBtn = (Button) findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetworkController.postFileAsync(ipET.getText().toString(), photoFile.getPath(), new Functor() {
                    @Override
                    public void run(Object jsonObject) {
                        updateResultSpinner((JsonObject)jsonObject);
                    }
                },
                toastDisplayFunctor);
            }
        });
    }

    private void initGetSpinner() {
        updateGetSpinner(Collections.singletonList("(List not loaded)"));
    }

    private void initLoadBtn() {
        (findViewById(R.id.loadBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetworkController.getListFromServiceAsync(ipET.getText().toString(), new Functor() {
                    @Override
                    public void run(Object param) {
                        List<String> list=(List<String>)param;
                        list.add(0, "(Choose)");
                        updateGetSpinner(list);
                    }
                },
                toastDisplayFunctor);
            }
        });
    }

    private void initTakeBtn() {
        (findViewById(R.id.takeBtn)).setOnClickListener(new View.OnClickListener() {
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
                    editText.setText(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/"+editText.getText().toString()+".jpg");
                            if (photoFile != null) {
                                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                                        "com.example.android.fileprovider",
                                        photoFile);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                }
            };
        });
    }

    private void initPickBtn() {
        (findViewById(R.id.pickBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select from file system"), REQUEST_PICK_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            sendBtn.setVisibility(View.VISIBLE);
        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            try {
                createFileWithName(data.getData());
                sendBtn.setVisibility(View.VISIBLE);
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(), "Could not create file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createFileWithName(Uri uri) throws IOException {
        InputStream inputStream = MainActivity.this.getContentResolver().openInputStream(uri);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + getFilenameFromUri(uri));
        OutputStream outputStream = new FileOutputStream(photoFile);
        outputStream.write(buffer);
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

    private void updateGetSpinner(List<String> list) {
        Spinner spinner = (Spinner) findViewById(R.id.getSpinner);

        Toast.makeText(getApplicationContext(), "List loaded", Toast.LENGTH_SHORT).show();

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, final View selectedItemView, final int position, long id) {
                if (id != 0) {
                    final long idToGet = id - 1;
                    final String url=ipET.getText().toString();
                    NetworkController.getFromServiceAsync(idToGet, url, new Functor() {
                                @Override
                                public void run(Object str) {
                                    findViewById(R.id.resultSpinner).setVisibility(View.INVISIBLE);
                                    descriptorTV.setText((String) str);
                                }
                            },
                    toastDisplayFunctor);
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

        String message="Response received";

        List<String> keyList=new ArrayList<>();
        keyList.add("(Choose from result elements)");
        for (Map.Entry e:jsonObject.entrySet()) {
            keyList.add((String)e.getKey());
            if(e.getKey().equals("message"))
                message+=", message: "+jsonObject.get("message");
        }


        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, keyList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, final View selectedItemView, final int position, long id) {
                if (id == 0) {
                    descriptorTV.setText("");
                }
                else{
                    descriptorTV.setText(StringHandler.formatToDisplay(jsonObject.get(spinner.getSelectedItem().toString()).toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }
}
