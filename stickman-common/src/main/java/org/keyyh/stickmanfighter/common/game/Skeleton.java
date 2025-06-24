package org.keyyh.stickmanfighter.common.game;

import org.keyyh.stickmanfighter.common.data.Pose;
import java.awt.geom.Point2D;

public class Skeleton {
    // Kích thước cố định của bộ xương
    private final double headRadius = 17;
    private final double torsoLength = 60;
    private final double neckLength = 17;
    private final double shoulderWidthFromBodyCenter = 2;
    private final double upperArmLength = 35;
    private final double lowerArmLength = 40;
    private final double thighLength = 50;
    private final double calfLength = 55;

    // Các điểm khớp xương công khai để dễ dàng truy cập
    public final Point2D.Double hip = new Point2D.Double();
    public final Point2D.Double neck = new Point2D.Double();
    public final Point2D.Double headCenter = new Point2D.Double();
    public final Point2D.Double shoulderL = new Point2D.Double();
    public final Point2D.Double elbowL = new Point2D.Double();
    public final Point2D.Double wristL = new Point2D.Double();
    public final Point2D.Double shoulderR = new Point2D.Double();
    public final Point2D.Double elbowR = new Point2D.Double();
    public final Point2D.Double wristR = new Point2D.Double();
    public final Point2D.Double kneeL = new Point2D.Double();
    public final Point2D.Double footL = new Point2D.Double();
    public final Point2D.Double kneeR = new Point2D.Double();
    public final Point2D.Double footR = new Point2D.Double();

    private Pose lastAppliedPose;

    public Skeleton() {}

    public void updatePointsFromPose(double rootX, double rootY, Pose pose) {
        if (pose == null) return;
        this.lastAppliedPose = pose;

        this.hip.setLocation(rootX, rootY);
        double verticalUp = -Math.PI / 2;

        this.neck.setLocation(calculateEndPoint(this.hip, torsoLength, verticalUp + pose.torso));
        this.headCenter.setLocation(calculateEndPoint(this.neck, neckLength, verticalUp + pose.torso + pose.neck));
        this.shoulderL.setLocation(calculateEndPoint(this.neck, shoulderWidthFromBodyCenter, Math.PI + pose.torso));
        this.shoulderR.setLocation(calculateEndPoint(this.neck, shoulderWidthFromBodyCenter, 0 + pose.torso));
        this.elbowL.setLocation(calculateEndPoint(this.shoulderL, upperArmLength, pose.shoulderL + pose.torso));
        this.wristL.setLocation(calculateEndPoint(this.elbowL, lowerArmLength, pose.shoulderL + pose.elbowL + pose.torso));
        this.elbowR.setLocation(calculateEndPoint(this.shoulderR, upperArmLength, pose.shoulderR + pose.torso));
        this.wristR.setLocation(calculateEndPoint(this.elbowR, lowerArmLength, pose.shoulderR + pose.elbowR + pose.torso));
        this.kneeL.setLocation(calculateEndPoint(this.hip, thighLength, pose.hipL));
        this.footL.setLocation(calculateEndPoint(this.kneeL, calfLength, pose.hipL + pose.kneeL));
        this.kneeR.setLocation(calculateEndPoint(this.hip, thighLength, pose.hipR));
        this.footR.setLocation(calculateEndPoint(this.kneeR, calfLength, pose.hipR + pose.kneeR));
    }

    private Point2D.Double calculateEndPoint(Point2D.Double start, double length, double angle) {
        double endX = start.x + length * Math.cos(angle);
        double endY = start.y + length * Math.sin(angle);
        return new Point2D.Double(endX, endY);
    }

    // Getters cho các hằng số private
    public double getHeadRadius() { return headRadius; }
    public double getTorsoLength() { return torsoLength; }
    public Pose getLastAppliedPose() { return lastAppliedPose; }
    public Point2D.Double getHeadCenter() { return headCenter; }
}