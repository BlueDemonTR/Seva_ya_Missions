package poyoraz.seva_ya.models;

import java.util.ArrayList;
import java.util.UUID;

public class PlayerData {
    public boolean missionsPulled = false;
    public Mission tryingToComplete = null;
    public ArrayList<UUID> witnesses = new ArrayList<>();
    public ArrayList<Mission> boundMissions = new ArrayList<>();
}
