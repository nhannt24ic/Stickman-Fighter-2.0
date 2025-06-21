package org.keyyh.stickmanfighter.server.game;

import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.data.Pose;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickmanCharacterServer {
    public final UUID id;
    public double x, y;
    public boolean isFacingRight;

    private List<Pose> runRightKeyframes, runLeftKeyframes, jumpKeyframes;
    private Pose currentPose;
    private long lastFrameTime = 0;
    private int currentRunFrame = -1;
    private final int TIME_PER_RUN_FRAME_MAX = 20;

    private boolean isJumping = false;
    private double jumpInitialSpeed = -15.0;
    private double gravity = 1;
    private double movingMaxSpeed = 9;
    private double currentVerticalSpeed = 0;
    private final int groundLevel = 450;

    public StickmanCharacterServer(UUID id, double startX, double startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.isFacingRight = true;

        initializeRunRightKeyframes();
        initializeRunLeftKeyframes();
        //initializeJumpKeyframes(); // Sẽ làm sau
        setToIdlePose();
    }

    // --- HÀM UPDATE LOGIC CHÍNH CỦA SERVER ---
    public void update(InputPacket input) {
        boolean wantMove = input.moveLeft || input.moveRight;

        // Xử lý di chuyển ngang
        if (wantMove) {
            if (input.moveLeft) {
                this.x -= movingMaxSpeed;
                this.isFacingRight = false;
            }
            if (input.moveRight) {
                this.x += movingMaxSpeed;
                this.isFacingRight = true;
            }
        }

        // Xử lý vật lý (trọng lực và va chạm đất đơn giản)
        this.y += currentVerticalSpeed;
        currentVerticalSpeed += gravity;
        if (this.y > groundLevel) {
            this.y = groundLevel;
            currentVerticalSpeed = 0;
            isJumping = false;
        }

        // --- XỬ LÝ ANIMATION ---
        if (wantMove && !isJumping) { // Nếu đang muốn di chuyển và đang trên mặt đất
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime > TIME_PER_RUN_FRAME_MAX) {
                lastFrameTime = currentTime;
                List<Pose> currentAnimation = isFacingRight ? runRightKeyframes : runLeftKeyframes;
                currentRunFrame = (currentRunFrame + 1) % currentAnimation.size();
                this.currentPose = currentAnimation.get(currentRunFrame);
            }
        } else {
            setToIdlePose();
        }
    }

    public Pose getCurrentPose() {
        return this.currentPose;
    }

    // --- CÁC HÀM KHỞI TẠO ANIMATION (SAO CHÉP TỪ CLIENT) ---
    private void applyPose(Pose pose) {
        this.currentPose = pose;
    }

    private void setToIdlePose() {
        // Tư thế nghỉ đơn giản, bạn có thể copy tư thế nghỉ đầy đủ từ client
        this.currentPose = new Pose(0,0,125,-10,55,10,115,-5,65,5);
    }

    private void initializeRunLeftKeyframes(){
        runLeftKeyframes = new ArrayList<>();
        for (Pose rightPose : runRightKeyframes) {
            double torso = -Math.toDegrees(rightPose.torso);
            double neck = -Math.toDegrees(rightPose.neck);
            double shoulderL = 180 - Math.toDegrees(rightPose.shoulderR);
            double shoulderR = 180 - Math.toDegrees(rightPose.shoulderL);
            double hipL = 180 - Math.toDegrees(rightPose.hipR);
            double hipR = 180 - Math.toDegrees(rightPose.hipL);
            double elbowL = -Math.toDegrees(rightPose.elbowR);
            double elbowR = -Math.toDegrees(rightPose.elbowL);
            double kneeL = -Math.toDegrees(rightPose.kneeR);
            double kneeR = -Math.toDegrees(rightPose.kneeL);
            runLeftKeyframes.add(new Pose(torso, neck, shoulderL, elbowL, shoulderR, elbowR, hipL, kneeL, hipR, kneeR));
        }
    }

    private void initializeRunRightKeyframes() {
        runRightKeyframes = new ArrayList<>();
        runRightKeyframes.add(new Pose(50, 0,  175 - 50 , -20, 40 - 40-10 , -110, 135, 10, 20, 80));
        runRightKeyframes.add(new Pose(50, 0,  125 -10, -40, 30-10, -110, 125, 20, 35, 90));
        runRightKeyframes.add(new Pose(50, 0,  115-10, -90, 70-10, -110,   100, 40,   50, 70));
        runRightKeyframes.add(new Pose(50, 0,  100-10, -110, 100-10, -110,   70, 50,   70, 50));
        runRightKeyframes.add(new Pose(50, 0,  70-10, -110, 115-10, -90,   50, 70,   100, 40));
        runRightKeyframes.add(new Pose(50, 0,  30-10, -110, 125-10, -40,   35, 90,   125, 20));
        runRightKeyframes.add(new Pose(50, 0,  0-10, -110, 135-10, -20, 20, 80, 135, 10));
    }
}