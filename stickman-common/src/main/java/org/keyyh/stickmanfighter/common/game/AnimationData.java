package org.keyyh.stickmanfighter.common.game;

import org.keyyh.stickmanfighter.common.data.Pose;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AnimationData {
    private AnimationData() {}

    private static Pose mirrorPose(Pose rightPose) {
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
        return new Pose(torso, neck, shoulderL, elbowL, shoulderR, elbowR, hipL, kneeL, hipR, kneeR);
    }

    // --- Dữ liệu animation gốc (hướng phải) ---
    public static List<Pose> createRunRightKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(40, 0,  175 - 40 , -20, 40 - 40, -110, 135, 10, 20, 80));
        keyframes.add(new Pose(40, 0,  125, -40, 30, -110, 125, 20, 35, 90));
        keyframes.add(new Pose(40, 0,  115, -90, 70, -110,   100, 40,   50, 70));
        keyframes.add(new Pose(40, 0,  100, -110, 100, -110,   70, 50,   70, 50));
        keyframes.add(new Pose(40, 0,  70, -110, 115, -90,   50, 70,   100, 40));
        keyframes.add(new Pose(40, 0,  30, -110, 125, -40,   35, 90,   125, 20));
        keyframes.add(new Pose(40, 0,  0, -110, 135, -20, 20, 80, 135, 10));
        return keyframes;
    }

    public static List<Pose> createPunchRightKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(10, 0, 90, -90, 60, 10, 120, 0, 60, 0));
        keyframes.add(new Pose(10, 0, 0, -20, 60, 10, 120, 0, 60, 0));
        keyframes.add(new Pose(10, 0, 45, -45, 60, 10, 120, 0, 60, 0));
        return keyframes;
    }

    public static List<Pose> createKickRightKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(-10, 0, 135, 0, 50, 0, 100, 0, 20, 90));
        keyframes.add(new Pose(-15, 0, 140, 0, 55, 0, 110, 0, -30, 10));
        return keyframes;
    }

    public static List<Pose> createBlockRightKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(-10, 0, 100, -120, 80, 120, 115, 0, 65, 0));
        return keyframes;
    }

    public static List<Pose> createDashRightKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(60, -20, 20, 20, 160, -45, 45, 30, 160, 20));
        return keyframes;
    }

    // --- Các hàm tạo animation lật ngược (hướng trái) ---
    public static List<Pose> createRunLeftKeyframes() {
        return createRunRightKeyframes().stream().map(AnimationData::mirrorPose).collect(Collectors.toList());
    }

    public static List<Pose> createPunchLeftKeyframes() {
        return createPunchRightKeyframes().stream().map(AnimationData::mirrorPose).collect(Collectors.toList());
    }

    public static List<Pose> createKickLeftKeyframes() {
        return createKickRightKeyframes().stream().map(AnimationData::mirrorPose).collect(Collectors.toList());
    }

    public static List<Pose> createBlockLeftKeyframes() {
        return createBlockRightKeyframes().stream().map(AnimationData::mirrorPose).collect(Collectors.toList());
    }

    public static List<Pose> createDashLeftKeyframes() {
        return createDashRightKeyframes().stream().map(AnimationData::mirrorPose).collect(Collectors.toList());
    }

    // Các animation không phân biệt hướng
    public static List<Pose> createJumpKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(0, 0, 190, -80, -10, 80, 160, -90, 20, 90));
        keyframes.add(new Pose(0, 0, 200, -30, -20, 30, 210, -140, -30, 140));
        keyframes.add(new Pose(0, 0, 200, -50, -20, 50, 200, -120, -20, 120));
        keyframes.add(new Pose(0, 0, 170, -40, 10, 40, 160, -90, 20, 90));
        keyframes.add(new Pose(0, 0, 150, -50, 30, 50, 160, -60, 20, 60));
        keyframes.add(new Pose(0, 0, 135, -30, 45, 30, 130, -30, 50, 30));
        keyframes.add(new Pose(0, 0, 125, -10, 55, 10, 115, -5, 65, 5));
        return keyframes;
    }

    public static List<Pose> createCrouchKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(45, -10, 80, -60, 100, -60, 100, 80, 80, 80));
        return keyframes;
    }

    public static List<Pose> createIdleKeyframes() {
        List<Pose> keyframes = new ArrayList<>();
        keyframes.add(new Pose(0, 0, 125, -10, 55, 10, 115, -5, 65, 5));
        return keyframes;
    }
}