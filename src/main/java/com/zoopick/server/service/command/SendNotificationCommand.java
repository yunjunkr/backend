package com.zoopick.server.service.command;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SendNotificationCommand(String title, String body, NotificationPayload payload) {

}
