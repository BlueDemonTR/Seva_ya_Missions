package poyoraz.seva_ya;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;

import java.util.*;

public class GlobalMissionHolder {
    private static final ArrayList<Mission> configMissions = new ArrayList<>();

    public static ArrayList<Mission> getMissions(MinecraftServer server) {
        ArrayList<Mission> allMissions = new ArrayList<>(configMissions);

        allMissions.addAll(
                StateSaverAndLoader.getServerState(server).assignedMissions
        );

        return allMissions;
    }

    public static void parseMissions() {
        MissionsConfig.allTasks.forEach((jsonString) -> {
            if(jsonString.isEmpty()) return;

            try {
                JsonObject json = (JsonObject) JsonParser.parseString(jsonString);

                configMissions.add(
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

    public static ArrayList<Mission> getMissionsByDifficulty(MissionType type, MinecraftServer server) {
        if(type == MissionType.ASSIGNED) {
            return new ArrayList<Mission>(StateSaverAndLoader.getServerState(server).assignedMissions);
        }

        ArrayList<Mission> filtered = new ArrayList<>();

        configMissions.forEach((mission) -> {
            if(mission.type == type) {
                filtered.add(mission);
            }
        });

        return filtered;
    }

    public static ArrayList<Mission> getAvailableMissionsByDifficulty(MissionType type, MinecraftServer server) {
        try {
            return new ArrayList<Mission>(getMissionsByDifficulty(type, server).stream().filter(mission -> {
                switch (mission.type) {
                    case EASY, MEDIUM, HARD, ASSIGNED -> {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static Text getMissionsAsText(ArrayList<Mission> missions) {
        MutableText text = Text.literal("");

        for (int i = 0; i < missions.size(); i++) {
            Mission mission = missions.get(i);
            text.append(mission.toText());

            if(i != missions.size() - 1) {
                text.append("\n");
            }

        }

        return text;
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

    public static Mission getMissionById(String id, MinecraftServer server) {
        for (Mission mission : getMissions(server)) {
            if (mission.id.equals(id)) return mission;
        }
        return null;
    }

    public static Mission getMissionById(String id) {
        for (Mission mission : configMissions) {
            if (mission.id.equals(id)) return mission;
        }
        return null;
    }

    public static Mission getMissionByName(String name, MinecraftServer server) {
        for (Mission mission : getMissions(server)) {
            if (mission.name.equals(name)) return mission;
        }
        return null;
    }
}
