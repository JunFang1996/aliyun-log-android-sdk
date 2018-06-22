package com.aliyun.sls.android.sdk;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.sls.android.sdk.core.AsyncTask;
import com.aliyun.sls.android.sdk.core.RequestOperation;
import com.aliyun.sls.android.sdk.core.auth.CredentialProvider;
import com.aliyun.sls.android.sdk.core.callback.CompletedCallback;
import com.aliyun.sls.android.sdk.model.LogGroup;
import com.aliyun.sls.android.sdk.request.PostCachedLogRequest;
import com.aliyun.sls.android.sdk.request.PostLogRequest;
import com.aliyun.sls.android.sdk.result.PostCachedLogResult;
import com.aliyun.sls.android.sdk.result.PostLogResult;
import com.aliyun.sls.android.sdk.utils.Base64Kit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by wangjwchn on 16/8/2.
 * edited by wangzheng on 17/10/15
 */
public class LOGClient {
    private URI endpointURI;
    private RequestOperation requestOperation;
    private CacheManager cacheManager;
    private Boolean cachable;
    private ClientConfiguration.NetworkPolicy policy;
    private Context context;
    private WeakHashMap<PostLogRequest, CompletedCallback<PostLogRequest, PostLogResult>> mCompletedCallbacks = new WeakHashMap<PostLogRequest, CompletedCallback<PostLogRequest, PostLogResult>>();
    private CompletedCallback<PostLogRequest, PostLogResult> callbackImp;

    public LOGClient(String endpoint, CredentialProvider credentialProvider, ClientConfiguration conf) {
        try {
            mHttpType = "http://";
            if (endpoint.trim() != "") {
                mEndPoint = endpoint;
            } else {
                throw new NullPointerException("endpoint is null");
            }

            if (mEndPoint.startsWith("http://")) {
                mEndPoint = mEndPoint.substring(7);
            } else if (mEndPoint.startsWith("https://")) {
                mEndPoint = mEndPoint.substring(8);
                mHttpType = "https://";
            }

            while (mEndPoint.endsWith("/")) {
                mEndPoint = mEndPoint.substring(0, mEndPoint.length() - 1);
            }

            this.endpointURI = new URI(mHttpType + mEndPoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Endpoint must be a string like 'http://cn-****.log.aliyuncs.com'," +
                    "or your cname like 'http://image.cnamedomain.com'!");
        }

        if (credentialProvider == null) {
            throw new IllegalArgumentException("CredentialProvider can't be null.");
        }

        if (conf != null) {
            this.cachable = conf.getCachable();
            this.policy = conf.getConnectType();
        }

        requestOperation = new RequestOperation(endpointURI, credentialProvider, (conf == null ? ClientConfiguration.getDefaultConf() : conf));
        cacheManager = new CacheManager(this);

        callbackImp = new CompletedCallback<PostLogRequest, PostLogResult>() {
            @Override
            public void onSuccess(PostLogRequest request, PostLogResult result) {
                CompletedCallback<PostLogRequest, PostLogResult> callback = mCompletedCallbacks.get(request);
                if (callback != null) {
                    try {
                        callback.onSuccess(request, result);
                    } catch (Exception ignore) {
                        // The callback throws the exception, ignore it
                    }
                }
            }

            @Override
            public void onFailure(PostLogRequest request, LogException exception) {

                if (cachable) {
                    LogEntity item = new LogEntity();
                    item.setProject(request.mProject);
                    item.setStore(request.mLogStoreName);
                    item.setEndPoint(mEndPoint);
                    item.setJsonString(request.mLogGroup.LogGroupToJsonString());
                    item.setTimestamp(new Long(new Date().getTime()));
                    SLSDatabaseManager.getInstance().insertRecordIntoDB(item);
                }

                CompletedCallback<PostLogRequest, PostLogResult> callback = mCompletedCallbacks.get(request);
                if (callback != null) {
                    try {
                        callback.onFailure(request, exception);
                    } catch (Exception ignore) {
                        // The callback throws the exception, ignore it
                    }
                }
            }
        };

    }

    public AsyncTask<PostLogResult> asyncPostLog(PostLogRequest request, CompletedCallback<PostLogRequest, PostLogResult> completedCallback)
            throws LogException {

        mCompletedCallbacks.put(request, completedCallback);

        return requestOperation.postLog(request, callbackImp);
    }

    public AsyncTask<PostCachedLogResult> asyncPostCachedLog(PostCachedLogRequest request, final CompletedCallback<PostCachedLogRequest, PostCachedLogResult> completedCallback)
            throws LogException {
        return requestOperation.postCachedLog(request, completedCallback);
    }


    public PostLogResult syncPostLog(PostLogRequest request, CompletedCallback<PostLogRequest, PostLogResult> completedCallback)
            throws LogException {
        return requestOperation.postLog(request, completedCallback).getResult();
    }

    /*
     *  以下是0.3.1（包含）以前的版本。
     */
    //=================================================
    private String mEndPoint;
    private String mAccessKeyID;
    private String mAccessKeySecret;
    private String mAccessToken;
    private String mProject;
    private String mHttpType;

    public LOGClient(String endPoint, String accessKeyID, String accessKeySecret, String projectName) {
        mHttpType = "http://";
        if (endPoint != "") mEndPoint = endPoint;
        else throw new NullPointerException("endpoint is null");
        if (mEndPoint.startsWith("http://")) {
            mEndPoint = mEndPoint.substring(7);
        } else if (mEndPoint.startsWith("https://")) {
            mEndPoint = mEndPoint.substring(8);
            mHttpType = "https://";
        }
        while (mEndPoint.endsWith("/")) {
            mEndPoint = mEndPoint.substring(0, mEndPoint.length() - 1);
        }

        if (accessKeyID != "") mAccessKeyID = accessKeyID;
        else throw new NullPointerException("accessKeyID is null");

        if (accessKeySecret != "") mAccessKeySecret = accessKeySecret;
        else throw new NullPointerException("accessKeySecret is null");

        if (projectName != "") mProject = projectName;
        else throw new NullPointerException("projectName is null");

        mAccessToken = "";
    }


    public void SetToken(String token) {
        mAccessToken = token;
    }

    public String GetToken() {
        return mAccessToken;
    }

    public String GetEndPoint() {
        return mEndPoint;
    }

    public String GetKeyID() {
        return mAccessKeyID;
    }

    public String GetKeySecret() {
        return mAccessKeySecret;
    }

    public void PostLog(final LogGroup logGroup, String logStoreName) throws LogException {
        final String httpUrl = mHttpType + mProject + "." + mEndPoint + "/logstores/" + logStoreName + "/shards/lb";
        byte[] httpPostBody;
        try {
            httpPostBody = logGroup.LogGroupToJsonString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new LogException("LogClientError", "Failed to pass log to utf-8 bytes", e, "");
        }
        final byte[] httpPostBodyZipped = GzipFrom(httpPostBody);
        final Map<String, String> httpHeaders = GetHttpHeadersFrom(logStoreName, httpPostBody, httpPostBodyZipped);
        HttpPostRequest(httpUrl, httpHeaders, httpPostBodyZipped);
    }

    public void HttpPostRequest(String url, Map<String, String> headers, byte[] body) throws LogException {
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            throw new LogException("LogClientError", "illegal post url", e, "");
        }

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) u.openConnection();
        } catch (IOException e) {
            throw new LogException("LogClientError", "fail to create HttpURLConnection", e, "");
        }


        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new LogException("LogClientError", "fail to set http request method to  POST", e, "");
        }
        conn.setDoOutput(true);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }

        try {
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(body);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            throw new LogException("LogClientError", "fail to post data to URL:" + url, e, "");
        }


        try {
            int responseCode = conn.getResponseCode();
            String request_id = conn.getHeaderField("x-log-requestid");

            if (request_id == null) {
                request_id = "";
            }
            if (responseCode != 200) {
                InputStream error_stream = conn.getErrorStream();
                if (error_stream != null) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(error_stream));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    CheckError(response.toString(), request_id);
                    throw new LogException("LogServerError", "Response code:"
                            + String.valueOf(responseCode) + "\nMessage:"
                            + response.toString(), request_id);
                } else {
                    throw new LogException("LogServerError", "Response code:"
                            + String.valueOf(responseCode)
                            + "\nMessage: fail to connect to the server",
                            request_id);
                }
            }// else success
        } catch (IOException e) {
            throw new LogException("LogServerError",
                    "Failed to parse response data", "");
        }
    }

    private void CheckError(String error_message, String request_id) throws LogException {
        try {
            JSONObject obj = JSON.parseObject(error_message);
            if (obj != null && obj.containsKey("errorCode") && obj.containsKey("errorMessage")) {
                throw new LogException(obj.getString("errorCode"), obj.getString("errorMessage"), request_id);
            }
        } catch (JSONException e) {
        }
    }

    public Map<String, String> GetHttpHeadersFrom(String logStoreName, byte[] body, byte[] bodyZipped) throws LogException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        headers.put("Content-Type", "application/json");
        headers.put("Date", GetMGTTime());
        headers.put("Content-MD5", ParseToMd5U32From(bodyZipped));
        headers.put("Content-Length", String.valueOf(bodyZipped.length));
        headers.put("x-log-bodyrawsize", String.valueOf(body.length));
        headers.put("x-log-compresstype", "deflate");
        headers.put("Host", mProject + "." + mEndPoint);

        StringBuilder signStringBuf = new StringBuilder("POST" + "\n").
                append(headers.get("Content-MD5") + "\n").
                append(headers.get("Content-Type") + "\n").
                append(headers.get("Date") + "\n");
        String token = mAccessToken;
        if (token != null && token != "") {
            headers.put("x-acs-security-token", token);
            signStringBuf.append("x-acs-security-token:" + headers.get("x-acs-security-token") + "\n");
        }
        signStringBuf.append("x-log-apiversion:0.6.0\n").
                append("x-log-bodyrawsize:" + headers.get("x-log-bodyrawsize") + "\n").
                append("x-log-compresstype:deflate\n").
                append("x-log-signaturemethod:hmac-sha1\n").
                append("/logstores/" + logStoreName + "/shards/lb");
        String signString = signStringBuf.toString();
        try {
            String sign = hmac_sha1(signString, mAccessKeySecret);
            headers.put("Authorization", "LOG " + mAccessKeyID + ":" + sign);
        } catch (Exception e) {
            throw new LogException("LogClientError", "fail to get encode signature", e, "");
        }
        return headers;
    }


    public static String GetMGTTime() {
        Calendar cd = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 设置时区为GMT
        String str = sdf.format(cd.getTime());
        return str;
    }

    public static String hmac_sha1(String encryptText, String encryptKey) throws Exception {
        byte[] keyBytes = encryptKey.getBytes("UTF-8");
        byte[] dataBytes = encryptText.getBytes("UTF-8");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
        return new String(Base64Kit.encode(mac.doFinal(dataBytes)));
    }


    private String ParseToMd5U32From(byte[] bytes) throws LogException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String res = new BigInteger(1, md.digest(bytes)).toString(16).toUpperCase();
            StringBuilder zeros = new StringBuilder();
            for (int i = 0; i + res.length() < 32; i++) {
                zeros.append("0");
            }
            return zeros.toString() + res;
        } catch (NoSuchAlgorithmException e) {
            throw new LogException("LogClientError", "Not Supported signature method " + "MD5", e, "");
        }
    }

    private byte[] GzipFrom(byte[] jsonByte) throws LogException {
        ByteArrayOutputStream out = null;
        Deflater compresser = new Deflater();
        try {
            out = new ByteArrayOutputStream(jsonByte.length);
            compresser.setInput(jsonByte);
            compresser.finish();
            byte[] buf = new byte[10240];
            while (compresser.finished() == false) {
                int count = compresser.deflate(buf);
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new LogException("LogClientError", "fail to zip data", "");
        } finally {
            compresser.end();
            try {
                if (out.size() != 0) out.close();
            } catch (IOException e) {
            }
        }
    }

    public ClientConfiguration.NetworkPolicy getPolicy() {
        return policy;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d("SLS SDK", "LOGClient finalize");
    }
    //=================================================
}
