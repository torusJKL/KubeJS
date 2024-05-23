package dev.latvian.mods.kubejs.color;

import dev.latvian.mods.kubejs.bindings.ColorWrapper;
import dev.latvian.mods.rhino.util.SpecialEquality;
import net.minecraft.network.chat.TextColor;

public interface Color extends SpecialEquality {
	int getArgbJS();

	default int getRgbJS() {
		return getArgbJS() & 0xFFFFFF;
	}

	default int getFireworkColorJS() {
		return getRgbJS();
	}

	default String getHexJS() {
		return String.format("#%08X", getArgbJS());
	}

	default String getSerializeJS() {
		return getHexJS();
	}

	default TextColor createTextColorJS() {
		return TextColor.fromRgb(getRgbJS());
	}

	@Override
	default boolean specialEquals(Object o, boolean shallow) {
		Color c = ColorWrapper.of(o);
		return shallow ? (getArgbJS() == c.getArgbJS()) : (getRgbJS() == c.getRgbJS());
	}
}