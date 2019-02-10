package com.example.djau;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class HttpPersistentConnection implements Serializable {
    /**
     * Connexió que autentica l'usuari amb una cookie.
     */
    public static int CONN_TIMEOUT = 5000;
    public static int READ_TIMEOUT = 6000;

    public static String METHOD_POST = "POST";

    SerializableCookieManager msCookieManager = new SerializableCookieManager();

    private void getAndSaveCookiesToManager(HttpURLConnection connection)
    {
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        List<String> cookiesHeader = headerFields.get("Set-Cookie");

        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }
        }
    }

    private void setCookiesToConnection(HttpURLConnection connection)
    {
        if (msCookieManager.getCookieStore().getCookies().size() > 0) {
            // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
            connection.setRequestProperty("Cookie",
                    TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
        }
    }

    public String requestWebService(String serviceUrl) throws HttpErrorException, IOException {
        disableConnectionReuseIfNecessary();

        HttpURLConnection urlConnection = null;
        try {
            // create connection
            Log.e("ERROR", "WebService URL:" + serviceUrl);
            URL urlToRequest = new URL(serviceUrl);
            urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            //urlConnection.setRequestProperty("Authorization", Constants.BASIC_AUTH);
            urlConnection.setConnectTimeout(CONN_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            setCookiesToConnection(urlConnection);
            urlConnection.connect();

            getAndSaveCookiesToManager(urlConnection);
            String response = rebreResposta(urlConnection);
            Log.e("HTTP", response);
            return response;
        } catch (Exception e) {
            String response = getResponseText(
                    new BufferedInputStream(urlConnection.getErrorStream()));

            String msg = e.toString() + response;
            Log.e("ERROR", msg);
            throw new HttpErrorException(response, String.valueOf(urlConnection.getResponseCode()), e);
        }
    }

    public JSONObject requestGetJSONObject(String url) throws Exception {
        //Retorna un objecte JSON Object, plè o sense dades.
        String dades = requestData(url);
        if (dades == "")
            return new JSONObject();
        else
            return new JSONObject(dades);
    }

    public JSONArray requestGetJSONArray(String url) throws Exception {
        //Retorna un objecte JSON ARRAY o bé un array buit.
        String dades = requestData(url);
        if (dades == "")
            return new JSONArray();
        else
            return new JSONArray(dades);
    }

    public String requestData(String url) throws Exception {
        String dades = requestWebService(url);
        if (dades != null) {
            Log.e("DEBUG", dades);
            return dades;
        }
        else {
            Log.e("DEBUG", "El webservice ha tornat null");
            return "";
        }
    }

    public String sendJSONArray(JSONArray arr, String urlString, String method) throws Exception {
        return sendJSONData(arr.toString(), urlString, method);
    }

    public String sendJSONObject(JSONObject obj, String urlString, String method) throws Exception {
        return sendJSONData(obj.toString(), urlString, method);
    }

    public String sendJSONData(String data, String urlString, String method) throws HttpErrorException, IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setDoOutput(true);
            setCookiesToConnection(connection);

            DataOutputStream dStream = new DataOutputStream(connection.getOutputStream());
            Log.e("DEBUG","Post strings:" + data);
            dStream.writeBytes(data); //Writes out the string to the underlying output stream as a sequence of bytes
            dStream.flush(); // Flushes the data output stream.
            dStream.close();
            connection.connect();

            getAndSaveCookiesToManager(connection);
            String response = rebreResposta(connection);
            Log.e("HTTP", response);
            return response;
        }
        catch (Exception e)
        {
            String response = getResponseText(
                    new BufferedInputStream(connection.getErrorStream()));

            Log.e("ERROR", response);
            throw new HttpErrorException(response, String.valueOf(connection.getResponseCode()), e);
        }
    }

    private String rebreResposta(HttpURLConnection urlConnection) throws Exception {
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new Exception("Problemes en la connexió, " +
                    "codi: " + urlConnection.getResponseCode() + ", " +
                    "resposta:" + urlConnection.getResponseMessage());
        InputStream in = new BufferedInputStream(
                urlConnection.getInputStream());
        return getResponseText(in);
    }

    private String rebreRespostaError(HttpURLConnection urlConnection) throws Exception {
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new Exception("Problemes en la connexió, " +
                    "codi: " + urlConnection.getResponseCode() + ", " +
                    "resposta:" + urlConnection.getResponseMessage());
        InputStream in = new BufferedInputStream(
                urlConnection.getErrorStream());
        return getResponseText(in);
    }

    private void disableConnectionReuseIfNecessary() {
        // see HttpURLConnection API doc
        if (Integer.parseInt(Build.VERSION.SDK)
                < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

}
