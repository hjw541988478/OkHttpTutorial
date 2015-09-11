package in.harvestday.okhttpdemo;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okio.BufferedSink;

/**
 * 支持常用的Post和Get请求，并且支持相关参数初始化
 */
public class OkHttpUtil {

    private static final MediaType MEDIA_TYPE_STRING = MediaType.parse("text/plain;charset=utf-8");
    private static final MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream;charset=utf-8");
    private static int CACHE_SIZE = 10 * 1024 * 1024; //default 10MB
    private static OkHttpUtil instance = null;
    private OkHttpClient client = null;

    private Map<String, String> defaultHeaders = null;

    private OkHttpUtil() {
        init();
    }

    public static OkHttpUtil getInstance() {
        if (instance == null) {
            synchronized (OkHttpUtil.class) {
                instance = new OkHttpUtil();
            }
        }
        return instance;
    }

    /**
     * 默认初始化参数
     *
     * @return
     */
    private OkHttpUtil init() {
        client = new OkHttpClient();
        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setWriteTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        return this;
    }

    /**
     * 是否支持Cookie
     *
     * @param cookieEnable
     */
    public void setCookieEnable(boolean cookieEnable) {
        if (cookieEnable)
            client.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        else
            client.setCookieHandler(null);
    }

    /**
     * 初始化带有缓存目录的Client
     *
     * @param cacheDirectory
     * @param cacheSize
     * @return
     */
    public void init(String cacheDirectory, int cacheSize) {
        init();
        File cacheDir = new File(cacheDirectory);
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        if (CACHE_SIZE > 0)
            CACHE_SIZE = cacheSize;
        Cache cache = new Cache(cacheDir, CACHE_SIZE);
        client.setCache(cache);
    }

    /**
     * 设置默认的请求头
     *
     * @param headers
     * @return
     */
    public void setDefaultHeaders(Map<String, String> headers) {
        if (defaultHeaders == null)
            defaultHeaders = new HashMap<>();
        if (!defaultHeaders.isEmpty())
            defaultHeaders.clear();
        defaultHeaders.putAll(headers);
    }

    private boolean hasDefaultHeaders() {
        if (defaultHeaders != null)
            return !defaultHeaders.isEmpty();
        else
            return false;
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
                        callback.onSuccess(response.body().string());
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
                        callback.onSuccess(response.body().string());
                } else {
                    if (callback != null)
                        callback.onFailure(response.message());
                }
            }
        });

    }

    /**
     * 带参数的Get请求
     *
     * @param url
     * @param params
     * @param callback
     */
    public void getAsync(String url, Map<String, String> params, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(buildUrlParams(url, params));
        getAsyncCore(builder, callback);
    }

    /**
     * 异步Get请求
     *
     * @param url
     * @param callback
     */
    public void getAsync(String url, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url);
        getAsyncCore(builder, callback);
    }

    /**
     * 支持普通的表单参数的Post请求
     *
     * @param url
     * @param params
     * @param callback
     */
    public void postAsync(String url, Map<String, String> params, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url).post(buildFormRequestBody(params));
        postAsyncCore(builder, callback);
    }

    /**
     * 支持RequestBody参数构造的Post请求
     *
     * @param url
     * @param requestBody
     * @param callback
     */
    public void postAsync(String url, RequestBody requestBody, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        postAsyncCore(builder, callback);
    }

    /**
     * 支持不同类型构造的Post请求
     *
     * @param url
     * @param param
     * @param callback
     */
    public void postAsync(String url, Object param, OkHttpCallback callback) {

        RequestBody body;
        if (param instanceof File)
            body = RequestBody.create(MEDIA_TYPE_STREAM, (File) param);
        else if (param instanceof String)
            body = RequestBody.create(MEDIA_TYPE_STRING, (String) param);
        else
            body = RequestBody.create(MEDIA_TYPE_STREAM, ObjectUtil.toByteArray(param));
        Request.Builder builder = new Request.Builder().url(url).post(body);
        postAsyncCore(builder, callback);
    }

    /**
     * 支持InputStream类型构造的Post请求
     *
     * @param url
     * @param is
     * @param callback
     */
    public void postAsync(String url, final InputStream is, OkHttpCallback callback) {

        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MEDIA_TYPE_STRING;
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

    /**
     * 带文件上传的Post请求
     *
     * @param fileKeys
     * @param files
     * @param params
     * @return
     */
    public void postAsync(String url, String[] fileKeys, File[] files, Map<String, String> params, OkHttpCallback callback) {
        Request.Builder builder = new Request.Builder().url(url).post(buildMultipartFormRequest(fileKeys, files, params));
        postAsyncCore(builder, callback);
    }


    /**
     * 获取Mime类型
     *
     * @param path
     * @return
     */
    private String getMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }

    private RequestBody buildMultipartFormRequest(String[] fileKeys, File[] files, Map<String, String> params) {
        MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.addPart(Headers.of("Content-Disposition", "form-data;name=\"" + entry.getKey() + "\""), RequestBody.create(null, entry.getValue()));
        }
        if (files != null && fileKeys != null && files.length == fileKeys.length) {
            RequestBody fileBody = null;
            for (int i = 0; i < files.length; i++) {
                fileBody = RequestBody.create(MediaType.parse(getMimeType(files[i].getName())), files[i]);
                builder.addPart(Headers.of("Content-Disposition",
                        "form-data;name=\"" + fileKeys[i] + "\";filename=\"" + files[i].getName() + "\""), fileBody);
            }
        } else {
            try {
                throw new Exception("文件名参数长度和文件实体内容长度不一致");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return builder.build();
    }

    private RequestBody buildFormRequestBody(Map<String, String> params) {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (Map.Entry<String, String> param : params.entrySet())
            builder.add(param.getKey(), param.getValue());
        return builder.build();
    }


    private Headers buildHeaders(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.set(header.getKey(), header.getValue());
        }
        return builder.build();
    }


    private String buildUrlParams(String url, Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder(url).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            stringBuilder.append(entry.getKey()).append("=").append(entry.getKey()).append("&");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}
