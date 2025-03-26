package poyoraz.seva_ya.suggesters;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import poyoraz.seva_ya.CurrentMissionsHolder;
import poyoraz.seva_ya.models.Mission;

import java.util.List;

abstract class AbstractMissionSuggester implements SuggestionProvider<ServerCommandSource> {
    protected void suggestMissions(List<Mission> missions, SuggestionsBuilder suggestionsBuilder) {
        for(Mission mission : missions) {
            if(CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), mission.name)) {
                suggestionsBuilder.suggest(mission.name);
            }
        }
    }
}
