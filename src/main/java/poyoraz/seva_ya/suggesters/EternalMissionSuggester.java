package poyoraz.seva_ya.suggesters;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.GlobalMissionHolder;
import poyoraz.seva_ya.StateSaverAndLoader;
import poyoraz.seva_ya.models.PlayerData;

import java.util.concurrent.CompletableFuture;

public class EternalMissionSuggester extends AbstractMissionSuggester {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<ServerCommandSource> commandContext,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException {
        LivingEntity target = EntityArgumentType.getPlayer(commandContext, "player");

        PlayerData playerData = StateSaverAndLoader.getPlayerState(target);

        suggestMissions(playerData.boundMissions, suggestionsBuilder);

        return suggestionsBuilder.buildFuture();
    }
}
