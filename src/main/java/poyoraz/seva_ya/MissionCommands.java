package poyoraz.seva_ya;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;
import poyoraz.seva_ya.models.PlayerData;
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
                        CurrentMissionsHolder.getWeeklyMissions(commandContext.getSource().getServer())
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
        LivingEntity player;

        try {
            player = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LivingEntity contextPlayer = commandContext.getSource().getPlayer();

        if(Objects.equals(contextPlayer, player)) {
            feedback(
                    "You can't witness your own mission",
                    commandContext.getSource()
            );
            return 0;
        }

        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        UUID uuid = Objects.requireNonNull(contextPlayer).getUuid();

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
        CurrentMissionsHolder.checkMissionCompletion(player);

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
                                                            return (new MissionSuggester())
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
                        )
        );

    }
}
