package com.zoopick.server.service.command;

import com.zoopick.server.entity.NotificationType;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public interface NotificationPayload {
    NotificationType type();

    Map<String, String> toMap();
}
