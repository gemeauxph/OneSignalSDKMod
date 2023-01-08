package top.oneconnectapi.onesignal.outcomes.domain

import top.oneconnectapi.onesignal.OneSignalApiResponseHandler
import top.oneconnectapi.onesignal.influence.domain.OSInfluence

interface OSOutcomeEventsRepository {
    fun getSavedOutcomeEvents(): List<OSOutcomeEventParams>
    fun saveOutcomeEvent(event: OSOutcomeEventParams)
    fun removeEvent(outcomeEvent: OSOutcomeEventParams)
    fun requestMeasureOutcomeEvent(appId: String, deviceType: Int, event: OSOutcomeEventParams, responseHandler: OneSignalApiResponseHandler)
    fun saveUniqueOutcomeNotifications(eventParams: OSOutcomeEventParams)
    fun getNotCachedUniqueOutcome(name: String, influences: List<OSInfluence>): List<OSInfluence>
    fun getUnattributedUniqueOutcomeEventsSent(): Set<String>?
    fun saveUnattributedUniqueOutcomeEventsSent(unattributedUniqueOutcomeEvents: Set<String>)
    fun cleanCachedUniqueOutcomeEventNotifications(notificationTableName: String, notificationIdColumnName: String)
}
