package top.oneconnectapi.onesignal.outcomes.data

import top.oneconnectapi.onesignal.OSLogger
import top.oneconnectapi.onesignal.OSSharedPreferences
import top.oneconnectapi.onesignal.OneSignalAPIClient
import top.oneconnectapi.onesignal.OneSignalDb
import top.oneconnectapi.onesignal.outcomes.domain.OSOutcomeEventsRepository

class OSOutcomeEventsFactory(private val logger: OSLogger, private val apiClient: OneSignalAPIClient, dbHelper: OneSignalDb?, preferences: OSSharedPreferences?) {
    private val outcomeEventsCache: OSOutcomeEventsCache = OSOutcomeEventsCache(logger, dbHelper!!, preferences!!)
    private var repository: OSOutcomeEventsRepository? = null

    fun getRepository(): OSOutcomeEventsRepository = if (repository != null) validateRepositoryVersion() else createRepository()

    private fun validateRepositoryVersion(): OSOutcomeEventsRepository {
        if (!outcomeEventsCache.isOutcomesV2ServiceEnabled && repository is OSOutcomeEventsV1Repository) {
            return repository!!
        }
        if (outcomeEventsCache.isOutcomesV2ServiceEnabled && repository is OSOutcomeEventsV2Repository) {
            return repository!!
        }
        return createRepository()
    }

    private fun createRepository() = if (outcomeEventsCache.isOutcomesV2ServiceEnabled) {
        OSOutcomeEventsV2Repository(logger, outcomeEventsCache, OSOutcomeEventsV2Service(apiClient))
    } else {
        OSOutcomeEventsV1Repository(logger, outcomeEventsCache, OSOutcomeEventsV1Service(apiClient))
    }
}
