package poyoraz.seva_ya;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;
import poyoraz.seva_ya.suggesters.CurrentMissionSuggester;
import poyoraz.seva_ya.suggesters.EternalMissionSuggester;

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

    public static int getAllMissions(CommandContext<ServerCommandSource> commandContext) {
        feedback(
                GlobalMissionHolder.getMissionsAsString(GlobalMissionHolder.missions),
                commandContext.getSource()
        );

        return 1;
    }

    public static int getCurrentMissions(CommandContext<ServerCommandSource> commandContext) {
        feedback(
                GlobalMissionHolder.getMissionsAsString(
                        CurrentMissionsHolder.getMissions(commandContext.getSource().getServer())
                ),
                commandContext.getSource()
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
                GlobalMissionHolder.getMissionsAsString(
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
            mission = CurrentMissionsHolder.getMissionByName(name);
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
        LivingEntity missionOwner;

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

        playerData.boundMissions.add(playerData.tryingToComplete);

        return 1;
    }

    public static int unbindEternalMission(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity missionOwner;
        Mission mission;

        try {
            missionOwner = EntityArgumentType.getPlayer(commandContext, "player");
            String name = StringArgumentType.getString(commandContext, "mission_name");
            mission = GlobalMissionHolder.getMissionByName(name);

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

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missionsAdmin")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(MissionCommands::base)
                                        .then(literal("show")
                                                .executes(MissionCommands::getCurrentMissions)
                                        )
                                        .then(literal("showAll")
                                                .executes(MissionCommands::getAllMissions)
                                        )
                                        .then(literal("reroll")
                                                .executes(MissionCommands::rerollMissions)
                                        )
                                        .then(literal("grant")
                                                .then(argument("player", EntityArgumentType.players())
                                                        .executes(MissionCommands::grantMissions)
                                                )
                                        )
                                        .then(literal("bind")
                                                .then(argument("player", EntityArgumentType.players())
                                                        .executes(MissionCommands::bindEternalMission)
                                                )
                                        )
                                        .then(literal("unbind")
                                                .then(argument("player", EntityArgumentType.players())
                                                        .then(argument("mission_name", StringArgumentType.string())
                                                                .suggests((commandContext, suggestionsBuilder) -> {
                                                                    return (new EternalMissionSuggester())
                                                                            .getSuggestions(
                                                                                    commandContext,
                                                                                    suggestionsBuilder
                                                                            );
                                                                })
                                                                .executes(MissionCommands::unbindEternalMission)
                                                        )
                                                )
                                        )
                        )
        );

        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions")
                                        .then(literal("get")
                                                .executes(MissionCommands::getWeeklyMissions)
                                        )
                                        .then(literal("finish")
                                                .then(argument("mission_name", StringArgumentType.string())
                                                        .suggests((commandContext, suggestionsBuilder) -> {
                                                            return (new CurrentMissionSuggester())
                                                                    .getSuggestions(
                                                                            commandContext,
                                                                            suggestionsBuilder
                                                                    );
                                                        })
                                                        .executes(MissionCommands::attemptCompleteMission)

                                                )
                                        )
                                        .then(literal("witness")
                                                .then(argument("player", EntityArgumentType.players())
                                                        .executes(MissionCommands::witnessPlayer)
                                                )
                                        )
                                        .then(literal("showBound")
                                                .executes(MissionCommands::getBoundMissions)
                                        )
                        )
        );

    }
}
