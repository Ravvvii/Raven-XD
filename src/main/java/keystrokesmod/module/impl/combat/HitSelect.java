package keystrokesmod.module.impl.combat;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.MoveUtil;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import static keystrokesmod.module.ModuleManager.hitSelect;

public class HitSelect extends Module {
    private static final String[] PREFERENCES = new String[]{"Move Speed", "KB Reduction", "Critical Hit"};
    private final ModeSetting preference;
    private final SliderSetting delay;

    private static long attackTime = -1;
    private static boolean currentShouldAttack = false;

    public HitSelect() {
        super("HitSelect", category.combat, "Chooses the best time to hit.");
        this.registerSetting(preference = new ModeSetting("Preference", PREFERENCES, 0,
                "Move Speed: Keep sprint but legitimate\n" +
                        "KB Reduction: KnockBack reduction\n" +
                        "Critical Hit: Critical hit frequency"));
        this.registerSetting(delay = new SliderSetting("Delay", 300, 0, 300, 1));
    }

    @Override
    public String getInfo() {
        return "Pause";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(@NotNull AttackEntityEvent event) {
        if (!currentShouldAttack) {
            event.setCanceled(true);
            return;
        }

        if (canAttack())
            attackTime = System.currentTimeMillis();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreUpdate(PreUpdateEvent event) {
        currentShouldAttack = true;

        switch ((int) preference.getInput()) {
            case 0:
                currentShouldAttack = mc.thePlayer.hurtTime > 0 && !mc.thePlayer.onGround && MoveUtil.isMoving();
                break;
            case 1:
                currentShouldAttack = !mc.thePlayer.onGround && mc.thePlayer.motionY < 0;
                break;
            case 2:
                currentShouldAttack = !mc.thePlayer.onGround && mc.thePlayer.fallDistance > 0.3;
                break;
        }

        if (!currentShouldAttack)
            currentShouldAttack = System.currentTimeMillis() - HitSelect.attackTime >= hitSelect.delay.getInput();
    }

    public static boolean canAttack() {
        return canSwing();
    }

    public static boolean canSwing() {
        return hitSelect.isEnabled() && currentShouldAttack;
    }
}
