package poyoraz.seva_ya;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;

import java.util.ArrayList;
import java.util.Arrays;

public class MissionHolder {
    public static ArrayList<Mission> missions = new ArrayList<>();

    public static ArrayList<Mission> weeklyMissions = new ArrayList<>();

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

    public static int rerollMissions(CommandContext<ServerCommandSource> commandContext) {
        weeklyMissions.clear();

        addWeeklyMissionsByDifficulty(MissionType.EASY, MissionsConfig.easyTaskCount);
        addWeeklyMissionsByDifficulty(MissionType.MEDIUM, MissionsConfig.mediumTaskCount);
        addWeeklyMissionsByDifficulty(MissionType.HARD, MissionsConfig.hardTaskCount);

        serializeMissions(commandContext.getSource().getServer());

        return 1;
    }

    private static void addWeeklyMissionsByDifficulty(MissionType type, int count) {
        ArrayList<Mission> filtered = getMissionsByDifficulty(type);
        if(filtered.size() <= count) {
            weeklyMissions.addAll(filtered);
        }
        while(count > 0) {
            int random = (int) (Math.random() * filtered.size());
            Mission mission = filtered.get(random);

            if(weeklyMissions.contains(mission)) {
                continue;
            } else {
                weeklyMissions.add(mission);
                count--;
            }
        }
    }
    public static void serializeMissions(MinecraftServer server) {
        ArrayList<String> currentMissions = new ArrayList<>();

        weeklyMissions.forEach((mission) -> {
            currentMissions.add(mission.id);
        });

        StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(server);

        serverState.serializedMissions = String.join("/", currentMissions);
    }

    public static void deserializeCurrentMissions(String idList) {
        weeklyMissions.clear();

        Seva_ya_Missions.LOGGER.info("bepis: " + idList);

        ArrayList<String> ids = new ArrayList<>(Arrays.asList(idList.split("/")));

        ids.forEach(( id ) -> {
            Mission mission = getMissionById(id);
            Seva_ya_Missions.LOGGER.info("bepis: " + id);
            if(mission != null) weeklyMissions.add(mission);
        });
    }

    public static Mission getMissionById(String id) {
        for (Mission mission : missions) {
            if (mission.id.equals(id)) return mission;
        }
        return null;
    }


}
