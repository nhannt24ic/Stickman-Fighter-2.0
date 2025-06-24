package org.keyyh.stickmanfighter.common.game;

public final class GameConstants {
    private GameConstants() {}

    public static final double SPEED_X = 9.0;
    public static final double DASH_DISTANCE = 149.0;
    public static final double JUMP_INITIAL_SPEED = -15.0;
    public static final double GRAVITY = 1.0;
    public static final int GROUND_LEVEL = 450;

    public static final int TIME_PER_RUN_FRAME = 25;
    public static final int TIME_PER_JUMP_FRAME = 45;

    public static final double MAX_HEALTH = 1000;
    public static final double MAX_STAMINA = 500;

    // Chi phí thể lực
    public static final double JUMP_STAMINA_COST = 2;
    public static final double DASH_STAMINA_COST = 10;
    public static final double PUNCH_STAMINA_COST = 1;
    public static final double KICK_STAMINA_COST = 1.5;

    // Sát thương cơ bản
    public static final double PUNCH_DAMAGE = 25;
    public static final double KICK_DAMAGE = 40;
}