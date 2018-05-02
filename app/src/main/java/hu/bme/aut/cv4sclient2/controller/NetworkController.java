package hu.bme.aut.cv4sclient2.controller;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.StringBody;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.CoreConnectionPNames;

public class NetworkController {
    public static String getFromService(String url) throws IOException {
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

    public static JsonObject postFile(String url, String filePath, int id) throws IOException {
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
        Log.d("result", result);

        result = result.replace("\\\"", "'");
        result = result.replace("\\\\", "\\");
        result = result.substring(1, result.length() - 1);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(result).getAsJsonObject();

        return jsonObject;
    }
}
