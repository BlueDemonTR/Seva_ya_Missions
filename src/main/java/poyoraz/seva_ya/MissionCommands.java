package poyoraz.seva_ya;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import poyoraz.seva_ya.models.AssignedMission;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;
import poyoraz.seva_ya.suggesters.CurrentMissionSuggester;
import poyoraz.seva_ya.suggesters.EternalMissionSuggester;
import poyoraz.seva_ya.suggesters.GlobalMissionSuggester;
import poyoraz.seva_ya.suggesters.MissionTypeSuggester;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;

public class MissionCommands {
    public static int base(CommandContext<ServerCommandSource> commandContext) {
        return 1;
    }

    public static void feedback(String string, ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal(string), false);
    }

    public static void feedback(Text text, ServerCommandSource source) {
        source.sendFeedback(() -> text, false);
    }

    public static int getAllMissions(CommandContext<ServerCommandSource> commandContext) {
        MissionType type = null;

        try {
            String typeString = StringArgumentType.getString(commandContext, "mission_type");

            if (!typeString.isEmpty()) {
                type = MissionType.valueOf(typeString);
            }
        } catch (Exception ignored) {
        }

        ArrayList<Mission> missions = type == null
                ? GlobalMissionHolder.getMissions(commandContext.getSource().getServer())
                : GlobalMissionHolder.getMissionsByDifficulty(type, commandContext.getSource().getServer());

        MissionMenu.create(
                missions,
                text -> {
                    feedback(text, commandContext.getSource());
                    return null;
                },
                missionType -> {
                    return new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/missions-admin showAll " + missionType.name()
                    );
                },
                mission -> {
                    return new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/missions finish \"" + mission.name + "\""
                    );
                }
        );

        return 1;
    }

    public static int getCurrentMissions(CommandContext<ServerCommandSource> commandContext) {
        MissionType type = null;

        try {
            String typeString = StringArgumentType.getString(commandContext, "mission_type");

            if(!typeString.isEmpty()) {
                type = MissionType.valueOf(typeString);
            }
        } catch (Exception ignored) {
        }

        ArrayList<Mission> missions = type == null
                ? CurrentMissionsHolder.getMissions(commandContext.getSource().getServer())
                : CurrentMissionsHolder.getMissionsByDifficulty(type, commandContext.getSource().getServer());

        MissionMenu.create(
                missions,
                text -> {
                    feedback(text, commandContext.getSource());
                    return null;
                },
                missionType -> {
                    return new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/missions get " + missionType.name()
                    );
                },
                mission -> {
                    return new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/missions finish \"" + mission.name + "\""
                    );
                }
        );

        return 1;
    }

    public static int getBoundMissions(CommandContext<ServerCommandSource> commandContext) {
        PlayerData playerData = StateSaverAndLoader.getPlayerState(
                Objects.requireNonNull(
                                commandContext
                                        .getSource()
                                        .getPlayer()
                        )
                );

        feedback(
                GlobalMissionHolder.getMissionsAsText(
                        playerData.boundMissions
                ),
                commandContext.getSource()
        );

        return 1;
    }

    public static int grantMissions(CommandContext<ServerCommandSource> commandContext) {
        try {
            LivingEntity player = EntityArgumentType.getPlayer(commandContext, "player");

            PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

            playerData.missionsPulled = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    public static int rerollMissions(CommandContext<ServerCommandSource> commandContext) {
        MinecraftServer server = commandContext.getSource().getServer();

        CurrentMissionsHolder.rerollMissions(server);
        feedback("Missions Rerolled Successfully", commandContext.getSource());
        return 1;
    }

    public static int getWeeklyMissions(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity player = commandContext.getSource().getPlayer();

        assert player != null;
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

        if(!playerData.missionsPulled) {
            commandContext.getSource().sendFeedback(() ->
                            Text.literal(
                                    "You haven't gotten the missions for this week yet, interact with a mission log block."
                            ),
                    false
            );

            return 0;
        }

        getCurrentMissions(commandContext);
        return 1;
    }

    public static int attemptCompleteMission(CommandContext<ServerCommandSource> commandContext) {
        String name = "";
        Mission mission = null;

        try {
            name = StringArgumentType.getString(commandContext, "mission_name");
            mission = CurrentMissionsHolder.getMissionByName(name, commandContext.getSource().getServer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(mission == null) {
            feedback(
                    "This mission doesn't exist",
                    commandContext.getSource()
            );
        }

        LivingEntity player = commandContext.getSource().getPlayer();

        assert player != null;
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

        playerData.tryingToComplete = mission;
        playerData.witnesses.clear();

        CurrentMissionsHolder.checkMissionCompletion(player);
        return 1;
    }

    public static int witnessPlayer(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity missionOwner;

        try {
            missionOwner = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LivingEntity witnessPlayer = commandContext.getSource().getPlayer();

        if(Objects.equals(witnessPlayer, missionOwner)) {
            feedback(
                    "You can't witness your own mission",
                    commandContext.getSource()
            );
            return 0;
        }

        PlayerData playerData = StateSaverAndLoader.getPlayerState(missionOwner);
        UUID uuid = Objects.requireNonNull(witnessPlayer).getUuid();

        if(playerData.tryingToComplete == null) {
            feedback(
                    "This player isn't trying to complete a mission",
                    commandContext.getSource()
            );
            return 0;
        }

        if(playerData.witnesses.contains(uuid)) {
            feedback(
                    "You have already witnessed this mission",
                    commandContext.getSource()
            );
            return 0;
        }

        playerData.witnesses.add(uuid);
        CurrentMissionsHolder.checkMissionCompletion(missionOwner);

        return 1;
    }

    public static int bindEternalMission(CommandContext<ServerCommandSource> commandContext) {
        PlayerEntity missionOwner;

        try {
            missionOwner = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PlayerData playerData = StateSaverAndLoader.getPlayerState(missionOwner);

        if(playerData.tryingToComplete == null) {
            feedback(
                    "This player isn't trying to complete a mission",
                    commandContext.getSource()
            );
            return 0;
        }

        if(playerData.tryingToComplete.type != MissionType.ETERNAL) {
            feedback(
                    "Mission isn't eternal",
                    commandContext.getSource()
            );
            return 0;
        }

        if(GlobalMissionHolder.isMissionBound(playerData.tryingToComplete, commandContext.getSource().getServer())) {
            feedback(
                    "Mission is already bound",
                    commandContext.getSource()
            );
        }

        feedback(
                "Bound player to eternal mission: " + playerData.tryingToComplete.name,
                commandContext.getSource()
        );
        missionOwner.sendMessage(
                Text
                        .literal("You have been bound to the eternal mission ")
                        .append(playerData.tryingToComplete.name)
                        .append(". Run the complete command again to get your reward."),
                false
        );

        playerData.boundMissions.add(playerData.tryingToComplete);

        return 1;
    }

    public static int unbindEternalMission(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity missionOwner;
        Mission mission;

        try {
            missionOwner = EntityArgumentType.getPlayer(commandContext, "player");
            String name = StringArgumentType.getString(commandContext, "mission_name");
            mission = GlobalMissionHolder.getMissionByName(name, commandContext.getSource().getServer());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PlayerData playerData = StateSaverAndLoader.getPlayerState(missionOwner);

        if(mission == null) {
            feedback(
                    "No such mission exists",
                    commandContext.getSource()
            );
        }

        if(!playerData.boundMissions.contains(mission)) {
            feedback(
                    "Mission is not bound to this player",
                    commandContext.getSource()
            );
        }

        assert mission != null;
        feedback(
                "Unbound player from eternal mission: " + mission.name,
                commandContext.getSource()
        );

        playerData.boundMissions.remove(mission);

        return 1;
    }

    private static int createAssignedMission(CommandContext<ServerCommandSource> commandContext) {
        PlayerEntity assignee;
        String name;
        String description;
        int reward;

        try {
            name = StringArgumentType.getString(commandContext, "name");
            description = StringArgumentType.getString(commandContext, "description");
            reward = IntegerArgumentType.getInteger(commandContext, "reward");
            assignee = EntityArgumentType.getPlayer(commandContext, "assignee");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        AssignedMission assignedMission = new AssignedMission(
                UUID.randomUUID().toString(),
                name,
                description,
                reward,
                assignee.getUuid()
        );

        MinecraftServer server = commandContext.getSource().getServer();

        StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);

        state.assignedMissions.add(assignedMission);
        CurrentMissionsHolder.missionsCached = false;

        return 1;
    }

    private static int rewardAssignedMission(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity assigned;

        try {
            assigned = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        PlayerData playerData = StateSaverAndLoader.getPlayerState(assigned);
        Mission mission = playerData.tryingToComplete;
        PlayerEntity assignee = commandContext.getSource().getPlayer();

        if(mission == null) {
            feedback(
                    "This player isn't trying to complete a mission",
                    commandContext.getSource()
            );
            return 0;
        }

        if(mission.type != MissionType.ASSIGNED) {
            feedback(
                    "Mission isn't an assigned mission",
                    commandContext.getSource()
            );
            return 0;
        }

        assert assignee != null;
        if(!mission.assignee.equals(assignee.getUuid())) {
            feedback(
                    "This isn't your assigned mission",
                    commandContext.getSource()
            );
            return 0;
        }

        CurrentMissionsHolder.finishMission(assigned);

        StateSaverAndLoader
                .getServerState(
                        commandContext.getSource().getServer()
                )
                .assignedMissions
                .removeIf(assignedMission -> {
                    return assignedMission.id.equals(mission.id);
                });

        return 1;
    }

    private static boolean isOverseer(ServerCommandSource serverCommandSource) {
        StateSaverAndLoader stateSaverAndLoader = StateSaverAndLoader.getServerState(serverCommandSource.getServer());

        return stateSaverAndLoader.overseers.contains(Objects.requireNonNull(serverCommandSource.getPlayer()).getUuid());
    }

    private static int assignOverseer(CommandContext<ServerCommandSource> commandContext) {
        PlayerEntity player;

        try {
            player = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(player == null) {
            feedback("No such player exists", commandContext.getSource());

            return 0;
        }

        StateSaverAndLoader stateSaverAndLoader = StateSaverAndLoader.getServerState(commandContext.getSource().getServer());

        if(stateSaverAndLoader.overseers.contains(player.getUuid())) {
            feedback("Player is already an overseer", commandContext.getSource());

            return 0;
        }

        stateSaverAndLoader.overseers.add(player.getUuid());
        feedback("Player is now an overseer, they might need to relog for autocomplete to work properly", commandContext.getSource());
        player.sendMessage(
                Text.literal("You are now an overseer, relog for autocomplete to work properly"),
                false
        );

        return 1;
    }

    private static int unassignOverseer(CommandContext<ServerCommandSource> commandContext) {
        PlayerEntity player;

        try {
            player = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(player == null) {
            feedback("No such player exists", commandContext.getSource());

            return 0;
        }

        StateSaverAndLoader stateSaverAndLoader = StateSaverAndLoader.getServerState(commandContext.getSource().getServer());

        if(!stateSaverAndLoader.overseers.contains(player.getUuid())) {
            feedback("Player isn't an overseer", commandContext.getSource());

            return 0;
        }

        stateSaverAndLoader.overseers.remove(player.getUuid());
        feedback("Player is no longer an overseer", commandContext.getSource());
        player.sendMessage(
                Text.literal("You are no longer an overseer, relog for autocomplete to work properly"),
                false
        );
        return 1;
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions-admin")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(MissionCommands::base)
                                        .then(literal("show")
                                                .executes(MissionCommands::getCurrentMissions)
                                                .then(argument("mission_type", StringArgumentType.string())
                                                        .suggests(new MissionTypeSuggester())
                                                        .executes(MissionCommands::getCurrentMissions)
                                                )
                                        )
                                        .then(literal("showAll")
                                                .executes(MissionCommands::getAllMissions)
                                                .then(argument("mission_type", StringArgumentType.string())
                                                        .suggests(new MissionTypeSuggester())
                                                        .executes(MissionCommands::getCurrentMissions)
                                                )
                                        )
                                        .then(literal("reroll")
                                                .executes(MissionCommands::rerollMissions)
                                        )
                                        .then(literal("grant")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::grantMissions)
                                                )
                                        )
                                        .then(literal("bind")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::bindEternalMission)
                                                )
                                        )
                                        .then(literal("unbind")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .then(argument("mission_name", StringArgumentType.string())
                                                                .suggests(new EternalMissionSuggester())
                                                                .executes(MissionCommands::unbindEternalMission)
                                                        )
                                                )
                                        )
                                        .then(literal("create-assigned")
                                                .then(argument("name", StringArgumentType.string())
                                                        .then(argument("description", StringArgumentType.string())
                                                                .then(argument("reward", IntegerArgumentType.integer(0))
                                                                        .then(argument("assignee", EntityArgumentType.player())
                                                                                .executes(MissionCommands::createAssignedMission)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(literal("assign-overseer")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::assignOverseer)))
                                        .then(literal("unassign-overseer")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::unassignOverseer)))
                        )
        );

        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions-overseer")
                                        .requires(MissionCommands::isOverseer)
                                        .executes(MissionCommands::base)
                                        .then(literal("bind")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::bindEternalMission)
                                                )
                                        )
                                        .then(literal("unbind")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .then(argument("mission_name", StringArgumentType.string())
                                                                .suggests(new EternalMissionSuggester())
                                                                .executes(MissionCommands::unbindEternalMission)
                                                        )
                                                )
                                        )
                                        .then(literal("create-assigned")
                                                .then(argument("name", StringArgumentType.string())
                                                        .then(argument("description", StringArgumentType.string())
                                                                .then(argument("reward", IntegerArgumentType.integer(0))
                                                                        .then(argument("assignee", EntityArgumentType.player())
                                                                                .executes(MissionCommands::createAssignedMission)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                        )
        );

        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions")
                                        .executes(MissionCommands::getWeeklyMissions)
                                        .then(literal("get")
                                                .executes(MissionCommands::getWeeklyMissions)
                                                .then(argument("mission_type", StringArgumentType.string())
                                                        .suggests(new MissionTypeSuggester())
                                                        .executes(MissionCommands::getCurrentMissions)
                                                )
                                        )
                                        .then(literal("finish")
                                                .then(argument("mission_name", StringArgumentType.string())
                                                        .suggests(new CurrentMissionSuggester())
                                                        .executes(MissionCommands::attemptCompleteMission)

                                                )
                                        )
                                        .then(literal("witness")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::witnessPlayer)
                                                )
                                        )
                                        .then(literal("show-bound")
                                                .executes(MissionCommands::getBoundMissions)
                                        )
                        )
        );

        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions-assignee")
                                        .then(literal("reward")
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(MissionCommands::rewardAssignedMission)
                                                )
                                        )
                        )
        );

    }
}
