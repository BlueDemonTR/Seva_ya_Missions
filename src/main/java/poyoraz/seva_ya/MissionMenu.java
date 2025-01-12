package poyoraz.seva_ya;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import poyoraz.seva_ya.models.Mission;
import poyoraz.seva_ya.models.MissionType;

import java.util.*;
import java.util.function.Function;

public class MissionMenu {
    public static void create(
            ArrayList<Mission> missions,
            Function<Text, Void> feedbackCallback,
            Function<MissionType, ClickEvent> typeClickCallback
    ) {
        Set<MissionType> missionTypes = getMissionTypes(missions);

        if(missionTypes.isEmpty()) {
            feedbackCallback.apply(Text.literal("No missions found with the given type"));
            return;
        }

        if(missionTypes.size() == 1) {
            feedbackCallback.apply(GlobalMissionHolder.getMissionsAsText(missions));
            return;
        }

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
