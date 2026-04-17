package com.umc.linkyou.service.alarm;

import java.util.List;

public interface FcmSubscriber {

    void updateTopicSubscription(String token, List<String> topics, boolean shouldSubscribe);
}
