package poyoraz.seva_ya;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import static net.minecraft.server.command.CommandManager.*;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;

import java.util.ArrayList;

public class Seva_ya_Missions implements ModInitializer {
	public static final String MOD_ID = "seva_ya_missions";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ArrayList<Mission> missions = new ArrayList<>();

	public void parseMissions() {
		MissionsConfig.allTasks.forEach((jsonString) -> {
			try {
				JsonObject json = (JsonObject) JsonParser.parseString(jsonString);

				missions.add(
					new Mission(
							json.get("name").getAsString(),
							json.get("description").getAsString(),
							Mission.getTypeFromInt(json.get("difficulty").getAsInt()),
							json.get("reward").getAsInt()
					)
				);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, MissionsConfig.class);
		MissionCommands.initialize();
		parseMissions();

		LOGGER.info("Hello Fabric world!");
	}
}