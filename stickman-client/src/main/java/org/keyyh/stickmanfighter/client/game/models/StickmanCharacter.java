package org.keyyh.stickmanfighter.client.game.models;

import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.Pose;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickmanCharacter {
    private final UUID id;
    private final boolean isAI;
    public double rootX, rootY;
    private Color characterColor;
    // private boolean isFacingRight;

    private double startX, startY;
    private double targetX, targetY;
    private long startTime, targetTime;

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
        this.rootX = rootX;
        this.rootY = rootY;
        this.characterColor = characterColor;
        this.isAI = isAI;

        this.startX = this.targetX = rootX;
        this.startY = this.targetY = rootY;
        this.startTime = this.targetTime = System.currentTimeMillis();

        hip = new Point2D.Double(); neck = new Point2D.Double(); headCenter = new Point2D.Double();
        shoulderL = new Point2D.Double(); elbowL = new Point2D.Double(); wristL = new Point2D.Double();
        shoulderR = new Point2D.Double(); elbowR = new Point2D.Double(); wristR = new Point2D.Double();
        kneeL = new Point2D.Double(); footL = new Point2D.Double();
        kneeR = new Point2D.Double(); footR = new Point2D.Double();

        initializeRunRightKeyframes();
        initializeRunLeftKeyframes();
        initializeJumpKeyframes();
        setToIdlePose();
        updatePointsFromAngles();
    }

    public void addState(CharacterState newState, long packetTimestamp) {
        // Di chuyển trạng thái "đích" hiện tại thành trạng thái "bắt đầu"
        this.startX = this.targetX;
        this.startY = this.targetY;
        this.startTime = this.targetTime;

        // Đặt trạng thái mới làm "đích"
        this.targetX = newState.x;
        this.targetY = newState.y;
        this.targetTime = packetTimestamp;

        // Cập nhật các trạng thái khác không cần nội suy
        // this.isFacingRight = newState.isFacingRight;
        if (newState.currentPose != null) {
            applyPose(newState.currentPose);
        } else {
            setToIdlePose();
        }
    }

    public void interpolate(long renderTime) {
        if (targetTime == startTime) {
            // Nếu chỉ có một điểm dữ liệu, không nội suy
            return;
        }

        // Tính toán hệ số nội suy 't' (một giá trị từ 0.0 đến 1.0)
        float t = (float)(renderTime - startTime) / (float)(targetTime - startTime);
        t = Math.max(0.0f, Math.min(t, 1.0f)); // Giới hạn t trong khoảng [0, 1]

        // Áp dụng công thức nội suy tuyến tính (Lerp) cho vị trí
        this.rootX = startX + (targetX - startX) * t;
        this.rootY = startY + (targetY - startY) * t;

        updatePointsFromAngles();
    }

    public UUID getId() { return id; }

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

    private void initializeJumpKeyframes() {
        jumpKeyframes = new ArrayList<>();
        jumpKeyframes.add(new Pose(0, 0, 190, -80, -10, 80, 160, -90, 20, 90));
        jumpKeyframes.add(new Pose(0, 0, 200, -30, -20, 30, 210, -140, -30, 140));
        jumpKeyframes.add(new Pose(0, 0, 200, -50, -20, 50, 200, -120, -20, 120));
        jumpKeyframes.add(new Pose(0, 0, 170, -40, 10, 40, 160, -90, 20, 90));
        jumpKeyframes.add(new Pose(0, 0, 150, -50, 30, 50, 160, -60, 20, 60));
        jumpKeyframes.add(new Pose(0, 0, 135, -30, 45, 30, 130, -30, 50, 30));
        jumpKeyframes.add(new Pose(0, 0, 125, -10, 55, 10, 115, -5, 65, 5));
    }

    public void applyPose(Pose pose) {
        this.torsoAngle = pose.torso;
        this.neckAngle = pose.neck;
        this.shoulderLAngle = pose.shoulderL;
        this.elbowLAngle = pose.elbowL;
        this.shoulderRAngle = pose.shoulderR;
        this.elbowRAngle = pose.elbowR;
        this.hipLAngle = pose.hipL;
        this.kneeLAngle = pose.kneeL;
        this.hipRAngle = pose.hipR;
        this.kneeRAngle = pose.kneeR;
    }

    public void setToIdlePose() {

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
}