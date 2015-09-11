package in.harvestday.okhttpdemo;

/**
 * Created by Administrator on 2015/9/10.
 */
public abstract class OkHttpCallback<T> {
    public void onPreExcute() {
    }

    public abstract void onSuccess(T response);

    public abstract void onFailure(String errMsg);

    public void onAfterExcute() {

    }
}
