package cat.institutmontilivi.djau;

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
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpPersistentConnection implements Serializable {
    /**
     * Connexió que autentica l'usuari amb una cookie.
     */
    public int CONN_TIMEOUT = 10000;
    public int READ_TIMEOUT = 10000;
    public String METHOD_POST = "POST";
    public boolean API_DEBUG = true; //Accepta qualsevol HTTP, perillós.

    SerializableCookieManager msCookieManager = new SerializableCookieManager();

    public HttpPersistentConnection()
    {
        //Obtenir si és DEBUG de la configuració de l'Aplicació.
        this.API_DEBUG = Configuration.getInstance().APIDebug;
    }

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

    public String requestWebService(String serviceUrl) throws HttpErrorException, IOException, NoSuchAlgorithmException {
        disableConnectionReuseIfNecessary();

        HttpURLConnection urlConnection = null;
        try {
            // create connection
            Log.e("ERROR", "WebService URL:" + serviceUrl);
            urlConnection = obrirConnexioHTTPoHTTPS(serviceUrl);
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

    public HttpURLConnection obrirConnexioHTTPoHTTPS(String serviceUrl) throws IOException {
        URL urlToRequest = new URL(serviceUrl);
        if (serviceUrl.contains("https://")) {
            if (API_DEBUG)
                trustAllHosts();
            HttpsURLConnection conn = (HttpsURLConnection) urlToRequest.openConnection();
            if (API_DEBUG)
                conn.setHostnameVerifier(DO_NOT_VERIFY);
            return conn;
        }
        else
        {
            return (HttpURLConnection) urlToRequest.openConnection();
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
        return sendData(arr.toString(), urlString, method);
    }

    public String sendJSONObject(JSONObject obj, String urlString, String method) throws Exception {
        return sendData(obj.toString(), urlString, method);
    }

    public String sendData(String data, String urlString, String method) throws HttpErrorException, IOException {
        HttpURLConnection connection = null;
        try {
            connection = obrirConnexioHTTPoHTTPS(urlString);
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

    // always verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
