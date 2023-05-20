package dev.latvian.mods.kubejs.recipe.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeExceptionJS;
import dev.latvian.mods.kubejs.recipe.RecipeFunction;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.OptionalRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.util.JsonIO;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RecipeSchema {
	public static final RecipeComponent<InputItem> INPUT_ITEM = new RecipeComponent<>() {
		@Override
		public String componentType() {
			return "input_item";
		}

		@Override
		public RecipeComponentType getType() {
			return RecipeComponentType.INPUT;
		}

		@Override
		public JsonElement write(InputItem value) {
			return value == InputItem.EMPTY ? null : value.ingredient.toJson();
		}

		@Override
		public InputItem read(Object from) {
			var i = InputItem.of(from);

			if (i.isEmpty()) {
				throw new RecipeExceptionJS(from + " is not a valid ingredient!");
			}

			return i;

			/*
			var ingredient = InputItem.of(o);

			if (ingredient.isEmpty() && !key.isEmpty()) {
				return ingredient;
			} else if (ingredient.ingredient == Ingredient.EMPTY) {
				if (key.isEmpty()) {
					throw new RecipeExceptionJS(o + " is not a valid ingredient!");
				} else {
					throw new RecipeExceptionJS(o + " with key '" + key + "' is not a valid ingredient!");
				}
			}

			return ingredient;
			 */
		}
	};

	public static final RecipeComponent<InputItem> DEFAULT_INPUT_ITEM = INPUT_ITEM.optional(InputItem.EMPTY);
	public static final RecipeComponent<List<InputItem>> INPUT_ITEM_ARRAY = INPUT_ITEM.asArray();

	public static final RecipeComponent<OutputItem> OUTPUT_ITEM = new RecipeComponent<>() {
		@Override
		public String componentType() {
			return "output_item";
		}

		@Override
		public RecipeComponentType getType() {
			return RecipeComponentType.OUTPUT;
		}

		@Override
		public JsonElement write(OutputItem value) {
			if (value == OutputItem.EMPTY) {
				return null;
			}

			var json = new JsonObject();
			json.addProperty("item", value.item.kjs$getId());
			json.addProperty("count", value.item.getCount());

			if (value.item.getTag() != null) {
				json.addProperty("nbt", value.item.getTag().toString());
			}

			if (value.hasChance()) {
				json.addProperty("chance", value.getChance());
			}

			return json;
		}

		@Override
		public OutputItem read(Object from) {
			var i = OutputItem.of(from);

			if (i.isEmpty()) {
				throw new RecipeExceptionJS(from + " is not a valid result!");
			}

			return i;
		}
	};

	public static final RecipeComponent<OutputItem> DEFAULT_OUTPUT_ITEM = OUTPUT_ITEM.optional(OutputItem.EMPTY);
	public static final RecipeComponent<List<OutputItem>> OUTPUT_ITEM_ARRAY = OUTPUT_ITEM.asArray();

	public final Supplier<RecipeJS> factory;
	public final RecipeKey<?>[] keys;
	private int minRequiredArguments;
	private Map<Integer, RecipeConstructor> constructors;

	public RecipeSchema(Supplier<RecipeJS> factory, RecipeKey<?>... keys) {
		this.factory = factory;
		this.keys = keys;
		this.minRequiredArguments = 0;

		var set = new HashSet<String>();

		for (int i = 0; i < keys.length; i++) {
			if (!(keys[i].component() instanceof OptionalRecipeComponent)) {
				if (minRequiredArguments > 0) {
					throw new IllegalStateException("Required key '" + keys[i].name() + "' must be ahead of other default keys!");
				}
			} else if (minRequiredArguments == 0) {
				minRequiredArguments = i + 1;
			}

			if (!set.add(keys[i].name())) {
				throw new IllegalStateException("Duplicate key '" + keys[i].name() + "'");
			}
		}
	}

	public RecipeSchema(RecipeKey<?>... keys) {
		this(RecipeJS::new, keys);
	}

	public RecipeSchema constructor(RecipeConstructor.Factory factory, RecipeKey<?>... keys) {
		var c = new RecipeConstructor(this, keys, factory);

		if (constructors == null) {
			constructors = new HashMap<>(3);
		}

		if (constructors.put(c.keys().length, c) != null) {
			throw new IllegalStateException("Constructor with " + c.keys().length + " arguments already exists!");
		}

		return this;
	}

	public RecipeSchema constructor(RecipeKey<?>... keys) {
		return constructor(RecipeConstructor.Factory.DEFAULT, keys);
	}

	public Map<Integer, RecipeConstructor> constructors() {
		if (keys.length == 0) {
			return Map.of();
		}

		if (constructors == null) {
			constructors = new HashMap<>(1);

			KubeJS.LOGGER.info("Generating constructors for " + factory.get().getClass().getName());

			for (int a = Math.max(2, minRequiredArguments); a < keys.length; a++) {
				KubeJS.LOGGER.info("> " + a);
			}

			constructors.put(keys.length, new RecipeConstructor(this, keys, RecipeConstructor.Factory.DEFAULT));
			constructors.remove(0);
			constructors.remove(1);
		}

		return constructors;
	}

	public int minRequiredArguments() {
		return minRequiredArguments;
	}

	public RecipeJS deserialize(RecipeFunction type, @Nullable ResourceLocation id, JsonObject json) {
		var r = factory.get();
		r.type = type;
		r.id = id;
		r.json = json;
		r.initValues(this, id == null);

		if (id != null && CommonProperties.get().debugInfo) {
			r.originalJson = (JsonObject) JsonIO.copy(json);
		}

		r.deserialize();
		return r;
	}
}