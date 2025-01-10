package poyoraz.seva_ya.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class MissionsConfig extends MidnightConfig {
    public static final String TEXT = "text";
    public static final String NUMBERS = "numbers";
    public static final String SLIDERS = "sliders";
    public static final String LISTS = "lists";
    public static final String FILES = "files";

    @Comment(category = TEXT) public static Comment missionsExplanation;
    @Entry(category = LISTS, name="All tasks") public static List<String> allTasks = Lists.newArrayList("{}");
    @Entry(category = NUMBERS, name="Easy Task Count Per Week") public static int easyTaskCount = 4;
    @Entry(category = NUMBERS, name="Medium Task Count Per Week") public static int mediumTaskCount = 2;
    @Entry(category = NUMBERS, name="Hard Task Count Per Week") public static int hardTaskCount = 1;
    @Entry(category = NUMBERS, name="Assigned Task Count Per Week") public static int assignedTaskCount = -1;
    @Entry(category = NUMBERS, name="Eternal Task Count Per Week") public static int eternalTaskCount = -1;
    @Entry(category = NUMBERS, name="Scaling Base") public static int scalingBase = 9;
    @Entry(category = LISTS, name="Reward List") public static List<String> rewards = Lists.newArrayList(
            "minecraft:iron_nugget",
            "minecraft:iron_ingot",
            "minecraft:iron_block"
    );
}
