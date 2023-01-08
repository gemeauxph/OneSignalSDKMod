package top.oneconnectapi.onesignal.outcomes.data

import top.oneconnectapi.onesignal.OneSignalAPIClient
import top.oneconnectapi.onesignal.OneSignalApiResponseHandler
import org.json.JSONObject

internal class OSOutcomeEventsV1Service(client: OneSignalAPIClient) : OSOutcomeEventsClient(client) {
    /***
     * API endpoint /api/v1/outcomes/measure
     */
    override fun sendOutcomeEvent(jsonObject: JSONObject, responseHandler: OneSignalApiResponseHandler) {
        client.post("outcomes/measure", jsonObject, responseHandler)
    }
}
