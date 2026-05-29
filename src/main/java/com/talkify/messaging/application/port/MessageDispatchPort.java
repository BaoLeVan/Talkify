package com.talkify.messaging.application.port;

import com.talkify.messaging.application.event.MessageDispatchEvent;

public interface MessageDispatchPort {
    void dispatch(MessageDispatchEvent event);
}
