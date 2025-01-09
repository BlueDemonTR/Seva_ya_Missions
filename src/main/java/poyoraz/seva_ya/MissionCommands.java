package poyoraz.seva_ya;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.PlayerData;

import java.util.ArrayList;

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
                        )
        );

    }
}
