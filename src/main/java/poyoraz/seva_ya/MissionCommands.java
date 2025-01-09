package poyoraz.seva_ya;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;

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
        ArrayList<Mission> weeklyMissions = MissionHolder.getWeeklyMissions(commandContext.getSource().getServer());

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

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions")
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
                        )
        );
    }
}
