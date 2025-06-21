package org.keyyh.stickmanfighter.common.data;

import java.io.Serializable;

public class Pose implements Serializable {
    private static final long serialVersionUID = 1L;

    public double torso, neck, shoulderL, elbowL, shoulderR, elbowR, hipL, kneeL, hipR, kneeR;

    public Pose() {}

    public Pose(double torso, double neck, double shoulderL, double elbowL, double shoulderR, double elbowR, double hipL, double kneeL, double hipR, double kneeR) {
        this.torso = Math.toRadians(torso); this.neck = Math.toRadians(neck);
        this.shoulderL = Math.toRadians(shoulderL); this.elbowL = Math.toRadians(elbowL);
        this.shoulderR = Math.toRadians(shoulderR); this.elbowR = Math.toRadians(elbowR);
        this.hipL = Math.toRadians(hipL); this.kneeL = Math.toRadians(kneeL);
        this.hipR = Math.toRadians(hipR); this.kneeR = Math.toRadians(kneeR);
    }
}