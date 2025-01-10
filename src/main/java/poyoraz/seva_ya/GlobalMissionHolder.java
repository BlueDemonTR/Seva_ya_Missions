package poyoraz.seva_ya;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;

import java.util.*;

public class GlobalMissionHolder {
    public static ArrayList<Mission> missions = new ArrayList<>();

    public static ArrayList<Mission> getMissions() {
        return missions;
    }

    public static void parseMissions() {
        MissionsConfig.allTasks.forEach((jsonString) -> {
            try {
                JsonObject json = (JsonObject) JsonParser.parseString(jsonString);

                missions.add(
                        new Mission(
                                json.get("id").getAsString(),
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

    public static ArrayList<Mission> getMissionsByDifficulty(MissionType type) {
        ArrayList<Mission> filtered = new ArrayList<>();

        missions.forEach((mission) -> {
            if(mission.type == type) {
                filtered.add(mission);
            }
        });

        return filtered;
    }

    public static ArrayList<Mission> getAvailableMissionsByDifficulty(MissionType type, MinecraftServer server) {
        try {
            ArrayList<Mission> ret = new ArrayList<Mission>(getMissionsByDifficulty(type).stream().filter(mission -> {
                switch (mission.type) {
                    case EASY, MEDIUM, HARD -> {
                        return true;
                    }
                    case ETERNAL -> {
                        return !isMissionBound(mission, server);
                    }
                    default -> {
                        return false;
                    }
                }
            }).toList());
            Seva_ya_Missions.LOGGER.info("pebis2");
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String getMissionsAsString(ArrayList<Mission> missions) {
        String str = "";

        for (Mission mission : missions) {
            str = str.concat(mission.toString());
        }

        return str;
    }

    public static boolean isMissionBound(Mission mission, MinecraftServer server) {
        StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
        for(Map.Entry<UUID, PlayerData> set : state.players.entrySet()) {
            if(set.getValue().boundMissions.contains(mission)) {
                return true;
            }
        }

        return false;
    }

    public static Mission getMissionById(String id) {
        for (Mission mission : missions) {
            if (mission.id.equals(id)) return mission;
        }
        return null;
    }

    public static Mission getMissionByName(String name) {
        for (Mission mission : missions) {
            if (mission.name.equals(name)) return mission;
        }
        return null;
    }
}
