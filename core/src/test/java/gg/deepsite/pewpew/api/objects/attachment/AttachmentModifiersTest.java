package gg.deepsite.pewpew.api.objects.attachment;

import gg.deepsite.pewpew.api.enums.ModifierType;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentModifiersTest {

	private static ConfigurationNode parse(String yaml) throws Exception {
		return YamlConfigurationLoader.builder()
				.source(() -> new BufferedReader(new StringReader(yaml)))
				.build()
				.load();
	}

	@Test
	void readsMultipliersAdditionsAndLongForms() throws Exception {
		ConfigurationNode node = parse("""
				damage: 0.9
				range_multiplier: 1.1
				recoil_bonus: -0.1
				max_ammo: 5
				fire-rate: 1.25
				""");
		List<String> warnings = new ArrayList<>();
		AttachmentModifiers modifiers = AttachmentModifiers.parse(node, warnings::add);

		assertEquals(0.9, modifiers.multiplier(ModifierType.DAMAGE), 1e-9);
		assertEquals(1.1, modifiers.multiplier(ModifierType.RANGE), 1e-9);
		assertEquals(-0.1, modifiers.addition(ModifierType.RECOIL), 1e-9);
		assertEquals(1.25, modifiers.multiplier(ModifierType.FIRE_RATE), 1e-9);
		assertTrue(warnings.isEmpty(), warnings.toString());
	}

	@Test
	void maxAmmoIsAdditiveWhenWrittenBare() throws Exception {
		AttachmentModifiers modifiers = AttachmentModifiers.parse(parse("max_ammo: 5\n"), w -> { });
		assertEquals(5.0, modifiers.addition(ModifierType.MAX_AMMO), 1e-9);
		assertEquals(1.0, modifiers.multiplier(ModifierType.MAX_AMMO), 1e-9);
	}

	@Test
	void skipsUnknownAndInvalidEntriesWithAWarningEach() throws Exception {
		ConfigurationNode node = parse("""
				bogus: 2
				damage: abc
				recoil: -1
				""");
		List<String> warnings = new ArrayList<>();
		AttachmentModifiers modifiers = AttachmentModifiers.parse(node, warnings::add);

		assertEquals(3, warnings.size(), warnings.toString());
		assertTrue(modifiers.isEmpty());
	}

	@Test
	void missingBlockMeansNoModifiers() throws Exception {
		AttachmentModifiers modifiers = AttachmentModifiers.parse(parse("name: x\n").node("modifiers"), w -> { });
		assertTrue(modifiers.isEmpty());
	}
}
