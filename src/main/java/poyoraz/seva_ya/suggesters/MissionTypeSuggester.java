package poyoraz.seva_ya.suggesters;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.GlobalMissionHolder;
import poyoraz.seva_ya.models.MissionType;

import java.util.concurrent.CompletableFuture;

public class MissionTypeSuggester implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<ServerCommandSource> commandContext,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException {
        suggestionsBuilder.suggest(MissionType.EASY.name());
        suggestionsBuilder.suggest(MissionType.MEDIUM.name());
        suggestionsBuilder.suggest(MissionType.HARD.name());
        suggestionsBuilder.suggest(MissionType.ASSIGNED.name());
        suggestionsBuilder.suggest(MissionType.ETERNAL.name());

        return suggestionsBuilder.buildFuture();
    }
}
