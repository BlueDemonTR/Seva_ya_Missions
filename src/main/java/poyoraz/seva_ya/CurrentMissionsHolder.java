package poyoraz.seva_ya;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;

import java.util.ArrayList;

public class CurrentMissionsHolder {
    private static ArrayList<Mission> missions = new ArrayList<>();
    private static boolean missionsCached = false;

    public static ArrayList<Mission> getWeeklyMissions(MinecraftServer server) {
        if(missionsCached) return missions;

        ArrayList<String> ids = StateSaverAndLoader.getServerState(server).currentMissions;

        ArrayList<Mission> _missions = new ArrayList<>();

        ids.forEach(( id ) -> {
            Mission mission = GlobalMissionHolder.getMissionById(id);
            if(mission != null) _missions.add(mission);
        });

        missions = _missions;
        missionsCached = true;

        return _missions;
    }

    public static void rerollMissions(MinecraftServer server) {
        StateSaverAndLoader stateSaver = StateSaverAndLoader.getServerState(server);

        stateSaver.players.forEach((uuid, playerData) -> {
            playerData.missionsPulled = false;
        });

        stateSaver.currentMissions.clear();

        missionsCached = false;
        addWeeklyMissionsByDifficulty(MissionType.EASY, MissionsConfig.easyTaskCount, server);
        addWeeklyMissionsByDifficulty(MissionType.MEDIUM, MissionsConfig.mediumTaskCount, server);
        addWeeklyMissionsByDifficulty(MissionType.HARD, MissionsConfig.hardTaskCount, server);
    }

    private static void addWeeklyMissionsByDifficulty(MissionType type, int count, MinecraftServer server) {
        ArrayList<Mission> filtered = GlobalMissionHolder.getMissionsByDifficulty(type);
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

    public static void checkMissionCompletion(LivingEntity player) {

        if(checkForCompletion(player) == 1) {
            finishMission(player);
        }
    }

    public static void finishMission(LivingEntity player) {
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

        Mission mission = playerData.tryingToComplete;

        playerData.tryingToComplete = null;
        playerData.witnesses.clear();

        ItemStack reward = new ItemStack(
                Registries.ITEM.get(Identifier.of(MissionsConfig.reward)),
                mission.reward
        );

        PlayerEntity playerEntity = (PlayerEntity) player;
        playerEntity.sendMessage(Text.of(
                        "You have completed the mission successfully! You get "
                                + mission.reward
                                + " "
                                + reward.getName().getString()
                                + (mission.reward != 1 ? "s" : "")
                ),
                false
        );

        player.giveOrDropStack(reward);
    }

    public static int checkForCompletion(LivingEntity player) {
        MinecraftServer server = player.getServer();

        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        int witnessCount = 0;
        Mission mission = playerData.tryingToComplete;

        if(mission == null) return 0;

        Seva_ya_Missions.LOGGER.info(mission.type.toString());
        switch (mission.type) {
            case EASY -> {
                return 1;
            }
            case MEDIUM -> {
                witnessCount = 1;
            }
            case HARD -> {
                witnessCount = 2;
            }
            default -> {
                return 0;
            }
        }

        if (witnessCount > playerData.witnesses.size()) {
            assert server != null;
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "/say @a "
                            + player.getName().getString()
                            + " is trying to complete the mission: "
                            + mission.name
                            + " "
                            + String.valueOf(playerData.witnesses.size())
                            + "/"
                            + String.valueOf(witnessCount)
            );

            return 0;
        }

        return 1;
    }

    public static Mission getMissionByName(String name) {
        for (Mission mission : missions) {
            if (mission.name.equals(name)) return mission;
        }
        return null;
    }

    public static Mission getMissionById(String id) {
        for (Mission mission : missions) {
            if (mission.id.equals(id)) return mission;
        }
        return null;
    }

}