package poyoraz.seva_ya;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.PlayerData;

import java.util.concurrent.CompletableFuture;

public class MissionSuggester implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        LivingEntity player = commandContext.getSource().getPlayer();

        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);

        if(playerData.missionsPulled) {
            for(Mission mission : MissionHolder.getWeeklyMissionsAdmin(commandContext.getSource().getServer())) {

                if(CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), mission.name)) {
                    suggestionsBuilder.suggest("\"" + mission.name + "\"");
                }
            }
        }

        return suggestionsBuilder.buildFuture();
    }
}
