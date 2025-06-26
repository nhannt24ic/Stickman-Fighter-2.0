package org.keyyh.stickmanfighter.client.game;

import org.keyyh.stickmanfighter.client.gui.GameScreen;
import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.data.Pose;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.CharacterFSMState;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;
import org.keyyh.stickmanfighter.common.game.GameConstants;
import org.keyyh.stickmanfighter.common.game.Skeleton;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickmanCharacter {
    // --- ID & Dữ liệu hiển thị cơ bản ---
    private final UUID id;
    private Color characterColor;
    private float lineWidth = 6f;

    // --- Trạng thái ---
    public double rootX, rootY;
    private boolean isFacingRight;
    private Pose currentPose;
    private Shape activeHitbox;
    private double health;
    private double stamina;

    // private boolean isJumping = false;
    private double currentVerticalSpeed = 0;

    private double startX, startY, targetX, targetY;
    private long startTime, targetTime;

    private final Skeleton skeleton = new Skeleton();

    private CharacterFSMState fsmState;
    // private int currentFrame;
    private long actionStartTime;
    // private long lastFrameTime;

    public StickmanCharacter(UUID id, double rootX, double rootY, Color characterColor, boolean isAI) {
        this.id = id;
        this.rootX = rootX;
        this.rootY = rootY;
        this.characterColor = characterColor;
        this.isFacingRight = true;

        this.health = GameConstants.MAX_HEALTH;
        this.stamina = GameConstants.MAX_STAMINA;

        this.startX = this.targetX = rootX;
        this.startY = this.targetY = rootY;
        this.startTime = this.targetTime = System.currentTimeMillis();

        this.currentPose = new Pose(0, 0, 125, -10, 55, 10, 115, -5, 65, 5);
        this.skeleton.updatePointsFromPose(this.rootX, this.rootY, this.currentPose);

        this.fsmState = CharacterFSMState.IDLE;
        this.actionStartTime = System.currentTimeMillis();
    }

    public void updateLocalPlayer(GameScreen.InputHandler inputHandler) {
        InputPacket input = inputHandler.getCurrentInputPacket();
        long currentTime = System.currentTimeMillis();
        updatePhysics(currentTime);

        switch (fsmState) {
            case IDLE: case RUNNING: case LANDING:
                handleControllableStateInput(input, currentTime);
                break;
            case BLOCKING:
                if (input.primaryAction != PlayerAction.BLOCK) { setToIdle(); }
                break;
            case CROUCHING:
                if (input.primaryAction == PlayerAction.KICK && input.modifiers.contains(ActionModifier.S)) {
                    startAction(CharacterFSMState.KICK_LOW, currentTime);
                } else if (input.primaryAction != PlayerAction.CROUCH) {
                    setToIdle();
                }
                break;
            case PUNCH_NORMAL:
                if (isActionFinished(currentTime, 400)) { setToIdle(); }
                break;
            case KICK_NORMAL:
                if (isActionFinished(currentTime, 450)) { setToIdle(); }
                break;
            case DASHING:
                if (isActionFinished(currentTime, 500)) { setToIdle(); }
                break;
            case JUMPING: case FALLING:
                handleAirborneInput(input, currentTime);
                break;
            default:
                setToIdle();
                break;
        }
        updateAnimation(currentTime);
        this.skeleton.updatePointsFromPose(this.rootX, this.rootY, this.currentPose);
    }

    private void handleControllableStateInput(InputPacket input, long currentTime) {
        switch (input.primaryAction) {
            case BLOCK:
                startAction(CharacterFSMState.BLOCKING, currentTime);
                break;
            case DASH:
                if (input.modifiers.contains(ActionModifier.D)) {
                    this.rootX += GameConstants.DASH_DISTANCE;
                    this.isFacingRight = true;
                } else if (input.modifiers.contains(ActionModifier.A)) {
                    this.rootX -= GameConstants.DASH_DISTANCE;
                    this.isFacingRight = false;
                }
                startAction(CharacterFSMState.DASHING, currentTime);
                break;
            case PUNCH:
                if (input.modifiers.contains(ActionModifier.W)) startAction(CharacterFSMState.PUNCH_HOOK, currentTime);
                else if (input.modifiers.contains(ActionModifier.ENTER)) startAction(CharacterFSMState.PUNCH_HEAVY, currentTime);
                else startAction(CharacterFSMState.PUNCH_NORMAL, currentTime);
                break;
            case KICK:
                if (input.modifiers.contains(ActionModifier.W)) startAction(CharacterFSMState.KICK_HIGH, currentTime);
                else if (input.modifiers.contains(ActionModifier.S)) startAction(CharacterFSMState.KICK_LOW, currentTime);
                else startAction(CharacterFSMState.KICK_NORMAL, currentTime);
                break;
            case JUMP:
                if ((fsmState == CharacterFSMState.IDLE || fsmState == CharacterFSMState.RUNNING)) {
                    this.currentVerticalSpeed = GameConstants.JUMP_INITIAL_SPEED;
                    startAction(CharacterFSMState.JUMPING, currentTime);
                }
                break;
            case CROUCH:
                startAction(CharacterFSMState.CROUCHING, currentTime);
                break;
            case MOVE:
                if (this.fsmState != CharacterFSMState.RUNNING) {
                }
                this.fsmState = CharacterFSMState.RUNNING;
                if (input.modifiers.contains(ActionModifier.A)) {
                    this.rootX -= GameConstants.SPEED_X; this.isFacingRight = false;
                }
                if (input.modifiers.contains(ActionModifier.D)) {
                    this.rootX += GameConstants.SPEED_X; this.isFacingRight = true;
                }
                break;
            case IDLE:
            default:
                if (this.fsmState == CharacterFSMState.RUNNING) {
                    setToIdle();
                }
                break;
        }
    }

    private void handleAirborneInput(InputPacket input, long currentTime) {
        if (input.primaryAction == PlayerAction.MOVE) {
            if (input.modifiers.contains(ActionModifier.A)) this.rootX -= GameConstants.SPEED_X / 2;
            if (input.modifiers.contains(ActionModifier.D)) this.rootX += GameConstants.SPEED_X / 2;
        }
        if (input.primaryAction == PlayerAction.KICK && input.modifiers.contains(ActionModifier.ENTER)) {
            startAction(CharacterFSMState.KICK_AERIAL, currentTime);
        }
    }

    private void updatePhysics(long currentTime) {
        this.rootY += this.currentVerticalSpeed;
        this.currentVerticalSpeed += GameConstants.GRAVITY;

        if (this.fsmState == CharacterFSMState.JUMPING && this.currentVerticalSpeed >= 0) {
            this.fsmState = CharacterFSMState.FALLING;
        }

        boolean onGround = (this.rootY >= GameConstants.GROUND_LEVEL);
        if (onGround) {
            this.rootY = GameConstants.GROUND_LEVEL;
            this.currentVerticalSpeed = 0;

            if (this.fsmState == CharacterFSMState.FALLING) {
                startAction(CharacterFSMState.LANDING, currentTime);
            }
        }
    }

    private void startAction(CharacterFSMState newState, long currentTime) {
        this.fsmState = newState;
        this.actionStartTime = currentTime;
    }

    private void setToIdle() {
        this.fsmState = CharacterFSMState.IDLE;
    }

    private boolean isActionFinished(long currentTime, long duration) {
        return currentTime - actionStartTime > duration;
    }

    // private void updateAnimation(long currentTime) {
    //     this.currentPose = this.currentPose;
    // }

    public void reconcile(CharacterState serverState) {
        this.health = serverState.health;
        this.stamina = serverState.stamina;
        this.activeHitbox = serverState.activeHitbox;

        this.startX = this.rootX;
        this.startY = this.rootY;
        this.startTime = System.currentTimeMillis();

        this.targetX = serverState.x;
        this.targetY = serverState.y;
        this.targetTime = this.startTime + 100;

        this.currentPose = serverState.currentPose;
        this.isFacingRight = serverState.isFacingRight;

        // Cập nhật FSM và animation frame
        this.fsmState = serverState.fsmState;
        // this.currentFrame = serverState.currentFrame;
        // this.actionStartTime = serverState.actionStartTime;
        // this.lastFrameTime = serverState.lastFrameTime;

        this.skeleton.updatePointsFromPose(this.rootX, this.rootY, this.currentPose);
    }

    public void addState(CharacterState newState, long packetTimestamp) {
        this.startX = this.targetX;
        this.startY = this.targetY;
        this.startTime = this.targetTime;

        this.targetX = newState.x;
        this.targetY = newState.y;
        this.targetTime = packetTimestamp;

        this.isFacingRight = newState.isFacingRight;
        this.activeHitbox = newState.activeHitbox;
        this.health = newState.health;
        this.stamina = newState.stamina;
        this.currentPose = newState.currentPose;

        this.fsmState = newState.fsmState;
        // this.currentFrame = newState.currentFrame;
        // this.actionStartTime = newState.actionStartTime;
        // this.lastFrameTime = newState.lastFrameTime;
    }

    public void interpolate(long renderTime) {
        if (renderTime >= targetTime || startTime >= targetTime) {
            this.rootX = targetX;
            this.rootY = targetY;
        } else {
            float t = (float)(renderTime - startTime) / (float)(targetTime - startTime);
            t = Math.max(0.0f, Math.min(t, 1.0f));
            this.rootX = startX + (targetX - startX) * t;
            this.rootY = startY + (targetY - startY) * t;
        }
        skeleton.updatePointsFromPose(this.rootX, this.rootY, this.currentPose);
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(this.characterColor != null ? this.characterColor : Color.BLACK);
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
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(activeHitbox);
        }
    }

    public List<Shape> getHurtboxes() {
        List<Shape> hurtboxes = new ArrayList<>();
        if (this.currentPose == null) return hurtboxes;

        Rectangle torsoRect = new Rectangle(-15, (int) -this.skeleton.getTorsoLength(), 30, (int) this.skeleton.getTorsoLength());
        AffineTransform torsoTransform = new AffineTransform();
        torsoTransform.translate(skeleton.hip.x, skeleton.hip.y);
        torsoTransform.rotate(this.currentPose.torso);
        hurtboxes.add(torsoTransform.createTransformedShape(torsoRect));

        hurtboxes.add(new Ellipse2D.Double(
                skeleton.headCenter.x - this.skeleton.getHeadRadius(),
                skeleton.headCenter.y - this.skeleton.getHeadRadius(),
                this.skeleton.getHeadRadius() * 2,
                this.skeleton.getHeadRadius() * 2
        ));
        return hurtboxes;
    }

    public UUID getId() { return id; }
    public boolean isFacingRight() { return this.isFacingRight; }
    public Point2D.Double getHeadCenter() { return skeleton.headCenter; }
    public double getHeadRadius() { return this.skeleton.getHeadRadius(); }
    public double getHealth() { return this.health; }
    public double getStamina() { return this.stamina; }
    public Skeleton getSkeleton() { return this.skeleton; }
}