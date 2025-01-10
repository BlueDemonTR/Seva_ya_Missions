package poyoraz.seva_ya.models;

import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class Mission {
    public String id;
    public String name;
    public String description;
    public MissionType type;
    public int reward;
    public UUID assignee = null;

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

    public String getTypeName() {
        switch (type) {
            case EASY -> {
                return "Easy";
            }
            case MEDIUM -> {
                return "Medium";
            }
            case HARD -> {
                return "Hard";
            }
            case ASSIGNED -> {
                return "Assigned";
            }
            case ETERNAL -> {
                return "Eternal";
            }
        }

        return "Invalid Type";
    }

    @Override
    public String toString() {
        return "§l[" + getTypeName() + "]§r " + name + ": " + description + "\nReward: " + reward + " Sevayic Shards\n";
    }
}
