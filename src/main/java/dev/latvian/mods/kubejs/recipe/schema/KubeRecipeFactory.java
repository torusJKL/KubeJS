package dev.latvian.mods.kubejs.recipe.schema;

import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public record KubeRecipeFactory(ResourceLocation id, TypeInfo recipeType, Supplier<? extends KubeRecipe> factory) {
	public static final KubeRecipeFactory DEFAULT = new KubeRecipeFactory(KubeJS.id("basic"), TypeInfo.of(KubeRecipe.class), KubeRecipe::new);

	public KubeRecipeFactory(ResourceLocation id, Class<?> recipeType, Supplier<? extends KubeRecipe> factory) {
		this(id, TypeInfo.of(recipeType), factory);
	}

	public KubeRecipe create() {
		return factory.get();
	}
}
