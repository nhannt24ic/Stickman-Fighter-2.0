package org.keyyh.stickmanfighter.server.game;

import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.data.Pose;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.CharacterFSMState;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;
import org.keyyh.stickmanfighter.common.game.AnimationData;
import org.keyyh.stickmanfighter.common.game.GameConstants;
import org.keyyh.stickmanfighter.common.game.Skeleton;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class StickmanCharacterServer {
    public final UUID id;
    public double x, y;
    public boolean isFacingRight;
    public long lastUpdateTime;
    public CharacterFSMState fsmState;

    private double health;
    private double stamina;

    private final Skeleton skeleton = new Skeleton();
    private double currentVerticalSpeed = 0;
    private Pose currentPose;
    private int currentFrame;
    private long actionStartTime;
    private long lastFrameTime;

    private final Set<UUID> hitTargetsInCurrentAction = new HashSet<>();

    private final List<Pose> jumpKeyframes, crouchKeyframes;
    private final List<Pose> idleFacingRightKeyframes, idleFacingLeftKeyframes;
    private final List<Pose> runRightKeyframes, runLeftKeyframes;
    private final List<Pose> punchRightKeyframes, punchLeftKeyframes;
    private final List<Pose> kickRightKeyframes, kickLeftKeyframes;
    private final List<Pose> blockRightKeyframes, blockLeftKeyframes;
    private final List<Pose> dashRightKeyframes,  dashLeftKeyframes;

    public StickmanCharacterServer(UUID id, double startX, double startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;

        this.health = GameConstants.MAX_HEALTH;
        this.stamina = GameConstants.MAX_STAMINA;

        this.isFacingRight = true;
        this.lastUpdateTime = System.currentTimeMillis();

        this.jumpKeyframes = AnimationData.createJumpKeyframes();
        this.crouchKeyframes = AnimationData.createCrouchKeyframes();

        this.idleFacingRightKeyframes = AnimationData.createIdleFacingRightKeyframes();
        this.idleFacingLeftKeyframes = AnimationData.createIdleFacingLeftKeyframes();

        this.blockRightKeyframes = AnimationData.createBlockRightKeyframes();
        this.blockLeftKeyframes = AnimationData.createBlockLeftKeyframes();

        this.dashRightKeyframes = AnimationData.createDashRightKeyframes();
        this.dashLeftKeyframes = AnimationData.createDashLeftKeyframes();

        this.runRightKeyframes = AnimationData.createRunRightKeyframes();
        this.runLeftKeyframes = AnimationData.createRunLeftKeyframes();

        this.punchRightKeyframes = AnimationData.createPunchRightKeyframes();
        this.punchLeftKeyframes = AnimationData.createPunchLeftKeyframes();

        this.kickRightKeyframes = AnimationData.createKickRightKeyframes();
        this.kickLeftKeyframes = AnimationData.createKickLeftKeyframes();

        setToIdle();
    }

    public void update(InputPacket input, long currentTime) {
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
                if (isActionFinished(currentTime, 500)) { setToIdle(); }
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
    }

    public void takeDamage(double damage) {
        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }
        // TODO: Chuyển sang trạng thái HIT_STUN khi bị đánh
        System.out.printf("Player %s took %.1f damage, remaining health: %.1f%n", this.id, damage, this.health);
    }

    private boolean canAfford(double cost) {
        return this.stamina >= cost;
    }

    private void spendStamina(double cost) {
        this.stamina -= cost;
        if (this.stamina < 0) this.stamina = 0;
    }

    private void updatePhysics(long currentTime) {
        this.y += this.currentVerticalSpeed;
        this.currentVerticalSpeed += GameConstants.GRAVITY;

        if (this.fsmState == CharacterFSMState.JUMPING && this.currentVerticalSpeed >= 0) {
            this.fsmState = CharacterFSMState.FALLING;
        }

        boolean onGround = (this.y >= GameConstants.GROUND_LEVEL);
        if (onGround) {
            this.y = GameConstants.GROUND_LEVEL;
            this.currentVerticalSpeed = 0;

            if (this.fsmState == CharacterFSMState.FALLING) {
                startAction(CharacterFSMState.LANDING, currentTime);
            }
        }
    }

    private void handleControllableStateInput(InputPacket input, long currentTime) {
        switch (input.primaryAction) {
            case BLOCK:
                startAction(CharacterFSMState.BLOCKING, currentTime);
                break;
            case DASH:
                if (canAfford(GameConstants.DASH_STAMINA_COST)){
                    spendStamina(GameConstants.DASH_STAMINA_COST);
                    if (input.modifiers.contains(ActionModifier.D)) {
                        this.x += GameConstants.DASH_DISTANCE;
                        this.isFacingRight = true;
                    } else if (input.modifiers.contains(ActionModifier.A)) {
                        this.x -= GameConstants.DASH_DISTANCE;
                        this.isFacingRight = false;
                    }
                    startAction(CharacterFSMState.DASHING, currentTime);
                }
                break;
            case PUNCH:
                if (canAfford(GameConstants.PUNCH_STAMINA_COST)){
                    spendStamina(GameConstants.PUNCH_STAMINA_COST);
                    if (input.modifiers.contains(ActionModifier.W)) startAction(CharacterFSMState.PUNCH_HOOK, currentTime);
                    else if (input.modifiers.contains(ActionModifier.ENTER)) startAction(CharacterFSMState.PUNCH_HEAVY, currentTime);
                    else startAction(CharacterFSMState.PUNCH_NORMAL, currentTime);
                }
                break;
            case KICK:
                if (canAfford(GameConstants.KICK_STAMINA_COST)){
                    spendStamina(GameConstants.KICK_STAMINA_COST);
                    if (input.modifiers.contains(ActionModifier.W)) startAction(CharacterFSMState.KICK_HIGH, currentTime);
                    else if (input.modifiers.contains(ActionModifier.S)) startAction(CharacterFSMState.KICK_LOW, currentTime);
                    else startAction(CharacterFSMState.KICK_NORMAL, currentTime);
                }
                break;
            case JUMP:
                if ((fsmState == CharacterFSMState.IDLE || fsmState == CharacterFSMState.RUNNING) && canAfford(GameConstants.JUMP_STAMINA_COST)) {
                    spendStamina(GameConstants.JUMP_STAMINA_COST);
                    this.currentVerticalSpeed = GameConstants.JUMP_INITIAL_SPEED;
                    startAction(CharacterFSMState.JUMPING, currentTime);
                }
                break;
            case CROUCH:
                startAction(CharacterFSMState.CROUCHING, currentTime);
                break;
            case MOVE:
                if (this.fsmState != CharacterFSMState.RUNNING) {
                    this.currentFrame = -1;
                }
                this.fsmState = CharacterFSMState.RUNNING;
                if (input.modifiers.contains(ActionModifier.A)) {
                    this.x -= GameConstants.SPEED_X; this.isFacingRight = false;
                }
                if (input.modifiers.contains(ActionModifier.D)) {
                    this.x += GameConstants.SPEED_X; this.isFacingRight = true;
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
            if (input.modifiers.contains(ActionModifier.A)) this.x -= GameConstants.SPEED_X / 2;
            if (input.modifiers.contains(ActionModifier.D)) this.x += GameConstants.SPEED_X / 2;
        }
        if (input.primaryAction == PlayerAction.KICK && input.modifiers.contains(ActionModifier.ENTER)) {
            startAction(CharacterFSMState.KICK_AERIAL, currentTime);
        }
    }

    private void updateAnimation(long currentTime) {
        long timePerFrame = (fsmState == CharacterFSMState.JUMPING || fsmState == CharacterFSMState.FALLING)
                ? GameConstants.TIME_PER_JUMP_FRAME : GameConstants.TIME_PER_RUN_FRAME;
        if (currentTime - lastFrameTime < timePerFrame) return;
        lastFrameTime = currentTime;
        List<Pose> currentAnimList = getAnimationForState(fsmState);
        boolean loop = isAnimationLooping(fsmState);
        if (currentAnimList.isEmpty()) {
            if(isFacingRight)
                this.currentPose = idleFacingRightKeyframes.get(0);
            else
                this.currentPose = idleFacingLeftKeyframes.get(0);
            return;
        }
        if (loop) {
            currentFrame = (currentFrame + 1) % currentAnimList.size();
        } else {
            if (currentFrame < currentAnimList.size() - 1) {
                currentFrame++;
            }
        }
        this.currentPose = currentAnimList.get(currentFrame);

        this.skeleton.updatePointsFromPose(this.x, this.y, this.currentPose);
    }

    private List<Pose> getAnimationForState(CharacterFSMState state) {
        switch (state) {
            case IDLE: case LANDING: return isFacingRight ? idleFacingRightKeyframes : idleFacingLeftKeyframes;
            case RUNNING: return isFacingRight ? runRightKeyframes : runLeftKeyframes;
            case JUMPING: case FALLING: return jumpKeyframes;
            case PUNCH_NORMAL: case PUNCH_HOOK: case PUNCH_HEAVY: return isFacingRight ? punchRightKeyframes : punchLeftKeyframes;
            case KICK_NORMAL: case KICK_AERIAL: case KICK_HIGH: case KICK_LOW: return isFacingRight ? kickRightKeyframes : kickLeftKeyframes;
            case BLOCKING: return isFacingRight ? blockRightKeyframes : blockLeftKeyframes;
            case CROUCHING: return crouchKeyframes;
            case DASHING: return isFacingRight ? dashRightKeyframes : dashLeftKeyframes;
            default: return idleFacingRightKeyframes;
        }
    }

    private boolean isAnimationLooping(CharacterFSMState state) {
        return state == CharacterFSMState.IDLE || state == CharacterFSMState.RUNNING;
    }

    private void startAction(CharacterFSMState newState, long currentTime) {
        this.fsmState = newState;
        this.actionStartTime = currentTime;
        this.currentFrame = -1;

        if (isAttackState(newState)) {
            this.hitTargetsInCurrentAction.clear();
        }
    }

    public boolean hasHitTarget(UUID targetId) {
        return hitTargetsInCurrentAction.contains(targetId);
    }

    public void addHitTarget(UUID targetId) {
        hitTargetsInCurrentAction.add(targetId);
    }

    private boolean isAttackState(CharacterFSMState state) {
        switch (state) {
            case PUNCH_NORMAL:
            case PUNCH_HOOK:
            case PUNCH_HEAVY:
            case KICK_NORMAL:
            case KICK_AERIAL:
            case KICK_HIGH:
            case KICK_LOW:
                return true;
            default:
                return false;
        }
    }

    private void setToIdle() {
        this.fsmState = CharacterFSMState.IDLE;
        this.currentFrame = -1;
    }

    private boolean isActionFinished(long currentTime, long duration) {
        return currentTime - actionStartTime > duration;
    }

    public Pose getCurrentPose() { return this.currentPose; }

    public List<Shape> getHurtboxes() {
        List<Shape> hurtboxes = new ArrayList<>();
        if (this.currentPose == null) return hurtboxes;

        Rectangle torsoRect = new Rectangle(-5, (int) -this.skeleton.getTorsoLength(), 11, (int) this.skeleton.getTorsoLength());
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

    public Line2D.Double getActiveHitbox() {
        if (this.fsmState == CharacterFSMState.PUNCH_NORMAL && (currentFrame >= 2 && currentFrame <= 3)) {
            Point2D.Double startPoint, endPoint;
            if (isFacingRight) {
                startPoint = skeleton.elbowR;
                endPoint = skeleton.wristR;
            } else {
                startPoint = skeleton.elbowL;
                endPoint = skeleton.wristL;
            }

            return new Line2D.Double(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
        }
        if (this.fsmState == CharacterFSMState.KICK_NORMAL && (currentFrame >= 3 && currentFrame <= 4)) {
            Point2D.Double startPoint, endPoint;
            if (isFacingRight) {
                startPoint = skeleton.kneeR;
                endPoint = skeleton.footR;
            } else {
                startPoint = skeleton.kneeL;
                endPoint = skeleton.footL;
            }

            return new Line2D.Double(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
        }
        return null;
    }

    public double getHealth() { return health; }
    public double getStamina() { return stamina; }
}