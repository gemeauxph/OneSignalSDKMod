package top.oneconnectapi.app.api;


import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OneConnect {

    private String api_key;
    private Context context;
    private String url = "https://developer.oneconnect.top";

    public String fetch(boolean free) throws IOException {

        OkHttpClient client = getUnsafeOkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("package_name", context.getPackageName())
                .add("api_key", api_key)
                .add("action", "fetchUserServers")
                .add("type", free ? "free" : "pro")
                .build();

        Request request = new Request.Builder()
                .url(url + "/view/front/controller.php")
                .post(formBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (Exception e) {
            Log.e("RESPONSEERROR", e.getMessage());
            return "";
        }
    }

    public OneConnect() { }

    public void initialize(Context context, String api_key) {
        this.api_key = api_key;
        this.context = context;
    }

    public void setEndPoint(String url) {
        this.url = url;
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
