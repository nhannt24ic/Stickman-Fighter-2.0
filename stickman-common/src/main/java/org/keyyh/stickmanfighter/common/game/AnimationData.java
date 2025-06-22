package org.keyyh.stickmanfighter.common.game;

import org.keyyh.stickmanfighter.common.data.Pose;
import java.util.ArrayList;
import java.util.List;

public final class AnimationData {
    private AnimationData() {}

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
}