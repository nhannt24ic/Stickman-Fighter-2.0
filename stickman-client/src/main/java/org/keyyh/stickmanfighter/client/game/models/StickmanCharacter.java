package org.keyyh.stickmanfighter.client.game.models;

import org.keyyh.stickmanfighter.client.gui.GameScreen;
import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.Pose;
import org.keyyh.stickmanfighter.common.game.AnimationData;
import org.keyyh.stickmanfighter.common.game.GameConstants;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickmanCharacter {
    private final UUID id;
    private final boolean isAI;
    public double rootX, rootY;
    private Color characterColor;
    private boolean isFacingRight;

    // Trạng thái dự đoán
    private boolean isJumping = false;
    private double currentVerticalSpeed = 0;

    // Trạng thái nội suy
    private double startX, startY, targetX, targetY;
    private long startTime, targetTime;

    // Dữ liệu hiển thị
    private float lineWidth = 6f;
    private final double headRadius = 17;
    private final double torsoLength = 60;
    private final double neckLength = 17;
    private final double shoulderWidthFromBodyCenter = 2;
    private final double upperArmLength = 40;
    private final double lowerArmLength = 35;
    private final double thighLength = 50;
    private final double calfLength = 45;

    private Point2D.Double hip, neck, headCenter, shoulderL, elbowL, wristL, shoulderR, elbowR, wristR, kneeL, footL, kneeR, footR;
    private double torsoAngle, neckAngle, shoulderLAngle, elbowLAngle, shoulderRAngle, elbowRAngle, hipLAngle, kneeLAngle, hipRAngle, kneeRAngle;

    private List<Pose> runRightKeyframes, runLeftKeyframes, jumpKeyframes;

    public StickmanCharacter(UUID id, double rootX, double rootY, Color characterColor, boolean isAI) {
        this.id = id;
        this.isAI = isAI;
        this.rootX = rootX;
        this.rootY = rootY;
        this.characterColor = characterColor;
        this.startX = this.targetX = rootX;
        this.startY = this.targetY = rootY;
        this.startTime = this.targetTime = System.currentTimeMillis();
        hip = new Point2D.Double(); neck = new Point2D.Double(); headCenter = new Point2D.Double();
        shoulderL = new Point2D.Double(); elbowL = new Point2D.Double(); wristL = new Point2D.Double();
        shoulderR = new Point2D.Double(); elbowR = new Point2D.Double(); wristR = new Point2D.Double();
        kneeL = new Point2D.Double(); footL = new Point2D.Double();
        kneeR = new Point2D.Double(); footR = new Point2D.Double();
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
        updatePointsFromAngles();
    }

    public void updateLocalPlayer(GameScreen.InputHandler inputHandler) {
        boolean isGrounded = (this.rootY >= GameConstants.GROUND_LEVEL);
        if (inputHandler.isJumpPressed() && isGrounded && !this.isJumping) {
            this.isJumping = true;
            this.currentVerticalSpeed = GameConstants.JUMP_INITIAL_SPEED;
        }
        boolean wantMove = inputHandler.isMoveLeft() || inputHandler.isMoveRight();
        if (wantMove) {
            if (inputHandler.isMoveLeft()) {
                this.rootX -= GameConstants.SPEED_X;
                this.isFacingRight = false;
            }
            if (inputHandler.isMoveRight()) {
                this.rootX += GameConstants.SPEED_X;
                this.isFacingRight = true;
            }
        }
        this.rootY += this.currentVerticalSpeed;
        this.currentVerticalSpeed += GameConstants.GRAVITY;
        if (this.rootY > GameConstants.GROUND_LEVEL) {
            this.rootY = GameConstants.GROUND_LEVEL;
            this.currentVerticalSpeed = 0;
            this.isJumping = false;
        }
        updatePointsFromAngles();
    }

    public void reconcile(CharacterState serverState) {
        this.rootX = serverState.x;
        this.rootY = serverState.y;
        this.isFacingRight = serverState.isFacingRight;
        if (serverState.currentPose != null) {
            applyPose(serverState.currentPose);
        } else {
            setToIdlePose();
        }
        updatePointsFromAngles();
    }

    public void addState(CharacterState newState, long packetTimestamp) {
        this.startX = this.targetX;
        this.startY = this.targetY;
        this.startTime = this.targetTime;
        this.targetX = newState.x;
        this.targetY = newState.y;
        this.targetTime = packetTimestamp;
        if (newState.currentPose != null) {
            applyPose(newState.currentPose);
        } else {
            setToIdlePose();
        }
    }

    public void interpolate(long renderTime) {
        if (targetTime == startTime) {
            updatePointsFromAngles();
            return;
        }
        float t = (float)(renderTime - startTime) / (float)(targetTime - startTime);
        t = Math.max(0.0f, Math.min(t, 1.0f));
        this.rootX = startX + (targetX - startX) * t;
        this.rootY = startY + (targetY - startY) * t;
        updatePointsFromAngles();
    }

    public UUID getId() { return id; }

    public void applyPose(Pose pose) {
        this.torsoAngle = pose.torso; this.neckAngle = pose.neck;
        this.shoulderLAngle = pose.shoulderL; this.elbowLAngle = pose.elbowL;
        this.shoulderRAngle = pose.shoulderR; this.elbowRAngle = pose.elbowR;
        this.hipLAngle = pose.hipL; this.kneeLAngle = pose.kneeL;
        this.hipRAngle = pose.hipR; this.kneeRAngle = pose.kneeR;
    }

    public void setToIdlePose() {
        applyPose(new Pose(0, 0, 125, -10, 55, 10, 115, -5, 65, 5));
    }

    private void updatePointsFromAngles() {
        this.hip.setLocation(rootX, rootY);
        double verticalUp = -Math.PI / 2;
        this.neck.setLocation(calculateEndPoint(this.hip, torsoLength, verticalUp + torsoAngle));
        this.headCenter.setLocation(calculateEndPoint(this.neck, neckLength, verticalUp + torsoAngle + neckAngle));
        this.shoulderL.setLocation(calculateEndPoint(this.neck, shoulderWidthFromBodyCenter, Math.PI + torsoAngle));
        this.shoulderR.setLocation(calculateEndPoint(this.neck, shoulderWidthFromBodyCenter, 0 + torsoAngle));
        this.elbowL.setLocation(calculateEndPoint(this.shoulderL, upperArmLength, shoulderLAngle + torsoAngle));
        this.wristL.setLocation(calculateEndPoint(this.elbowL, lowerArmLength, shoulderLAngle + elbowLAngle + torsoAngle));
        this.elbowR.setLocation(calculateEndPoint(this.shoulderR, upperArmLength, shoulderRAngle + torsoAngle));
        this.wristR.setLocation(calculateEndPoint(this.elbowR, lowerArmLength, shoulderRAngle + elbowRAngle + torsoAngle));
        this.kneeL.setLocation(calculateEndPoint(this.hip, thighLength, hipLAngle));
        this.footL.setLocation(calculateEndPoint(this.kneeL, calfLength, hipLAngle + kneeLAngle));
        this.kneeR.setLocation(calculateEndPoint(this.hip, thighLength, hipRAngle));
        this.footR.setLocation(calculateEndPoint(this.kneeR, calfLength, hipRAngle + kneeRAngle));
    }

    private Point2D.Double calculateEndPoint(Point2D.Double start, double length, double angle) {
        double endX = start.x + length * Math.cos(angle);
        double endY = start.y + length * Math.sin(angle);
        return new Point2D.Double(endX, endY);
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(this.characterColor);
        g2d.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int) hip.x, (int) hip.y, (int) neck.x, (int) neck.y);
        g2d.drawOval((int) (headCenter.x - headRadius), (int) (headCenter.y - headRadius), (int) (2 * headRadius), (int) (2 * headRadius));
        g2d.drawLine((int) neck.x, (int) neck.y, (int) shoulderL.x, (int) shoulderL.y);
        g2d.drawLine((int) shoulderL.x, (int) shoulderL.y, (int) elbowL.x, (int) elbowL.y);
        g2d.drawLine((int) elbowL.x, (int) elbowL.y, (int) wristL.x, (int) wristL.y);
        g2d.drawLine((int) hip.x, (int) hip.y, (int) kneeL.x, (int) kneeL.y);
        g2d.drawLine((int) kneeL.x, (int) kneeL.y, (int) footL.x, (int) footL.y);
        g2d.drawLine((int) neck.x, (int) neck.y, (int) shoulderR.x, (int) shoulderR.y);
        g2d.drawLine((int) shoulderR.x, (int) shoulderR.y, (int) elbowR.x, (int) elbowR.y);
        g2d.drawLine((int) elbowR.x, (int) elbowR.y, (int) wristR.x, (int) wristR.y);
        g2d.drawLine((int) hip.x, (int) hip.y, (int) kneeR.x, (int) kneeR.y);
        g2d.drawLine((int) kneeR.x, (int) kneeR.y, (int) footR.x, (int) footR.y);
    }

    public boolean isAI() { return isAI; }

    public Point2D.Double getHeadCenter() {
        return this.headCenter;
    }

    public double getHeadRadius() {
        return this.headRadius;
    }

    public boolean isFacingRight() {
        return this.isFacingRight;
    }
}