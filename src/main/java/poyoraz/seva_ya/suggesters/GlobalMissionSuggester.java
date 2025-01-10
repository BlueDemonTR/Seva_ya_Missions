package poyoraz.seva_ya.suggesters;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.GlobalMissionHolder;
import poyoraz.seva_ya.models.Mission;

import java.util.concurrent.CompletableFuture;

public class GlobalMissionSuggester extends AbstractMissionSuggester {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<ServerCommandSource> commandContext,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException {
        suggestMissions(GlobalMissionHolder.getMissions(commandContext.getSource().getServer()), suggestionsBuilder);

        return suggestionsBuilder.buildFuture();
    }
}
