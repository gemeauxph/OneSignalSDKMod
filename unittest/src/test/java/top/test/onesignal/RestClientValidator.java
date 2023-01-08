package top.test.onesignal;

import androidx.annotation.NonNull;

import top.oneconnectapi.onesignal.ShadowOneSignalRestClient.Request;

import org.json.JSONException;

import static top.test.onesignal.RestClientAsserts.assertHasAppId;
import static top.test.onesignal.RestClientAsserts.assertPlayerCreateAny;
import static top.test.onesignal.RestClientAsserts.assertRemoteParamsUrl;

import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;

// Validator runs on each mock REST API call on All tests to ensure the correct fields are sent
public class RestClientValidator {

   static final String GET_REMOTE_PARAMS_ENDPOINT = "android_params.js";

   public static void validateRequest(@NonNull ShadowOneSignalRestClient.Request request) throws JSONException {
      switch (request.method) {
         case GET:
            if (request.url.contains(GET_REMOTE_PARAMS_ENDPOINT))
               assertRemoteParamsUrl(request.url);
            break;
         case POST:
            if (request.url.endsWith("players"))
               assertPlayerCreateAny(request);
      }

      assertHasAppId(request);

      // TODO: Add rest of the REST API calls here
   }

}
