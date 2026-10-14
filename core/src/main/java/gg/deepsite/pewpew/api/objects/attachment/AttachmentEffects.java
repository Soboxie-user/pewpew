package gg.deepsite.pewpew.api.objects.attachment;

import gg.deepsite.pewpew.api.objects.ExplosiveConfig;
import gg.deepsite.pewpew.api.objects.PewpewEffect;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What an attachment does to a target when a shot with it hits, from the {@code effects} block. The keys
 * mirror the ones a gun uses for the same things ({@code victimEffects}, {@code shooterEffects},
 * {@code explosive}).
 *
 * <p>{@code chance} (0 to 1) is rolled on each hit, and {@code cooldown} (ticks, per shooter) limits how often
 * the whole block can trigger. {@code explosionCooldown} does the same for the explosion alone.
 */
public record AttachmentEffects(
		List<PewpewEffect> victimEffects,
		List<PewpewEffect> shooterEffects,
		int fireTicks,
		int freezeTicks,
		@Nullable Key impactParticle,
		@Nullable ExplosiveConfig explosive,
		double chance,
		int cooldown,
		int explosionCooldown) {

	public static final AttachmentEffects NONE = new AttachmentEffects(List.of(), List.of(), 0, 0, null, null, 1.0, 0, 0);

	public AttachmentEffects {
		victimEffects = List.copyOf(victimEffects);
		shooterEffects = List.copyOf(shooterEffects);
	}

	public boolean isEmpty() {
		return victimEffects.isEmpty() && shooterEffects.isEmpty() && fireTicks <= 0 && freezeTicks <= 0
				&& impactParticle == null && explosive == null;
	}
}
