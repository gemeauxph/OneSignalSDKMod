package top.oneconnectapi.onesignal.outcomes.data

import top.oneconnectapi.onesignal.OneSignalAPIClient
import top.oneconnectapi.onesignal.OneSignalApiResponseHandler
import org.json.JSONObject

internal abstract class OSOutcomeEventsClient(val client: OneSignalAPIClient) : OutcomeEventsService {
    abstract override fun sendOutcomeEvent(jsonObject: JSONObject, responseHandler: OneSignalApiResponseHandler)
}
