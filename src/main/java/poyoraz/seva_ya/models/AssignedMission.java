package poyoraz.seva_ya.models;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.UUID;

public class AssignedMission extends Mission {

    public AssignedMission(String id, String name, String description, int reward, UUID assignee) {
        super(id, name, description, MissionType.ASSIGNED, reward);
        this.assignee = assignee;
    }

    public static String getPlayerNameFromAssignedMission(MinecraftServer server, UUID assignee) {
        return Objects.requireNonNull(server.getUserCache()).getByUuid(assignee).get().getName();
    }
}
