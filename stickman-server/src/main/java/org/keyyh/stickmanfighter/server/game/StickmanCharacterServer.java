package org.keyyh.stickmanfighter.server.game;

import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.data.Pose;
import org.keyyh.stickmanfighter.common.game.AnimationData;
import org.keyyh.stickmanfighter.common.game.GameConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickmanCharacterServer {
    public final UUID id;
    public double x, y;
    public boolean isFacingRight;
    public long lastUpdateTime;

    private boolean isJumping = false;
    private double currentVerticalSpeed = 0;
    private Pose currentPose;
    private int currentFrame = -1;
    private long lastFrameTime = 0;

    private final List<Pose> runRightKeyframes;
    private final List<Pose> runLeftKeyframes;
    private final List<Pose> jumpKeyframes;

    public StickmanCharacterServer(UUID id, double startX, double startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.isFacingRight = true;
        this.lastUpdateTime = System.currentTimeMillis();

        this.runRightKeyframes = AnimationData.createRunRightKeyframes();
        this.jumpKeyframes = AnimationData.createJumpKeyframes();
        this.runLeftKeyframes = new ArrayList<>();
        for (Pose rightPose : runRightKeyframes) {
            double torso = -Math.toDegrees(rightPose.torso); double neck = -Math.toDegrees(rightPose.neck);
            double shoulderL = 180 - Math.toDegrees(rightPose.shoulderR); double shoulderR = 180 - Math.toDegrees(rightPose.shoulderL);
            double hipL = 180 - Math.toDegrees(rightPose.hipR); double hipR = 180 - Math.toDegrees(rightPose.hipL);
            double elbowL = -Math.toDegrees(rightPose.elbowR); double elbowR = -Math.toDegrees(rightPose.elbowL);
            double kneeL = -Math.toDegrees(rightPose.kneeR); double kneeR = -Math.toDegrees(rightPose.kneeL);
            runLeftKeyframes.add(new Pose(torso, neck, shoulderL, elbowL, shoulderR, elbowR, hipL, kneeL, hipR, kneeR));
        }
        setToIdlePose();
    }

    public void update(InputPacket input) {
        long currentTime = System.currentTimeMillis();
        boolean isGrounded = (this.y >= GameConstants.GROUND_LEVEL);

        if (input.jump && isGrounded && !this.isJumping) {
            this.isJumping = true;
            this.currentVerticalSpeed = GameConstants.JUMP_INITIAL_SPEED;
            this.currentFrame = 0;
            this.lastFrameTime = currentTime;
        }

        boolean wantMove = input.moveLeft || input.moveRight;
        boolean allowMove = !isJumping;

        if (allowMove) {
            if (input.moveLeft) {
                this.x -= GameConstants.SPEED_X;
                this.isFacingRight = false;
            }
            if (input.moveRight) {
                this.x += GameConstants.SPEED_X;
                this.isFacingRight = true;
            }
        }

        this.y += this.currentVerticalSpeed;
        this.currentVerticalSpeed += GameConstants.GRAVITY;

        if (this.y > GameConstants.GROUND_LEVEL) {
            this.y = GameConstants.GROUND_LEVEL;
            this.currentVerticalSpeed = 0;
            if (this.isJumping) {

                this.isJumping = false;
                setToIdlePose();
            }
        }

        if (isJumping) {
            if (currentTime - lastFrameTime >= GameConstants.TIME_PER_JUMP_FRAME) {
                lastFrameTime = currentTime;
                if (currentFrame < jumpKeyframes.size()) {
                    this.currentPose = jumpKeyframes.get(currentFrame);
                    currentFrame++;
                }
            }
        } else if (wantMove) {
            if (currentTime - lastFrameTime > GameConstants.TIME_PER_RUN_FRAME) {
                lastFrameTime = currentTime;
                List<Pose> anim = isFacingRight ? runRightKeyframes : runLeftKeyframes;
                currentFrame = (currentFrame + 1) % anim.size();
                this.currentPose = anim.get(currentFrame);
            }
        } else {
            setToIdlePose();
        }
    }

    public Pose getCurrentPose() {
        return this.currentPose;
    }

    private void setToIdlePose() {
        this.currentPose = new Pose(0, 0, 125, -10, 55, 10, 115, -5, 65, 5);
        this.currentFrame = -1;
    }
}