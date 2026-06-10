package com.talkify.common.id;

import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

public class UUIDv7Generator {
    
    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
