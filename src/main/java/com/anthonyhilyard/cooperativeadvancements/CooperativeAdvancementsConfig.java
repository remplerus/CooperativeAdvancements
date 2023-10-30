package com.anthonyhilyard.cooperativeadvancements;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;


import com.electronwill.nightconfig.core.Config;

public class CooperativeAdvancementsConfig
{
	public static final ModConfigSpec SPEC;
	public static final CooperativeAdvancementsConfig INSTANCE;
	static
	{
		Config.setInsertionOrderPreserved(true);
		Pair<CooperativeAdvancementsConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CooperativeAdvancementsConfig::new);
		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
	}

	public final ModConfigSpec.BooleanValue enabled;
	public final ModConfigSpec.BooleanValue perTeam;

	public CooperativeAdvancementsConfig(ModConfigSpec.Builder build)
	{
		build.comment("Common Configuration").push("options");

		enabled = build.comment(" Enables the entire mod (defaults to true).  Useful option for modpack makers.").define("enabled", true);
		perTeam = build.comment(" Set to true to only share advancements between members of the same team.").define("per_team", false);

		build.pop();
	}
}
