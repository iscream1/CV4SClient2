package hu.bme.aut.cv4sclient2.controller;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import hu.bme.aut.cv4sclient2.model.Functor;

public class NetworkController {
    public static void postFileAsync(final String url, final String photoPath, final Context context, final Functor onPostExecuteHandler) {
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
                    return post(url, photoPath);
                } catch (IOException e) {
                    this.exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(JsonObject jsonObject) {
                super.onPostExecute(jsonObject);
                if (exception == null) {
                    onPostExecuteHandler.run(jsonObject);
                }
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getListFromServiceAsync(final String url, final Context context, final Functor onPostExecuteHandler) {
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
                    String result = NetworkController.get(url);

                    JsonArray jsonArray = new JsonParser().parse(StringHandler.formatToParse(result)).getAsJsonArray();
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
                    onPostExecuteHandler.run(list);
                }
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getFromServiceAsync(final long id, final String url, final Context context, final Functor onPostExecuteHandler) {
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
                    String result = NetworkController.get(url + "/" + id);
                    return StringHandler.formatToDisplay(result);
                } catch (IOException e) {
                    this.exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String str) {
                super.onPostExecute(str);
                if(exception==null) {
                    onPostExecuteHandler.run(str);
                }
                else
                    Toast.makeText(context, "Request failed", Toast.LENGTH_SHORT).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static String get(String url) throws IOException {
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

        return sb.toString();
    }

    private static JsonObject post(String url, String filePath) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
        HttpPost httpPost = new HttpPost(url);
        File file = new File(filePath);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addBinaryBody("image", file);
        httpPost.setEntity(multipartEntityBuilder.build());
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        String result = stringBuilder.toString();

        result = result.replace("\\\"", "'");
        result = result.replace("\\\\", "\\");
        result = result.substring(1, result.length() - 1);

        return new JsonParser().parse(result).getAsJsonObject();
    }
}
