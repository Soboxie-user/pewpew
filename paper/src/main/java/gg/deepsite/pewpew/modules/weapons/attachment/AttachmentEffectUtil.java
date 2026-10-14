package gg.deepsite.pewpew.modules.weapons.attachment;

import gg.deepsite.pewpew.api.events.PewpewGunExplodeEvent;
import gg.deepsite.pewpew.api.objects.ExplosiveConfig;
import gg.deepsite.pewpew.api.objects.PewpewGunItem;
import gg.deepsite.pewpew.api.objects.attachment.AttachmentEffects;
import gg.deepsite.pewpew.api.objects.attachment.PewpewAttachment;
import gg.deepsite.pewpew.modules.weapons.shooting.Ballistics;
import gg.deepsite.pewpew.modules.weapons.shooting.Explosions;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Applies the {@code effects} of the attachments fitted to a gun when one of its shots hits something. */
@UtilityClass
public class AttachmentEffectUtil {

	private static final int MAX_TRACKED = 2048;

	/** Tick at which each shooter/attachment/effect may trigger again. Expired entries are swept when it grows. */
	private static final Map<String, Integer> NEXT_ALLOWED = new ConcurrentHashMap<>();

	public static void onHit(@NotNull LivingEntity target, @Nullable Player shooter, @NotNull PewpewGunItem gun,
							 @NotNull List<PewpewAttachment> fitted) {
		for (PewpewAttachment attachment : fitted) {
			AttachmentEffects effects = attachment.getEffects();
			if (effects == null || effects.isEmpty()) continue;
			if (effects.chance() < 1.0 && ThreadLocalRandom.current().nextDouble() >= effects.chance()) continue;
			if (!ready(shooter, attachment, "effects", effects.cooldown())) continue;

			Ballistics.applyEffects(target, effects.victimEffects());
			if (shooter != null) Ballistics.applyEffects(shooter, effects.shooterEffects());
			if (effects.fireTicks() > 0) {
				target.setFireTicks(Math.max(target.getFireTicks(), effects.fireTicks()));
			}
			if (effects.freezeTicks() > 0) {
				target.setFreezeTicks(Math.max(target.getFreezeTicks(), effects.freezeTicks()));
			}

			Location at = target.getLocation().add(0, target.getHeight() / 2, 0);
			if (effects.impactParticle() != null) Ballistics.impact(effects.impactParticle(), at);
			if (effects.explosive() != null && ready(shooter, attachment, "explosion", effects.explosionCooldown())) {
				explode(shooter, gun, effects.explosive(), at);
			}
		}
	}

	/**
	 * Starts the cooldown and returns true when {@code what} may trigger for this shooter and attachment, false
	 * while it is still cooling down. A cooldown of 0 or less never blocks.
	 */
	private static boolean ready(@Nullable Player shooter, PewpewAttachment attachment, String what, int cooldownTicks) {
		if (cooldownTicks <= 0) return true;
		int now = Bukkit.getCurrentTick();
		if (NEXT_ALLOWED.size() > MAX_TRACKED) NEXT_ALLOWED.values().removeIf(next -> next <= now);
		String key = (shooter == null ? "-" : shooter.getUniqueId().toString()) + '|' + attachment.getId() + '|' + what;
		Integer next = NEXT_ALLOWED.get(key);
		if (next != null && now < next) return false;
		NEXT_ALLOWED.put(key, now + cooldownTicks);
		return true;
	}

	private static void explode(@Nullable Player detonator, PewpewGunItem gun, ExplosiveConfig config, Location at) {
		PewpewGunExplodeEvent event = new PewpewGunExplodeEvent(
				detonator, gun, at, config.blastRadius(), config.explosionDamage());
		if (!event.callEvent()) return;
		ExplosiveConfig applied = new ExplosiveConfig(
				event.getBlastRadius(), event.getExplosionDamage(), config.explosionKnockback(),
				config.damageBlocks(), config.rebuildEnabled(), config.rebuildDelay(), config.blocksPerTick());
		Explosions.detonate(at.getWorld(), at, applied, detonator, gun);
	}
}
