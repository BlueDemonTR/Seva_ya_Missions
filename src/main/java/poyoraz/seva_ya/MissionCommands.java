package poyoraz.seva_ya;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.PlayerData;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;

public class MissionCommands {
    public static int base(CommandContext<ServerCommandSource> commandContext) {
        return 1;
    }

    public static int getAllMissions(CommandContext<ServerCommandSource> commandContext) {
        commandContext.getSource().sendFeedback(() ->
                        Text.literal(
                                getMissionsAsString(MissionHolder.missions)
                        ),
                false
        );

        return 1;
    }

    public static int getMissions(CommandContext<ServerCommandSource> commandContext) {
        ArrayList<Mission> weeklyMissions = MissionHolder.getWeeklyMissionsAdmin(commandContext.getSource().getServer());

        commandContext.getSource().sendFeedback(() ->
                Text.literal(
                        getMissionsAsString(weeklyMissions)
                ),
                false
        );

        return 1;
    }

    private static String getMissionsAsString(ArrayList<Mission> missions) {
        String str = "";

        for (Mission mission : missions) {
            str = str.concat(mission.toString());
        }

        return str;
    }

    public static int getWeeklyMissions(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity player = commandContext.getSource().getPlayer();

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

        getMissions(commandContext);
        return 1;
    }

    public static int attemptCompleteMission(CommandContext<ServerCommandSource> commandContext) {
        String name = "";
        Mission mission = null;

        try {
            name = StringArgumentType.getString(commandContext, "mission_name");
            mission = MissionHolder.getMissionByName(name);
            if(mission == null) throw new Exception("No such mission");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LivingEntity player = commandContext.getSource().getPlayer();

        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

        playerData.tryingToComplete = mission;
        playerData.witnesses.clear();

        checkMissionCompletion(player);
        return 1;
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


    public static int witnessPlayer(CommandContext<ServerCommandSource> commandContext) {
        LivingEntity player;

        try {
            player = EntityArgumentType.getPlayer(commandContext, "player");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(Objects.equals(commandContext.getSource().getPlayer(), player)) {
            commandContext.getSource().sendFeedback(() ->
                            Text.literal(
                                    "You can't witness your own mission"
                            ),
                    false
            );
            return 0;
        }

        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        UUID uuid = Objects.requireNonNull(commandContext.getSource().getPlayer()).getUuid();

        if(playerData.tryingToComplete == null) {
            commandContext.getSource().sendFeedback(() ->
                            Text.literal(
                                    "This player isn't trying to complete a mission"
                            ),
                    false
            );
            return 0;
        }

        if(playerData.witnesses.contains(uuid)) {
            commandContext.getSource().sendFeedback(() ->
                            Text.literal(
                                    "You have already witnessed this mission"
                            ),
                    false
            );
            return 0;
        }

        playerData.witnesses.add(uuid);
        checkMissionCompletion(player);

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
                                                .executes(MissionCommands::getMissions)
                                        )
                                        .then(literal("showAll")
                                                .executes(MissionCommands::getAllMissions)
                                        )
                                        .then(literal("reroll")
                                                .executes(MissionHolder::rerollMissions)
                                        )
                                        .then(literal("grant")
                                                .then(argument("player", EntityArgumentType.players())
                                                        .executes(MissionHolder::grantMissions)
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
                                                            return (new MissionSuggester()).getSuggestions(commandContext, suggestionsBuilder);
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
