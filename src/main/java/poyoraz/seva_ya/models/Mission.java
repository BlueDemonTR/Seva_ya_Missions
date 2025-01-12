package poyoraz.seva_ya.models;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.compress.utils.Lists;
import poyoraz.seva_ya.config.MissionsConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Mission {
    public String id;
    public String name;
    public String description;
    public MissionType type;
    public int reward;
    public UUID assignee = null;

    public ArrayList<ItemStack> cachedRewards = null;

    public Mission(String id, String name, String description, MissionType type, int reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.reward = reward;
    }

    public static MissionType getTypeFromInt(int type) {
        switch (type) {
            case 0 -> {
                return MissionType.EASY;
            }
            case 1 -> {
                return MissionType.MEDIUM;
            }
            case 2 -> {
                return MissionType.HARD;
            }
            case 3 -> {
                return MissionType.ASSIGNED;
            }
            case 4 -> {
                return MissionType.ETERNAL;
            }
        }

        return MissionType.ASSIGNED;
    }

    public Text getTypeLabel() {
        switch (type) {
            case EASY -> {
                return Text.literal("[Easy]").formatted(Formatting.GRAY);
            }
            case MEDIUM -> {
                return Text.literal("[Medium]").formatted(Formatting.BLUE);
            }
            case HARD -> {
                return Text.literal("[Hard]").formatted(Formatting.GOLD);
            }
            case ASSIGNED -> {
                return Text.literal("[Assigned]").formatted(Formatting.GREEN);
            }
            case ETERNAL -> {
                return Text.literal("[Eternal]").formatted(Formatting.DARK_PURPLE);
            }
        }

        return Text.literal("Invalid Type");
    }

    public Text toText() {
        MutableText nameText = Text.literal(name).formatted(Formatting.BOLD);

        nameText.setStyle(
                nameText
                        .getStyle()
                        .withHoverEvent(
                                new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT, Text.literal(description)
                                )
                        )
        );

        ArrayList<ItemStack> rewards = getRewards();

        MutableText rewardsText = Text.literal("");

        for (ItemStack reward : rewards) {
            MutableText rewardText = Text.literal(String.valueOf(reward.getCount()));
            if(!reward.equals(rewards.getLast())) {
                rewardText.append("+");
            }

            rewardsText.append(rewardText);
        }

        rewardsText.setStyle(
                rewardsText
                        .getStyle()
                        .withHoverEvent(
                                new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT, getRewardText()
                                )
                        )
        );

        return Text
                .literal("")
                .append(getTypeLabel())
                .append(" ")
                .append(nameText)
                .append(": ")
                .append(rewardsText);
    }

    @Override
    public String toString() {
        return "§l[" + type.name() + "]§r " + name + ": " + description + "\nReward: " + getRewardText();
    }

    public ArrayList<ItemStack> getRewards() {
        if(cachedRewards != null) return cachedRewards;

        ArrayList<ItemStack> rewards = new ArrayList<>();

        int remainingAwards = reward;

        ArrayList<String> reversedAwards = new ArrayList<>(MissionsConfig.rewards.reversed());

        for (int i = 0; i < MissionsConfig.rewards.size(); i++) {
            int power = MissionsConfig.rewards.size() - (i + 1);
            int value = (int) Math.pow(MissionsConfig.scalingBase, power);

            String currentAward = reversedAwards.get(i);

            int itemCount = (int) Math.floor((double) remainingAwards / value);

            remainingAwards %= value;

            rewards.add(
                new ItemStack(
                        Registries.ITEM.get(Identifier.of(currentAward)),
                        itemCount
                )
            );
        }

        cachedRewards = rewards;
        return rewards;
    }

    public Text getRewardText() {
        MutableText str = Text.literal("");

        ArrayList<ItemStack> rewards = getRewards();

        for (ItemStack reward : rewards) {
            int count = reward.getCount();

            if(count == 0) continue;

            str.append(String.valueOf(count));
            str.append(" ");
            str.append(reward.getName().getString());
            str.append(count != 1 ? "s" : "");

            if(!reward.equals(rewards.getLast())) {
                str.append(", ");
            }
        }

        return str;
    }

    public void rewardPlayer(PlayerEntity player) {
        player.sendMessage(
                Text
                        .literal(
                            "You have completed the mission successfully! You get "
                        )
                        .append(getRewardText()),
                false
        );

        getRewards().forEach(player::giveOrDropStack);
    }
}
