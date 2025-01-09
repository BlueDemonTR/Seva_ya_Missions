package poyoraz.seva_ya;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import poyoraz.seva_ya.config.MissionsConfig;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;

import static net.minecraft.server.command.CommandManager.*;

public class MissionCommands {
    public static int base(CommandContext<ServerCommandSource> commandContext) {
        commandContext.getSource().sendFeedback(() -> Text.literal(StateSaverAndLoader.getServerState(commandContext.getSource().getServer()).serializedMissions), false);
        return 1;
    }

    public static int getMissions(CommandContext<ServerCommandSource> commandContext) {
        String str = "";

        for (int i = 0; i < MissionHolder.weeklyMissions.size(); i++) {
            str = str.concat(MissionHolder.weeklyMissions.get(i).toString());
        }

        String finalStr = str;
        commandContext.getSource().sendFeedback(() -> Text.literal(finalStr), false);

        return 1;
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(
                (commandDispatcher, commandRegistryAccess, registrationEnvironment) ->
                        commandDispatcher.register(
                                literal("missions")
                                        .executes(MissionCommands::base)
                                        .then(literal("show")
                                                .executes(MissionCommands::getMissions)
                                        )
                                        .then(literal("reroll")
                                                .executes(MissionHolder::rerollMissions)
                                        )
                        )
        );
    }
}
