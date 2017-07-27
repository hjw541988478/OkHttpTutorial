# OkHttpTutorial
封装了OkHttp的常用操作，能够实现各种参数的POST和GET请求。
## 初始化
再主``Activity``或者``Application``里面完成初始化。
```java
OkHttpUtil.getInstance().init();
```
## 使用
### Get操作
```java
OkHttpUtil.getInstance().getAsync(,,);
```
### POST
```java
OkHttpUtil.getInstance().postAsync(,,);
```
参数不一样，使用Callback进行回调。

## 博客
[OkHttp探索使用](http://huangjiawen.cn/android/OkHttp%E6%8E%A2%E7%B4%A2%E4%BD%BF%E7%94%A8/)
