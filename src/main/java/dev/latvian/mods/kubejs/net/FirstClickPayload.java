package dev.latvian.mods.kubejs.net;

import dev.latvian.mods.kubejs.bindings.event.ItemEvents;
import dev.latvian.mods.kubejs.item.ItemClickedEventJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FirstClickPayload(int clickType) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, FirstClickPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(FirstClickPayload::new, FirstClickPayload::clickType);

	@Override
	public Type<?> type() {
		return KubeJSNet.FIRST_CLICK;
	}

	public void handle(IPayloadContext ctx) {
		if (ctx.player() instanceof ServerPlayer serverPlayer) {
			if (clickType == 0 && ItemEvents.FIRST_LEFT_CLICKED.hasListeners()) {
				ctx.enqueueWork(() -> {
					var stack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
					ItemEvents.FIRST_LEFT_CLICKED.post(ScriptType.SERVER, stack.getItem(), new ItemClickedEventJS(serverPlayer, InteractionHand.MAIN_HAND, stack));
				});
			} else if (clickType == 1 && ItemEvents.FIRST_RIGHT_CLICKED.hasListeners()) {
				ctx.enqueueWork(() -> {
					for (var hand : InteractionHand.values()) {
						var stack = serverPlayer.getItemInHand(hand);
						ItemEvents.FIRST_RIGHT_CLICKED.post(ScriptType.SERVER, stack.getItem(), new ItemClickedEventJS(serverPlayer, hand, stack));
					}
				});
			}
		}
	}
}