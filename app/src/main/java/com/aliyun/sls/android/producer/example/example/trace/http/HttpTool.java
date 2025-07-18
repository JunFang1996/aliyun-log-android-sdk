package com.aliyun.sls.android.producer.example.example.trace.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.aliyun.sls.android.okhttp.OKHttp3Tracer;
//import com.aliyun.sls.android.plugin.trace.SLSTelemetry;
import com.aliyun.sls.android.producer.HttpConfigProxy;
//import io.opentelemetry.context.Context;
//import io.opentelemetry.context.propagation.TextMapSetter;
import com.aliyun.sls.android.producer.example.example.trace.http.cookie.PersistentCookieStore;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author gordon
 * @date 2021/09/01
 */
public class HttpTool {
    private static final String TAG = "HttpTool";

    public interface HttpCallback {
        void onComplete(Response response);
    }

    //private static SLSTelemetry traceSdk = SLSTelemetry.getInstance();
    //private static Tracer tracer = traceSdk.getTracer("HttpTool");

    private HttpTool() {
        //no instance
    }

    public static void get(String host, String path, HttpCallback callback) {
        get(host, path, null, callback);
    }

    public static void get(String host, String path, Map<String, String> headers, HttpCallback callback) {
        http(host, path, "GET", mapToStrings(headers), null, callback);
    }

    public static void delete(String host, String path, HttpCallback callback) {
        http(host, path, "DELETE", mapToStrings(null), null, callback);
    }

    public static void post(String url, String body, HttpCallback callback) {
        post(url, null, body, callback);
    }

    public static void post(String host, String path, String body, HttpCallback callback) {
        post(host, path, null, body, callback);
    }

    public static void post(String host, String path, Map<String, String> headers, String body, HttpCallback callback) {
        if (null == headers) {
            headers = new HashMap<>();
        }

        headers.put("Content-Length", TextUtils.isEmpty(body) ? "0" : String.valueOf(body.length()));
        http(host, path, "POST", mapToStrings(headers), body, callback);
    }

    private static void http(String host, String path, String method, String[] headers, String body, HttpCallback callback) {
        //Span span = tracer.spanBuilder(String.format("Request HTTP: %s", path)).startSpan()
        //        .setAttribute(SemanticAttributes.HTTP_ROUTE, path)
        //        .setAttribute("http.query", body);
        //span.end();

        //final Context context = Context.current().with(span);
        //ThreadUtils.exec(() -> {
        //    Span span = Tracer.spanBuilder("start http request").build();
        //    span.end();
        //    ContextManager.INSTANCE.update(span);
        //    Response response = internalOkHttp(host, path, method, headers, body, callback);
            //callback.onComplete(response);
        internalOkHttp(host, path, method, headers, body, callback);
        //});
    }

    private static Response internalHttp(String host, String path, String method, String[] headers, String body) {
        Log.v(TAG, "http request =>> host: " + host + ", path: " + path + ", method: " + method + ", headers: " + arrayToString(headers) + ", body: " + body);
        final String urlString = host + path;

        //Span span = tracer.spanBuilder("/").setSpanKind(SpanKind.CLIENT).setParent(context).startSpan();
        //span.setAttribute(SemanticAttributes.HTTP_METHOD, method.toUpperCase(Locale.ROOT));
        //span.setAttribute("component", "http");
        //span.setAttribute(SemanticAttributes.HTTP_URL, urlString);
        //span.setAttribute(SemanticAttributes.HTTP_HOST, host);
        //span.setAttribute(SemanticAttributes.HTTP_ROUTE, path);
        //span.setAttribute(SemanticAttributes.HTTP_USER_AGENT, HttpConfigProxy.getUserAgent());

        int responseCode = -1;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //TextMapSetter<HttpURLConnection> setter = URLConnection::setRequestProperty;
            // Inject the request with the current Context/Span.
            //traceSdk.getPropagators().getTextMapPropagator().inject(context, connection, setter);

            if (TextUtils.equals("POST", method)) {
                connection.setDoOutput(true);
            }
            connection.setDoInput(true);
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-agent", HttpConfigProxy.getUserAgent());

            final String cookie = SLSCookieManager.getCookie();
            if (!TextUtils.isEmpty(cookie)) {
                connection.setRequestProperty("Cookie", cookie);
            }

            if (headers != null) {
                int pairs = headers.length / 2;
                for (int i = 0; i < pairs; i++) {
                    String key = headers[2 * i];
                    String val = headers[2 * i + 1];
                    connection.setRequestProperty(key, val);
                }
            }

            if (null != body && connection.getDoOutput()) {
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                //noinspection CharsetObjectCanBeUsed
                out.write(body.getBytes(Charset.forName("UTF-8")));
                out.flush();
                out.close();
            }

            Response response = new Response();
            Map<String, List<String>> responseHeaderMap = connection.getHeaderFields();
            if (null != responseHeaderMap) {
                String[] responseHeaders = new String[responseHeaderMap.size() * 2];
                int index = 0;
                for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                    responseHeaders[index * 2] = entry.getKey();
                    responseHeaders[index * 2 + 1] = listToString(entry.getValue());
                    index += 1;
                }
                response.headers = responseHeaders;
            }

            responseCode = connection.getResponseCode();
            response.code = responseCode;

            if (responseCode / 100 == 2) {
                response.data = streamToString(connection.getInputStream());
            } else {
                response.error = streamToString(connection.getErrorStream());
            }

            Log.v(TAG, "http response=>> code: " + responseCode + ", response: " + response.toString());
            return response;
        } catch (Exception ex) {
            Log.w(TAG, "exception: " + ex.getLocalizedMessage());
            Response response = new Response();
            response.error = ex.getLocalizedMessage();
            response.code = 400;

            //span.setAttribute("exception", ex.getLocalizedMessage());
            //span.addEvent("exception", Attributes.builder().put("message", ex.getLocalizedMessage())
            //        .put("cause", ex.getCause().toString()).build());
            //span.setStatus(StatusCode.ERROR, "Http Code: " + responseCode);

            return response;
        } finally {
            //span.end();
        }
    }

    private static Response internalOkHttp(String host, String path, String method, String[] headers, String body, HttpCallback callback) {
        Log.v(TAG, "http request =>> host: " + host + ", path: " + path + ", method: " + method + ", headers: " + arrayToString(headers) + ", body: " + body);
        final String urlString = host + path;

        OkHttpClient client = new Builder()
            .addInterceptor(new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    return chain.proceed(request);
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(new CookieJar() {

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                //SLSCookieManager.setCookie(cookies);
                if (null == cookies) {
                    return;
                }

                for (Cookie cookie : cookies) {
                    PersistentCookieStore.getInstance().add(url, cookie);
                }

                //SLSCookieManager.setCookie(cookies.get(0).value());
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                return PersistentCookieStore.getInstance().get(url);
                //List<Cookie> cookieList = new ArrayList<>();
                ////cookieList.add(new Cookie.Builder().hostOnlyDomain(url.host()).name("md.sid").value("s%3ABOofXUkt8mj3zJ2ZC1JHKR6FwmFmAiD0.wTBsquQZ5oF7tYubhg1imc2hXVyt9gToRXdaD50rXIU; logged_in=BOofXUkt8mj3zJ2ZC1JHKR6FwmFmAiD0").build());
                //cookieList.add(new Cookie.Builder().hostOnlyDomain(url.host()).name("md.sid").value(SLSCookieManager.getCookie()).build());
                //return cookieList;
            }
        }).build();

        Request request = new Request.Builder()
            .url(urlString)
            .method(method, TextUtils.equals("GET", method) ? null : RequestBody.create(MediaType.parse("application/json"), TextUtils.isEmpty(body) ? "" : body))
            .headers(Headers.of(headers))
            .build();

        try {
            //OKHttp3Tracer.newCallFactory(client).newCall(request).enqueue(new Callback() {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, okhttp3.Response res) throws IOException {
                    Response response = new Response();
                    response.code = res.code();
                    final int headerCount = res.headers().size();
                    String[] responseHeaders = new String[res.headers().size() * headerCount];
                    for (int i = 0; i < headerCount; i++) {
                        responseHeaders[i] = res.headers().name(i);
                        responseHeaders[i + 1] = res.headers().value(i);
                    }
                    response.headers = responseHeaders;

                    if (response.code / 100 == 2) {
                        response.data = res.body().string();
                    } else {
                        response.error = res.message();
                    }
                    Log.v(TAG, "http response=>> code: " + response.code + ", response: " + response.toString());
                    callback.onComplete(response);
                }
            });

            return null;
        } catch (Throwable ex) {
            Log.w(TAG, "exception: " + ex.getLocalizedMessage());
            Response response = new Response();
            response.error = ex.getLocalizedMessage();
            response.code = 400;
            callback.onComplete(response);
            return response;
        }
    }

    private static String listToString(List<String> list) {
        if (null == list || list.size() == 0) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String s : list) {
            stringBuilder.append(s).append(";");
        }

        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    private static String[] mapToStrings(Map<String, String> maps) {
        if (null == maps) {
            maps = new HashMap<>();
        }

        maps.put("User-agent", HttpConfigProxy.getUserAgent());
        maps.put("Content-Type", "application/json; charset=UTF-8");

        String[] headers = new String[maps.size() * 2];
        int index = 0;
        for (Map.Entry<String, String> entry : maps.entrySet()) {
            headers[index * 2] = entry.getKey();
            headers[index * 2 + 1] = entry.getValue();
            index++;
        }
        return headers;
    }

    private static String arrayToString(String[] array) {
        if (null == array) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (String s : array) {
            builder.append(" ");
            builder.append(s);
            builder.append(",");
        }

        builder.append("]");
        return builder.toString();
    }

    private static String streamToString(InputStream ins) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        String line;
        StringBuilder data = new StringBuilder();
        while ((line = in.readLine()) != null) {
            data.append(line);
        }
        in.close();
        return data.toString();

    }

    public static class Response {
        public int code;
        public String data;
        public String error;
        public String[] headers;

        public boolean success() {
            return code / 100 == 2;
        }

        @NonNull
        @Override
        public String toString() {
            return "{" + "code: " + code + ", headers: " + headerToString() + ", data: " + data + ", error: " + error + "}";
        }

        private String headerToString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            for (int i = 0; i < headers.length / 2; i++) {
                builder.append(headers[i * 2]);
                builder.append(": ");
                builder.append(headers[i * 2 + 1]);
                builder.append(", ");
            }
            builder.append("]");

            if (builder.length() > 2) {
                builder.replace(builder.length() - 2, builder.length() - 2, "");
            }

            return builder.toString();
        }
    }
}
