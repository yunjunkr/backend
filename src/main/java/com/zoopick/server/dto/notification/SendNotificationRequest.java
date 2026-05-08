package com.zoopick.server.dto.notification;


import com.zoopick.server.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendNotificationRequest {
    private String title;
    private String body;
    @NotNull
    private NotificationType type;
    private Map<String, Object> payload = Map.of();
}
