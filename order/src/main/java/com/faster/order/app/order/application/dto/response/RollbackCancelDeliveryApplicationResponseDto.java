package com.faster.order.app.order.application.dto.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record RollbackCancelDeliveryApplicationResponseDto(
    UUID deliveryId
) {
}
