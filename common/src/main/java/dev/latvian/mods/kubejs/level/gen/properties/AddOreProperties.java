package dev.latvian.mods.kubejs.level.gen.properties;

import com.google.common.collect.Iterables;
import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import dev.latvian.mods.kubejs.level.gen.filter.biome.BiomeFilter;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class AddOreProperties {

	public ResourceLocation id = null;

	public GenerationStep.Decoration worldgenLayer = GenerationStep.Decoration.UNDERGROUND_ORES;
	public BiomeFilter biomes = BiomeFilter.ALWAYS_TRUE;

	// feature configuration
	public List<OreConfiguration.TargetBlockState> targets = new ArrayList<>();
	public int size = 9;
	public boolean noSurface = false;

	// placement configuration
	public IntProvider count = ConstantInt.of(1);
	public int chance = 0;
	public boolean squared = false;
	public HeightRangePlacement height = HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(64));

	public int retrogen = 0;

	public void addTarget(RuleTest ruleTest, BlockStatePredicate targetState) {
		var blockState = Iterables.getFirst(targetState.getBlockStates(), Blocks.AIR.defaultBlockState());
		if (blockState.isAir()) {
			ConsoleJS.STARTUP.error("Target block state is empty!");
		} else {
			targets.add(OreConfiguration.target(ruleTest, blockState));
		}
	}

	public AddOreProperties count(int c) {
		count = ConstantInt.of(c);
		return this;
	}

	public AddOreProperties count(int min, int max) {
		count = UniformInt.of(min, max);
		return this;
	}

	public AddOreProperties count(IntProvider c) {
		count = c;
		return this;
	}

	public AddOreProperties chance(int c) {
		chance = c;
		return this;
	}

	public AddOreProperties size(int s) {
		size = s;
		return this;
	}

	public AddOreProperties squared() {
		return squared(true);
	}

	private AddOreProperties squared(boolean b) {
		squared = b;
		return this;
	}

	// some shortcuts for absolute height ranges
	public AddOreProperties uniformHeight(int min, int max) {
		return uniformHeight(VerticalAnchor.absolute(min), VerticalAnchor.absolute(max));
	}

	public AddOreProperties triangleHeight(int min, int max) {
		return triangleHeight(VerticalAnchor.absolute(min), VerticalAnchor.absolute(max));
	}

	// additional methods that use actual anchors
	public AddOreProperties uniformHeight(VerticalAnchor absolute, VerticalAnchor absolute1) {
		height = HeightRangePlacement.uniform(absolute, absolute1);
		return this;
	}

	public AddOreProperties triangleHeight(VerticalAnchor absolute, VerticalAnchor absolute1) {
		height = HeightRangePlacement.triangle(absolute, absolute1);
		return this;
	}

	// vertical anchors
	public VerticalAnchor aboveBottom(int y) {
		return VerticalAnchor.aboveBottom(y);
	}

	public VerticalAnchor belowTop(int y) {
		return VerticalAnchor.belowTop(y);
	}

	public VerticalAnchor bottom() {
		return VerticalAnchor.bottom();
	}

	public VerticalAnchor top() {
		return VerticalAnchor.top();
	}

	// just here to inform players to **switch their scripts over**
	@Deprecated(forRemoval = true)
	@ApiStatus.ScheduledForRemoval(inVersion = "4.3")
	public void setBlock(String id) {
		ConsoleJS.STARTUP.error("setBlock no longer works with the new worldgen system! Use the target system (addTarget(toReplace, with)) instead!");
	}

	// just here to inform players to **switch their scripts over**
	@Deprecated(forRemoval = true)
	@ApiStatus.ScheduledForRemoval(inVersion = "4.3")
	public Object getSpawnsIn() {
		ConsoleJS.STARTUP.error("spawnsIn no longer works with the new worldgen system! Use the target system (addTarget(toReplace, with)) instead!");
		return null;
	}

	// just here to inform players to **switch their scripts over**
	@Deprecated(forRemoval = true)
	@ApiStatus.ScheduledForRemoval(inVersion = "4.3")
	public void setSpawnsIn() {
		ConsoleJS.STARTUP.error("spawnsIn no longer works with the new worldgen system! Use the target system (addTarget(toReplace, with)) instead!");
	}
}
