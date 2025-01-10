package poyoraz.seva_ya;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poyoraz.seva_ya.config.MissionsConfig;

public class Seva_ya_Missions implements ModInitializer {
	public static final String MOD_ID = "seva_ya_missions";

	public static Seva_ya_Missions StaticInstance;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, MissionsConfig.class);
		MissionCommands.initialize();
		GlobalMissionHolder.parseMissions();

		StaticInstance = this;
	}
}