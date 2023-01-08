package top.oneconnectapi.onesignal.outcomes.domain

import top.oneconnectapi.onesignal.influence.domain.OSInfluenceChannel

open class OSCachedUniqueOutcome(
    private val influenceId: String,
    private val channel: OSInfluenceChannel
) {
    open fun getInfluenceId() = influenceId
    open fun getChannel() = channel
}
