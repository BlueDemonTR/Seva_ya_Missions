package poyoraz.seva_ya;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import poyoraz.seva_ya.models.AssignedMission;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;

import java.util.*;

public class StateSaverAndLoader extends PersistentState {

    public ArrayList<String> currentMissions = new ArrayList<>();
    public HashMap<UUID, PlayerData> players = new HashMap<>();
    public ArrayList<AssignedMission> assignedMissions = new ArrayList<>();
    public ArrayList<UUID> overseers = new ArrayList<>();

    private static final Type<StateSaverAndLoader> type = new Type<>(
            StateSaverAndLoader::new, // If there's no 'StateSaverAndLoader' yet create one
            StateSaverAndLoader::createFromNbt, // If there is a 'StateSaverAndLoader' NBT, parse it with 'createFromNbt'
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or 'World.NETHER'.  Any work)
        PersistentStateManager persistentStateManager = Objects
                .requireNonNull(
                        server
                                .getWorld(World.OVERWORLD)
                )
                .getPersistentStateManager();

        // The first time the following 'getOrCreate' function is called, it creates a brand new 'StateSaverAndLoader' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to 'getOrCreate' pass in the saved
        // 'StateSaverAndLoader' NBT on disk to our function 'StateSaverAndLoader::createFromNbt'.
        StateSaverAndLoader state = persistentStateManager.getOrCreate(type, Seva_ya_Missions.MOD_ID);

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        // Technically it's 'cleaner' if you only mark state as dirty when there was actually a change, but the vast majority
        // of mod writers are just going to be confused when their data isn't being saved, and so it's best just to 'markDirty' for them.
        // Besides, it's literally just setting a bool to true, and the only time there's a 'cost' is when the file is written to disk when
        // there were no actual change to any of the mods state (INCREDIBLY RARE).
        state.markDirty();

        return state;
    }
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbtCompound = new NbtCompound();
        putStringArray(nbtCompound, currentMissions);
        nbt.put("currentMissions", nbtCompound);

        NbtCompound assignedMissions = new NbtCompound();
        putAssignedMissionArray(assignedMissions, this.assignedMissions);
        nbt.put("assignedMissions", assignedMissions);

        NbtCompound overseers = new NbtCompound();
        putStringArray(overseers, this.overseers);
        nbt.put("overseers", overseers);

        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putBoolean("missionsPulled", playerData.missionsPulled);
            playerNbt.putString("tryingToComplete", playerData.tryingToComplete != null
                    ? playerData.tryingToComplete.id
                    : "");

            NbtCompound witnesses = new NbtCompound();
            putStringArray(witnesses, playerData.witnesses);
            playerNbt.put("witnesses", witnesses);

            NbtCompound boundMissions = new NbtCompound();
            putStringArray(
                    boundMissions,
                    playerData.boundMissions.stream().map(mission -> mission.id).toList()
            );
            playerNbt.put("boundMissions", boundMissions);

            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("playersNbt", playersNbt);
        return nbt;
    }

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup){
        StateSaverAndLoader state = new StateSaverAndLoader();

        NbtCompound missions = tag.getCompound("currentMissions");

        state.currentMissions = getStringArray(missions);

        state.assignedMissions = getAssignedMissionArray(tag.getCompound("assignedMissions"));

        state.overseers = new ArrayList<>();
        getStringArray(tag.getCompound("overseers"))
                .forEach(string -> state.overseers.add(UUID.fromString(string)));

        NbtCompound playersNbt = tag.getCompound("playersNbt");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();
            NbtCompound playerNbt = playersNbt.getCompound(key);

            playerData.missionsPulled = playerNbt.getBoolean("missionsPulled");
            UUID uuid = UUID.fromString(key);

            playerData.tryingToComplete = GlobalMissionHolder.getMissionById(playerNbt.getString("tryingToComplete"));

            playerData.witnesses = new ArrayList<UUID>(
                    getStringArray(playerNbt.getCompound("witnesses"))
                            .stream()
                            .map(UUID::fromString)
                            .toList()
            );

            playerData.boundMissions = new ArrayList<Mission>(
                    getStringArray(playerNbt.getCompound("boundMissions"))
                            .stream()
                            .map(GlobalMissionHolder::getMissionById)
                            .toList()
            );

            state.players.put(uuid, playerData);
        });

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(player.getWorld().getServer()));

        // Either get the player by the uuid, or we don't have data for them yet, make a new player state
        return serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    private static void putStringArray(NbtCompound nbtCompound, List<?> array) {
        for (int i = 0; i < array.size(); i++) {
            nbtCompound.putString(String.valueOf(i),  array.get(i).toString());
        }
    }

    private static ArrayList<String> getStringArray(NbtCompound nbtCompound) {
        ArrayList<String> curr = new ArrayList<>();
        nbtCompound.getKeys().forEach(key -> {
            curr.add(nbtCompound.getString(key));
        });
        return curr;
    }

    private static ArrayList<Mission> getMissionArray(NbtCompound nbtCompound) {
        ArrayList<Mission> curr = new ArrayList<>();

        nbtCompound.getKeys().forEach(key -> {
            NbtCompound missionData = nbtCompound.getCompound(key);

            curr.add(new Mission(
                    missionData.getString("id"),
                    missionData.getString("name"),
                    missionData.getString("description"),
                    MissionType.valueOf(missionData.getString("type")),
                    missionData.getInt("reward")
            ));
        });

        return curr;
    }

    private static void putMissionArray(NbtCompound nbtCompound, List<Mission> array) {
        for (int i = 0; i < array.size(); i++) {
            Mission mission = array.get(i);

            NbtCompound missionData = new NbtCompound();
            missionData.putString("name", mission.name);
            missionData.putString("id", mission.id);
            missionData.putString("description", mission.description);
            missionData.putInt("reward", mission.reward);
            missionData.putString("type", mission.type.name());

            nbtCompound.put(String.valueOf(i), missionData);
        }
    }

    private static ArrayList<AssignedMission> getAssignedMissionArray(NbtCompound nbtCompound) {
        ArrayList<AssignedMission> curr = new ArrayList<>();

        nbtCompound.getKeys().forEach(key -> {
            NbtCompound missionData = nbtCompound.getCompound(key);

            curr.add(new AssignedMission(
                    missionData.getString("id"),
                    missionData.getString("name"),
                    missionData.getString("description"),
                    missionData.getInt("reward"),
                    UUID.fromString(missionData.getString("assignee"))
            ));
        });

        return curr;
    }

    private static void putAssignedMissionArray(NbtCompound nbtCompound, List<AssignedMission> array) {
        for (int i = 0; i < array.size(); i++) {
            AssignedMission mission = array.get(i);

            NbtCompound missionData = new NbtCompound();
            missionData.putString("name", mission.name);
            missionData.putString("id", mission.id);
            missionData.putString("description", mission.description);
            missionData.putInt("reward", mission.reward);
            missionData.putString("assignee", mission.assignee.toString());

            nbtCompound.put(String.valueOf(i), missionData);
        }
    }
}
