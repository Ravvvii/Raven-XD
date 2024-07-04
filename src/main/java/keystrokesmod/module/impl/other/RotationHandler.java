package keystrokesmod.module.impl.other;

import keystrokesmod.event.MoveInputEvent;
import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.utils.ModeOnly;
import keystrokesmod.utility.AimSimulator;
import keystrokesmod.utility.MoveUtil;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class RotationHandler extends Module {
    private static @Nullable Float movementYaw = null;
    private static @Nullable Float rotationYaw = null;
    private static @Nullable Float rotationPitch = null;
    private boolean isSet = false;

    private final ModeSetting moveFix = new ModeSetting("Move fix", new String[]{"None", "Default", "Advanced"}, 0);
    private final ModeSetting smoothBack = new ModeSetting("Smooth back", new String[]{"None", "Default"}, 0);
    private final SliderSetting aimSpeed = new SliderSetting("Aim speed", 5, 0, 10, 0.1, new ModeOnly(smoothBack, 1));
    public static final ButtonSetting rotateBody = new ButtonSetting("Rotate body", true);
    public static final ButtonSetting fullBody = new ButtonSetting("Full body", false);
    public static final SliderSetting randomYawFactor = new SliderSetting("Random yaw factor", 1.0, 0.0, 10.0, 1.0);

    public RotationHandler() {
        super("RotationHandler", category.other);
        this.registerSetting(moveFix, smoothBack, aimSpeed);
        this.registerSetting(new DescriptionSetting("Classic"));
        this.registerSetting(rotateBody, fullBody, randomYawFactor);
        this.canBeEnabled = false;
    }

    public static float getMovementYaw() {
        if (movementYaw != null)
            return movementYaw;
        return mc.thePlayer.rotationYaw;
    }

    public static float getRotationYaw() {
        if (rotationYaw != null)
            return rotationYaw;
        return mc.thePlayer.rotationYaw;
    }

    public static float getRotationPitch() {
        if (rotationPitch != null)
            return rotationPitch;
        return mc.thePlayer.rotationPitch;
    }

    /**
     * Fix movement
     * @param event before update living entity (move)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(MoveInputEvent event) {
        if (isSet) {
            switch ((int) smoothBack.getInput()) {
                case 0:
                    rotationYaw = mc.thePlayer.rotationYaw;
                    rotationPitch = mc.thePlayer.rotationPitch;
                    break;
                case 1:
                    rotationYaw = AimSimulator.rotMove(mc.thePlayer.rotationYaw, getRotationYaw(), (float) aimSpeed.getInput());
                    rotationPitch = AimSimulator.rotMove(mc.thePlayer.rotationPitch, getRotationPitch(), (float) aimSpeed.getInput());
                    break;
            }
        }

        if (rotationYaw != null && rotationYaw == mc.thePlayer.rotationYaw) rotationYaw = null;
        if (rotationPitch != null && rotationPitch == mc.thePlayer.rotationPitch) rotationPitch = null;

        RotationEvent rotationEvent = new RotationEvent(getRotationYaw(), getRotationPitch());
        MinecraftForge.EVENT_BUS.post(rotationEvent);
        isSet = rotationEvent.isSet() || (!Objects.equals(rotationYaw, mc.thePlayer.rotationYaw) || !Objects.equals(rotationPitch, mc.thePlayer.rotationPitch));
        if (isSet) {
            rotationYaw = rotationEvent.getYaw();
            rotationPitch = rotationEvent.getPitch();
        }

        switch ((int) moveFix.getInput()) {
            case 0:
                movementYaw = null;
                break;
            case 1:
                movementYaw = null;

                final float forward = event.getForward();
                final float strafe = event.getStrafe();

                final double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MoveUtil.direction(mc.thePlayer.rotationYaw, forward, strafe)));

                if (forward == 0 && strafe == 0) {
                    return;
                }

                float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

                for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
                    for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                        if (predictedStrafe == 0 && predictedForward == 0) continue;

                        final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MoveUtil.direction(getRotationYaw(), predictedForward, predictedStrafe)));
                        final double difference = Math.abs(angle - predictedAngle);

                        if (difference < closestDifference) {
                            closestDifference = (float) difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }

                event.setForward(closestForward);
                event.setStrafe(closestStrafe);
                break;
            case 2:
                movementYaw = getRotationYaw();
                break;
        }
    }
}