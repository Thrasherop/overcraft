package dev.overcraft;

import dev.overcraft.hero.Blow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What dealt a hero's damage, as the kill feed carries it (#84, #102). */
class BlowTest {
	@Test
	void feedNamesTheKind() {
		assertEquals("weapon", Blow.weapon(true).feed());
		assertTrue(Blow.weapon(true).crit());
		assertFalse(Blow.WEAPON.crit());
		assertEquals("melee", Blow.MELEE.feed());
		assertEquals("ability:soundwave", Blow.ability("soundwave").feed());
		assertEquals("perk:superbloom", Blow.perk("superbloom").feed());
		assertEquals("ult:catnapper", Blow.ult("catnapper").feed());
		assertTrue(Blow.weapon("charged_shot", false).crit(true).crit());
		assertEquals("ult:overclock", Blow.ult("overclock").crit(true).feed());
	}

	/**
	 * Every icon a hero's damage names in the code is a slug the HUD's art uses ("pulsar_torpedoes") and fits the kill
	 * feed payload's 32 characters (KillFeedPayload writes it with writeUtf(.., 32): longer would fail to send).
	 */
	@Test
	void everyNamedIconFitsThePayload() throws IOException {
		Pattern call = Pattern.compile("Blow\\.(ability|perk|ult|weapon|melee)\\(\"([^\"]*)\"");
		List<String> found = new ArrayList<>();
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				Matcher m = call.matcher(Files.readString(file));
				while (m.find()) {
					String feed = m.group(1) + ":" + m.group(2);
					found.add(feed);
					assertTrue(m.group(2).matches("[a-z_]+"), feed + " in " + file);
					assertTrue(feed.length() <= 32, feed + " is longer than the payload's 32");
				}
			}
		}
		// the heroes' ability, perk and ult kills (the wiki's elimination icons), each named somewhere
		for (String expected : List.of("ability:soundwave", "ability:purr", "ult:catnapper", "perk:claws_out", "perk:superbloom",
			"ability:biotic_grenade", "ability:sleep_dart", "perk:local_anesthetic", "ability:shield_bash", "ability:whip_shot",
			"perk:whiplash", "ability:pulsar_torpedoes", "weapon:charged_shot", "ult:overclock", "ult:rally", "perk:biotic_bounce", "melee:claws_out", "ability:disruptor_shot",
			"ability:cyberspace", "ult:emp")) {
			assertTrue(found.contains(expected), expected + " is never dealt");
		}
	}
}
