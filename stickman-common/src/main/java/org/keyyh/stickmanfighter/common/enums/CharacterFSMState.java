package org.keyyh.stickmanfighter.common.enums;

public enum CharacterFSMState {
    // Trạng thái cơ bản
    IDLE,
    RUNNING,
    CROUCHING,
    JUMPING,
    FALLING,
    LANDING,
    BLOCKING,
    DASHING,

    // Trạng thái tấn công
    PUNCH_NORMAL,
    PUNCH_HOOK,
    PUNCH_HEAVY,
    KICK_NORMAL,
    KICK_LOW,
    KICK_HIGH,
    KICK_AERIAL,

    // Trạng thái bị tấn công
    HIT_STUN,
    KNOCK_UP,
    KNOCKED_DOWN,
    KNOCK_BACK
}