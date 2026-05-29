package com.talkify.messaging.application.port;

import java.util.Map;

public interface CachePort {

    Map<Long, Long> getReadReceiptsForConversation(Long conversationId);
}
