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

    private static ArrayList<Mission> weeklyMissions = new ArrayList<>();
    private static boolean missionsCached = false;

    public static ArrayList<Mission> getWeeklyMissions(MinecraftServer server) {
        if(missionsCached) return weeklyMissions;

        ArrayList<String> ids = StateSaverAndLoader.getServerState(server).currentMissions;

        ArrayList<Mission> _missions = new ArrayList<>();

        ids.forEach(( id ) -> {
            Mission mission = getMissionById(id);
            if(mission != null) _missions.add(mission);
        });

        weeklyMissions = _missions;
        missionsCached = true;

        return _missions;
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

    public static int rerollMissions(CommandContext<ServerCommandSource> commandContext) {
        MinecraftServer server = commandContext.getSource().getServer();

        Seva_ya_Missions.LOGGER.info("Bepis1");

        try {
            StateSaverAndLoader stateSaver = StateSaverAndLoader.getServerState(server);
            stateSaver.currentMissions.clear();
            missionsCached = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Seva_ya_Missions.LOGGER.info("Bepis2");

        addWeeklyMissionsByDifficulty(MissionType.EASY, MissionsConfig.easyTaskCount, server);

        Seva_ya_Missions.LOGGER.info("Bepis3");
        addWeeklyMissionsByDifficulty(MissionType.MEDIUM, MissionsConfig.mediumTaskCount, server);


        Seva_ya_Missions.LOGGER.info("Bepis4");
        addWeeklyMissionsByDifficulty(MissionType.HARD, MissionsConfig.hardTaskCount, server);


        Seva_ya_Missions.LOGGER.info("Bepis5");
        return 1;
    }

    private static void addWeeklyMissionsByDifficulty(MissionType type, int count, MinecraftServer server) {
        ArrayList<Mission> filtered = getMissionsByDifficulty(type);
        ArrayList<Mission> toAdd = new ArrayList<>();

        if(filtered.size() <= count) {
            toAdd = filtered;
        } else {
            while(count > 0) {
                int random = (int) (Math.random() * filtered.size());
                Mission mission = filtered.get(random);

                if(toAdd.contains(mission)) {
                    continue;
                } else {
                    toAdd.add(mission);
                    count--;
                }
            }
        }

        toAdd.forEach((mission) -> {
            addMissionToWeekly(mission, server);
        });
    }

    private static void addMissionToWeekly(Mission mission, MinecraftServer server) {
        StateSaverAndLoader stateSaver = StateSaverAndLoader.getServerState(server);

        stateSaver.currentMissions.add(mission.id);

        missionsCached = false;
    }

    public static Mission getMissionById(String id) {
        for (Mission mission : missions) {
            if (mission.id.equals(id)) return mission;
        }
        return null;
    }


}
