package top.oneconnectapi.onesignal.outcomes.domain;

import top.oneconnectapi.onesignal.influence.domain.OSInfluenceChannel;

import static top.oneconnectapi.onesignal.influence.domain.OSInfluenceChannel.NOTIFICATION;

public class OSCachedUniqueOutcomeName extends OSCachedUniqueOutcome {

    private String name;

    public OSCachedUniqueOutcomeName(String name, String notificationId) {
        this(name, notificationId, NOTIFICATION);
    }

    public OSCachedUniqueOutcomeName(String name, String notificationId, String channel) {
        this(name, notificationId, OSInfluenceChannel.fromString(channel));
    }

    public OSCachedUniqueOutcomeName(String name, String notificationId, OSInfluenceChannel channel) {
        super(notificationId, channel);
        this.name = name;
    }

    @Override
    public String getInfluenceId() {
        return super.getInfluenceId();
    }

    public String getName() {
        return name;
    }

    @Override
    public OSInfluenceChannel getChannel() {
        return super.getChannel();
    }

}
