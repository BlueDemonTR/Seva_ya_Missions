package poyoraz.seva_ya.suggesters;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.CurrentMissionsHolder;
import poyoraz.seva_ya.StateSaverAndLoader;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.PlayerData;

import java.util.concurrent.CompletableFuture;

public class CurrentMissionSuggester extends AbstractMissionSuggester {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<ServerCommandSource> commandContext,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException {
        LivingEntity player = commandContext.getSource().getPlayer();


        assert player != null;
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

        if(playerData.missionsPulled) {
            suggestMissions(
                    CurrentMissionsHolder.getMissions(commandContext.getSource().getServer()),
                    suggestionsBuilder
            );
        }

        return suggestionsBuilder.buildFuture();
    }
}
