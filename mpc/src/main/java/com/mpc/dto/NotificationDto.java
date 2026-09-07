package com.mpc.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private String type;   // FRIEND_REQUEST, FRIEND_ACCEPTED, GROUP_JOIN_REQUEST, GROUP_JOIN_APPROVED, GROUP_JOIN_REJECTED, KICKED, ANNOUNCEMENT
    private Object payload;
}
