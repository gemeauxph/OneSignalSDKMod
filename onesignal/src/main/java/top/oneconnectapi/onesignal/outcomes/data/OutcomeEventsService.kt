package top.oneconnectapi.onesignal.outcomes.data

import top.oneconnectapi.onesignal.OneSignalApiResponseHandler
import org.json.JSONObject

interface OutcomeEventsService {
    fun sendOutcomeEvent(jsonObject: JSONObject, responseHandler: OneSignalApiResponseHandler)
}
