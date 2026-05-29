package com.talkify.messaging.interfaces.rest.response;

import java.util.Map;

public record GetReadReceiptsResponse(
    Map<Long, Long> readReceipts
) {
    public static GetReadReceiptsResponse from(Map<Long, Long> readReceipts) {
        return new GetReadReceiptsResponse(readReceipts);
    }
}
