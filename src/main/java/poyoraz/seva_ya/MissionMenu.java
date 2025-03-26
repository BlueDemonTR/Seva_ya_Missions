package poyoraz.seva_ya;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;

import java.util.*;
import java.util.function.Function;

public class MissionMenu {
    public static void create(
            ArrayList<Mission> missions,
            Function<Text, Void> feedbackCallback,
            Function<MissionType, ClickEvent> typeClickCallback,
            Function<Mission, ClickEvent> completeCallback
    ) {
        Set<MissionType> missionTypes = getMissionTypes(missions);

        if(missionTypes.isEmpty()) {
            feedbackCallback.apply(Text.literal("No missions found with the given type"));
            return;
        }

        if(missionTypes.size() == 1) {
            createMissionScreen(missions, feedbackCallback, completeCallback);
            return;
        }

        createTypeScreen(missionTypes, feedbackCallback, typeClickCallback);
    }

    private static void createMissionScreen(
            ArrayList<Mission> missions,
            Function<Text, Void> feedbackCallback,
            Function<Mission, ClickEvent> completeCallback
    ) {
        MutableText text = Text.literal("");

        missions.forEach(mission -> {
            MutableText missionText = (MutableText) mission.toText();

            MutableText finishButton = Text.literal("[Finish]");
            finishButton.setStyle(finishButton
                    .getStyle()
                    .withColor(Formatting.BLUE)
                    .withClickEvent(completeCallback.apply(mission))
            );

            missionText.append(" ").append(finishButton);
            text.append(missionText);

            if(missions.getLast() != mission) {
                text.append("\n");
            }
        });

        feedbackCallback.apply(text);
    }

    private static void createTypeScreen(
            Set<MissionType> missionTypes,
            Function<Text, Void> feedbackCallback,
            Function<MissionType, ClickEvent> typeClickCallback
    ) {
        MutableText text = Text.literal("");

        missionTypes.forEach(missionType -> {
            MutableText typeText = Text
                    .literal("")
                    .append("[")
                    .append(missionType.name())
                    .append("]");

            typeText.setStyle(
                    typeText.getStyle()
                            .withClickEvent(typeClickCallback.apply(missionType))
            );

            text
                    .append(typeText)
                    .append("\n");
        });

        text.append("^ Choose a mission type");

        feedbackCallback.apply(text);
    }

    private static Set<MissionType> getMissionTypes(ArrayList<Mission> missions) {
        Set<MissionType> missionTypes = new HashSet<>();

        missions.forEach(mission -> {
            missionTypes.add(mission.type);
        });

        return missionTypes;
    }
}
