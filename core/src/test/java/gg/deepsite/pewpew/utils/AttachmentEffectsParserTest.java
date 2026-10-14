package gg.deepsite.pewpew.utils;

import gg.deepsite.pewpew.api.objects.attachment.AttachmentEffects;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentEffectsParserTest {

	private static ConfigurationNode parse(String yaml) throws Exception {
		return YamlConfigurationLoader.builder()
				.source(() -> new BufferedReader(new StringReader(yaml)))
				.build()
				.load();
	}

	@Test
	void readsEveryEffect() throws Exception {
		ConfigurationNode node = parse("""
				victimEffects:
				  - "SLOWNESS:60:1"
				shooterEffects:
				  - "SPEED:40:0"
				fireTicks: 40
				freezeTicks: 60
				chance: 0.5
				cooldown: 40
				impactParticle: "SNOWFLAKE"
				explosive:
				  blastRadius: 3.0
				  explosionDamage: 5.0
				  cooldown: 100
				""");
		List<String> warnings = new ArrayList<>();
		AttachmentEffects effects = AttachmentEffectsParser.parse("test.yml", "chip", node, warnings::add);

		assertEquals(1, effects.victimEffects().size());
		assertEquals(60, effects.victimEffects().get(0).duration());
		assertEquals(1, effects.shooterEffects().size());
		assertEquals(40, effects.fireTicks());
		assertEquals(60, effects.freezeTicks());
		assertNotNull(effects.impactParticle());
		assertNotNull(effects.explosive());
		assertEquals(3.0, effects.explosive().blastRadius(), 1e-9);
		assertEquals(0.5, effects.chance(), 1e-9);
		assertEquals(40, effects.cooldown());
		assertEquals(100, effects.explosionCooldown());
		assertTrue(warnings.isEmpty(), warnings.toString());
	}

	@Test
	void warnsAboutUnknownKeysAndKeepsTheRest() throws Exception {
		ConfigurationNode node = parse("""
				fireTicks: 20
				bogus: 1
				""");
		List<String> warnings = new ArrayList<>();
		AttachmentEffects effects = AttachmentEffectsParser.parse("test.yml", "chip", node, warnings::add);

		assertEquals(1, warnings.size(), warnings.toString());
		assertEquals(20, effects.fireTicks());
	}

	@Test
	void invalidChanceFallsBackToAlways() throws Exception {
		List<String> warnings = new ArrayList<>();
		AttachmentEffects effects = AttachmentEffectsParser.parse("test.yml", "chip",
				parse("fireTicks: 20\nchance: 3\n"), warnings::add);

		assertEquals(1, warnings.size(), warnings.toString());
		assertEquals(1.0, effects.chance(), 1e-9);
	}

	@Test
	void missingBlockMeansNoEffects() throws Exception {
		AttachmentEffects effects = AttachmentEffectsParser.parse("test.yml", "chip",
				parse("name: x\n").node("effects"), w -> { });
		assertTrue(effects.isEmpty());
	}
}
