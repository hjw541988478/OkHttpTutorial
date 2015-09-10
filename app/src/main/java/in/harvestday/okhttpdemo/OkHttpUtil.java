package in.harvestday.okhttpdemo;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okio.BufferedSink;

/**
 * Created by Administrator on 2015/9/10.
 */
public class OkHttpUtil {

    private static int CACHE_SIZE = 10 * 1024 * 1024; //default 10MB
    private static MediaType MEDIA_TYPE = MediaType.parse("text/plain;charset=utf-8");

    private static OkHttpUtil instance = null;
    private static OkHttpClient client = new OkHttpClient();

    private static Map<String, String> defaultHeaders = new HashMap<>();

    private OkHttpUtil() {
    }

    public static OkHttpUtil getInstance() {
        if (instance == null) {
            synchronized (OkHttpUtil.class) {
                instance = new OkHttpUtil();
            }
        }
        return instance;
    }

    public OkHttpUtil init() {
        if (client == null)
            client = new OkHttpClient();
        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setWriteTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        return this;
    }

    public OkHttpUtil init(String cacheDirectory, int cacheSize) {
        init();
        File cacheDir = new File(cacheDirectory);
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        if (CACHE_SIZE > 0)
            CACHE_SIZE = cacheSize;
        Cache cache = new Cache(cacheDir, CACHE_SIZE);
        client.setCache(cache);
        return this;
    }

    private static boolean hasDefaultHeaders() {
        return !defaultHeaders.isEmpty();
    }

    public OkHttpUtil initDefaultHeaders(Map<String, String> headers) {
        if (!defaultHeaders.isEmpty())
            defaultHeaders.clear();
        defaultHeaders.putAll(headers);
        return this;
    }

    private void getAsyncCore(Request.Builder builder, final OkHttpCallback callback) {
        if (hasDefaultHeaders())
            builder.headers(buildHeaders(defaultHeaders))
                    .build();
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (callback != null)
                    callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (callback != null)
                        callback.onFailure(response.message());
                } else {
                    if (callback != null)
                        callback.onSuccess(response);
                }
            }
        });
    }


    private void postAsyncCore(Request.Builder builder, final OkHttpCallback callback) {
        if (hasDefaultHeaders())
            builder.headers(buildHeaders(defaultHeaders));
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (callback != null)
                    callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (callback != null)
                        callback.onSuccess(response);
                } else {
                    if (callback != null)
                        callback.onFailure(response.message());
                }
            }
        });

    }


    public void getAsync(String url, Map<String, String> params, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(buildUrlParams(url, params));
        getAsyncCore(builder, callback);
    }

    public void getAsync(String url, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url);
        getAsyncCore(builder, callback);
    }


    public void postAsync(String url, Map<String, String> params, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url).post(buildFormRequestBody(params));
        postAsyncCore(builder, callback);
    }

    /**
     * RequestBody requestBody = new MultipartBuilder()
     * .type(MultipartBuilder.FORM)
     * .addPart(
     * Headers.of("Content-Disposition", "form-data; name=\"title\""),
     * RequestBody.create(null, "Square Logo"))
     * .addPart(
     * Headers.of("Content-Disposition", "form-data; name=\"image\""),
     * RequestBody.create(MEDIA_TYPE_PNG, new File("website/static/logo-square.png")))
     * .build();
     *
     * @param url
     * @param requestBody
     * @param callback
     */
    public void postAsync(String url, RequestBody requestBody, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        postAsyncCore(builder, callback);
    }

    public void postAsync(String url, Object param, OkHttpCallback callback) {

        RequestBody body;
        if (param instanceof File)
            body = RequestBody.create(MEDIA_TYPE, (File) param);
        else if (param instanceof String)
            body = RequestBody.create(MEDIA_TYPE, (String) param);
        else
            body = RequestBody.create(MEDIA_TYPE, ObjectUtil.toByteArray(param));
        Request.Builder builder = new Request.Builder().url(url).post(body);
        postAsyncCore(builder, callback);
    }

    public void postAsync(String url, final InputStream is, OkHttpCallback callback) {

        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MEDIA_TYPE;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                byte[] buffer = new byte[2048];
                while ((is.read(buffer)) != -1) {
                    sink.outputStream().write(buffer);
                }
                sink.outputStream().flush();
                is.close();
                sink.outputStream().close();
            }
        };

        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        postAsyncCore(builder, callback);
    }


    private static RequestBody buildFormRequestBody(Map<String, String> params) {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (Map.Entry<String, String> param : params.entrySet())
            builder.add(param.getKey(), param.getValue());
        return builder.build();
    }


    private static Headers buildHeaders(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.set(header.getKey(), header.getValue());
        }
        return builder.build();
    }


    private static String buildUrlParams(String url, Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder(url).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            stringBuilder.append(entry.getKey()).append("=").append(entry.getKey()).append("&");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}
