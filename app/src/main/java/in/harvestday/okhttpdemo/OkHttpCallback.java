package in.harvestday.okhttpdemo;

import com.squareup.okhttp.Response;

/**
 * Created by Administrator on 2015/9/10.
 */
public abstract class OkHttpCallback {
    public void onPreExcute() {
    }

    public abstract void onSuccess(Response response);

    public abstract void onFailure(String errMsg);

    public void onAfterExcute() {

    }
}
