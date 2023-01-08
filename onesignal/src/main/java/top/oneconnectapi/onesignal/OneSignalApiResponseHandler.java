package top.oneconnectapi.onesignal;

public interface OneSignalApiResponseHandler {
    void onSuccess(String response);

    void onFailure(int statusCode, String response, Throwable throwable);
}