package dev.latvian.mods.kubejs.recipe;

import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeOptional;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.type.TypeInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

/**
 * Represents a typed data key in a recipe schema. This refers to both the literal "key" used in the recipe JSON
 * and to a "holder" that can be used to lookup and set data in the recipe itself. It is also used by recipe constructors
 * in order to determine the order and types of arguments, as well as whether those arguments are optional or not.
 * <p>
 * A key is always associated with a {@link RecipeComponent} which represents the type of data that it holds.
 * <p>
 * Each recipe key has set of {@link #names} that it may be called by in JSON, as well as a {@link #functionNames} name
 * used in autogenerated methods and a primary {@link #name} used for serialization.
 * <p>
 * A key may also be {@link #optional}, which means that its value may not always be present in JSON and will also not
 * be serialized or written to the value map unless it is declared as {@link #alwaysWrite}.
 * <p>
 * By default, each key will have a "builder" method generated for scripts using the first {@link #functionNames} name.
 * You can disable this by setting {@link #noFunctions()} to true.
 * <p>
 * Finally, some types of components such as items or fluids may perform validation to ensure that they
 * aren't holding empty data. If empty data should explicitly be allowed, you can set {@link #allowEmpty} to true.
 *
 * @param <T> The type of element held by this key's component
 * @see RecipeSchema
 * @see RecipeComponent
 */
public final class RecipeKey<T> {
	public final RecipeComponent<T> component;
	public final TypeInfo typeInfo;
	public final Codec<T> codec;
	public final String name;
	public final ComponentRole role;
	public final SequencedSet<String> names;
	public RecipeOptional<T> optional;
	public boolean excluded;
	public List<String> functionNames;
	public boolean allowEmpty;
	public boolean alwaysWrite;

	public RecipeKey(RecipeComponent<T> component, String name, ComponentRole role) {
		this.component = component;
		this.typeInfo = component.typeInfo();
		this.codec = component.codec();
		this.name = name;
		this.role = role;
		this.names = new LinkedHashSet<>(1);
		this.names.add(name);
		this.optional = null;
		this.excluded = false;
		this.functionNames = null;
		this.allowEmpty = false;
		this.alwaysWrite = false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder(name);

		if (optional != null) {
			sb.append('?');
		}

		sb.append(':');
		sb.append(component);
		return sb.toString();
	}

	/**
	 * Marks this key as optional, meaning that it can be omitted when deserializing
	 * recipes from JSON and will not be serialized unless it is explicitly set.
	 *
	 * @param value The default value of this key; note that the instance supplied here
	 *              will be used directly and across multiple recipes, so make sure
	 *              to only pass immutable objects!
	 * @apiNote Note that this method <i>does not</i> actually set the value during
	 * recipe initialization unless {@link #alwaysWrite} is also set! This is mostly
	 * meant to be used as information for mods like ProbeJS.
	 * @see #optional(RecipeOptional)
	 */
	public RecipeKey<T> optional(T value) {
		return optional(new RecipeOptional.Constant<>(value));
	}

	/**
	 * Marks this key as optional, meaning that it can be omitted when deserializing
	 * recipes from JSON and will not be serialized unless it is explicitly set.
	 *
	 * @param value The default value of this key; unlike in {@link #optional(Object)},
	 *              the value will be computed at recipe initialization time, which makes
	 *              it safe to pass mutable objects here.
	 * @apiNote Note that this method <i>does not</i> actually set the value during
	 * recipe initialization unless {@link #alwaysWrite} is also set! This is mostly
	 * meant to be used as information for mods like ProbeJS.
	 * @see #optional(RecipeOptional)
	 */
	public RecipeKey<T> optional(RecipeOptional<T> value) {
		optional = value;
		return this;
	}

	/**
	 * Marks this key as optional, with a default value of <code>null</code>.
	 *
	 * @implNote Use this in place of regular optional(x) only if the value is dynamic/too complicated to compute from type
	 * @see #optional(RecipeOptional)
	 */
	public RecipeKey<T> defaultOptional() {
		optional = Cast.to(RecipeOptional.DEFAULT);
		return this;
	}

	/**
	 * Returns true if this key has a defined optional value
	 *
	 * @return Whether this key is optional
	 */
	public boolean optional() {
		return optional != null;
	}

	/**
	 * Adds an alternate name for this key in JSON
	 *
	 * @param name Another name this key may be called by
	 */
	public RecipeKey<T> alt(String name) {
		names.add(name);
		return this;
	}

	/**
	 * Adds multiple alternate names for this key in JSON
	 *
	 * @param names A list of names this key may be called by
	 */
	public RecipeKey<T> alt(String... names) {
		this.names.addAll(List.of(names));
		return this;
	}

	/**
	 * Excludes this key from auto-generated constructors.
	 * <i>Requires</i> optional() value to also be set.
	 * <p>
	 * This method does nothing if a custom constructor has been set.
	 */
	public RecipeKey<T> exclude() {
		excluded = true;
		return this;
	}

	public boolean includeInAutoConstructors() {
		return optional == null || !excluded;
	}

	/**
	 * Disables the generation of builder functions for this key.
	 */
	public RecipeKey<T> noFunctions() {
		functionNames = List.of();
		return this;
	}

	/**
	 * Sets a list of names that are used to auto-generate builder functions in JS, e.g. <code>.xp(value)</code>.
	 * The first one will be the preferred one that ProbeJS and other third-party documentation should recommend.
	 */
	public RecipeKey<T> functionNames(List<String> names) {
		functionNames = names;
		return this;
	}

	/**
	 * By default, some components may disallow empty values and throw an exception if encountered.
	 * Use this method to explicitly allow such empty values (ex. minecraft:air) in results/ingredients.
	 */
	public RecipeKey<T> allowEmpty() {
		allowEmpty = true;
		return this;
	}

	/**
	 * Set this in order to always write optional keys, even if their value hasn't changed.
	 * <p>
	 * This can also be used to always populate the value map with a default value for an optional key.
	 */
	public RecipeKey<T> alwaysWrite() {
		alwaysWrite = true;
		return this;
	}

	public String getPreferredBuilderKey() {
		return functionNames == null ? name : functionNames.getFirst();
	}
}
