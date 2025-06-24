package org.keyyh.stickmanfighter.client.game;

import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.Pose;
import org.keyyh.stickmanfighter.common.game.Skeleton;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickmanCharacter {
    private final UUID id;
    private Color characterColor;
    private boolean isFacingRight;

    // Trạng thái dự đoán
    private boolean isJumping = false;
    private double currentVerticalSpeed = 0;

    // Vị trí gốc do client dự đoán hoặc do server ra lệnh
    public double rootX, rootY;

    // Trạng thái nội suy
    private double startX, startY, targetX, targetY;
    private long startTime, targetTime;

    // Dữ liệu hiển thị
    private final Skeleton skeleton = new Skeleton();
    private Line2D.Double activeHitbox;
    private float lineWidth = 6f;

    public StickmanCharacter(UUID id, double rootX, double rootY, Color characterColor, boolean isAI) {
        this.id = id;
        this.rootX = rootX;
        this.rootY = rootY;
        this.characterColor = characterColor;

        this.startX = this.targetX = rootX;
        this.startY = this.targetY = rootY;
        this.startTime = this.targetTime = System.currentTimeMillis();

    }

    private void updateSkeleton(Pose pose) {
        if (pose != null) {
            this.skeleton.updatePointsFromPose(this.rootX, this.rootY, pose);
        }
    }

//    public void updateLocalPlayer(GameScreen.InputHandler inputHandler) {
//        boolean isGrounded = (this.rootY >= GameConstants.GROUND_LEVEL);
//        if (inputHandler.isJumpPressed() && isGrounded && !this.isJumping) {
//            this.isJumping = true;
//            this.currentVerticalSpeed = GameConstants.JUMP_INITIAL_SPEED;
//        }
//        boolean wantMove = inputHandler.isMoveLeft() || inputHandler.isMoveRight();
//        if (wantMove) {
//            if (inputHandler.isMoveLeft()) { this.rootX -= GameConstants.SPEED_X; this.isFacingRight = false; }
//            if (inputHandler.isMoveRight()) { this.rootX += GameConstants.SPEED_X; this.isFacingRight = true; }
//        }
//        this.rootY += this.currentVerticalSpeed;
//        this.currentVerticalSpeed += GameConstants.GRAVITY;
//        if (this.rootY > GameConstants.GROUND_LEVEL) {
//            this.rootY = GameConstants.GROUND_LEVEL;
//            this.currentVerticalSpeed = 0;
//            this.isJumping = false;
//        }
//        // Cập nhật bộ xương theo vị trí dự đoán và tư thế cuối cùng
//        updateSkeleton(this.skeleton.getLastAppliedPose());
//    }

    public void reconcile(CharacterState serverState) {
        this.rootX = serverState.x;
        this.rootY = serverState.y;
        this.isFacingRight = serverState.isFacingRight;
        this.activeHitbox = serverState.activeHitbox;
        updateSkeleton(serverState.currentPose);
    }

    public void addState(CharacterState newState, long packetTimestamp) {
        this.startX = this.targetX;
        this.startY = this.targetY;
        this.startTime = this.targetTime;
        this.targetX = newState.x;
        this.targetY = newState.y;
        this.targetTime = packetTimestamp;
        this.activeHitbox = newState.activeHitbox;
        if (newState.currentPose != null) {
            applyPoseToSkeleton(newState.currentPose);
        }
    }

    public void interpolate(long renderTime) {
        if (targetTime == startTime) {
            updateSkeleton(skeleton.getLastAppliedPose());
            return;
        }
        float t = (float)(renderTime - startTime) / (float)(targetTime - startTime);
        t = Math.max(0.0f, Math.min(t, 1.0f));
        this.rootX = startX + (targetX - startX) * t;
        this.rootY = startY + (targetY - startY) * t;
        updateSkeleton(skeleton.getLastAppliedPose());
    }

    public void applyPoseToSkeleton(Pose pose) {
        if (pose != null) {
            this.skeleton.updatePointsFromPose(this.rootX, this.rootY, pose);
        }
    }

    public List<Shape> getHurtboxes() {
        List<Shape> hurtboxes = new ArrayList<>();
        if (skeleton.getLastAppliedPose() == null) return hurtboxes;

        Rectangle torsoRect = new Rectangle(-5, (int) -this.skeleton.getTorsoLength(), 11, (int) this.skeleton.getTorsoLength());
        AffineTransform torsoTransform = new AffineTransform();
        torsoTransform.translate(skeleton.hip.x, skeleton.hip.y);
        torsoTransform.rotate(skeleton.getLastAppliedPose().torso);
        hurtboxes.add(torsoTransform.createTransformedShape(torsoRect));

        hurtboxes.add(new Ellipse2D.Double(
                skeleton.headCenter.x - this.skeleton.getHeadRadius(),
                skeleton.headCenter.y - this.skeleton.getHeadRadius(),
                this.skeleton.getHeadRadius() * 2,
                this.skeleton.getHeadRadius() * 2
        ));
        return hurtboxes;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(this.characterColor);
        g2d.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2d.drawLine((int) skeleton.hip.x, (int) skeleton.hip.y, (int) skeleton.neck.x, (int) skeleton.neck.y);
        g2d.drawOval((int) (skeleton.headCenter.x - skeleton.getHeadRadius()), (int) (skeleton.headCenter.y - skeleton.getHeadRadius()), (int) (skeleton.getHeadRadius() * 2), (int) (skeleton.getHeadRadius() * 2));
        g2d.drawLine((int) skeleton.neck.x, (int) skeleton.neck.y, (int) skeleton.shoulderL.x, (int) skeleton.shoulderL.y);
        g2d.drawLine((int) skeleton.shoulderL.x, (int) skeleton.shoulderL.y, (int) skeleton.elbowL.x, (int) skeleton.elbowL.y);
        g2d.drawLine((int) skeleton.elbowL.x, (int) skeleton.elbowL.y, (int) skeleton.wristL.x, (int) skeleton.wristL.y);
        g2d.drawLine((int) skeleton.hip.x, (int) skeleton.hip.y, (int) skeleton.kneeL.x, (int) skeleton.kneeL.y);
        g2d.drawLine((int) skeleton.kneeL.x, (int) skeleton.kneeL.y, (int) skeleton.footL.x, (int) skeleton.footL.y);
        g2d.drawLine((int) skeleton.neck.x, (int) skeleton.neck.y, (int) skeleton.shoulderR.x, (int) skeleton.shoulderR.y);
        g2d.drawLine((int) skeleton.shoulderR.x, (int) skeleton.shoulderR.y, (int) skeleton.elbowR.x, (int) skeleton.elbowR.y);
        g2d.drawLine((int) skeleton.elbowR.x, (int) skeleton.elbowR.y, (int) skeleton.wristR.x, (int) skeleton.wristR.y);
        g2d.drawLine((int) skeleton.hip.x, (int) skeleton.hip.y, (int) skeleton.kneeR.x, (int) skeleton.kneeR.y);
        g2d.drawLine((int) skeleton.kneeR.x, (int) skeleton.kneeR.y, (int) skeleton.footR.x, (int) skeleton.footR.y);

        drawDebugBoxes(g2d);
    }

    private void drawDebugBoxes(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 255, 70));
        for (Shape hurtbox : getHurtboxes()) {
            g2d.fill(hurtbox);
        }
        if (activeHitbox != null) {
            g2d.setColor(new Color(255, 0, 0, 120));
            g2d.setStroke(new BasicStroke(lineWidth));
            g2d.draw(activeHitbox);
        }
    }

    public UUID getId() { return id; }
    public boolean isFacingRight() { return this.isFacingRight; }
    public Point2D.Double getHeadCenter() { return skeleton.headCenter; }
    public double getHeadRadius() { return skeleton.getHeadRadius(); }
}