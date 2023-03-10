package top.oneconnectapi.onesignal.outcomes.data

import top.oneconnectapi.onesignal.OSLogger
import top.oneconnectapi.onesignal.OSOutcomeEvent
import top.oneconnectapi.onesignal.OneSignalApiResponseHandler
import top.oneconnectapi.onesignal.influence.domain.OSInfluenceType
import top.oneconnectapi.onesignal.outcomes.OSOutcomeConstants
import top.oneconnectapi.onesignal.outcomes.domain.OSOutcomeEventParams
import org.json.JSONException

internal class OSOutcomeEventsV1Repository(
    logger: OSLogger,
    outcomeEventsCache: OSOutcomeEventsCache,
    outcomeEventsService: OutcomeEventsService
) : OSOutcomeEventsRepository(logger, outcomeEventsCache, outcomeEventsService) {
    override fun requestMeasureOutcomeEvent(appId: String, deviceType: Int, eventParams: OSOutcomeEventParams, responseHandler: OneSignalApiResponseHandler) {
        val event = OSOutcomeEvent.fromOutcomeEventParamsV2toOutcomeEventV1(eventParams)
        when (event.session) {
            OSInfluenceType.DIRECT -> requestMeasureDirectOutcomeEvent(appId, deviceType, event, responseHandler)
            OSInfluenceType.INDIRECT -> requestMeasureIndirectOutcomeEvent(appId, deviceType, event, responseHandler)
            OSInfluenceType.UNATTRIBUTED -> requestMeasureUnattributedOutcomeEvent(appId, deviceType, event, responseHandler)
            else -> {
            }
        }
    }

    private fun requestMeasureDirectOutcomeEvent(appId: String, deviceType: Int, event: OSOutcomeEvent, responseHandler: OneSignalApiResponseHandler) {
        try {
            event.toJSONObjectForMeasure()
                .put(OSOutcomeConstants.APP_ID, appId)
                .put(OSOutcomeConstants.DEVICE_TYPE, deviceType)
                .put(OSOutcomeConstants.DIRECT_PARAM, true)
                .also { jsonObject ->
                    outcomeEventsService.sendOutcomeEvent(jsonObject, responseHandler)
                }
        } catch (e: JSONException) {
            logger.error("Generating direct outcome:JSON Failed.", e)
        }
    }

    private fun requestMeasureIndirectOutcomeEvent(appId: String, deviceType: Int, event: OSOutcomeEvent, responseHandler: OneSignalApiResponseHandler) {
        try {
            event.toJSONObjectForMeasure()
                .put(OSOutcomeConstants.APP_ID, appId)
                .put(OSOutcomeConstants.DEVICE_TYPE, deviceType)
                .put(OSOutcomeConstants.DIRECT_PARAM, false)
                .also { jsonObject ->
                    outcomeEventsService.sendOutcomeEvent(jsonObject, responseHandler)
                }
        } catch (e: JSONException) {
            logger.error("Generating indirect outcome:JSON Failed.", e)
        }
    }

    private fun requestMeasureUnattributedOutcomeEvent(appId: String, deviceType: Int, event: OSOutcomeEvent, responseHandler: OneSignalApiResponseHandler) {
        try {
            event.toJSONObjectForMeasure()
                .put(OSOutcomeConstants.APP_ID, appId)
                .put(OSOutcomeConstants.DEVICE_TYPE, deviceType)
                .also { jsonObject ->
                    outcomeEventsService.sendOutcomeEvent(jsonObject, responseHandler)
                }
        } catch (e: JSONException) {
            logger.error("Generating unattributed outcome:JSON Failed.", e)
        }
    }
}
