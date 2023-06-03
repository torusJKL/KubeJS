package dev.latvian.mods.kubejs.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.util.NotificationBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class NotificationToast implements Toast {
	public interface ToastIcon {
		void draw(Minecraft mc, PoseStack pose, int size);
	}

	public static final Map<Integer, BiFunction<Minecraft, String, ToastIcon>> ICONS = new HashMap<>(Map.of(
			1, TextureIcon::new,
			2, ItemIcon::new,
			3, AtlasIcon::new
	));

	public record TextureIcon(ResourceLocation texture) implements ToastIcon {
		public TextureIcon(Minecraft ignored, String icon) {
			this(new ResourceLocation(icon));
		}

		@Override
		public void draw(Minecraft mc, PoseStack pose, int size) {
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			var m = pose.last().pose();

			int p0 = -size / 2;
			int p1 = p0 + size;

			var buf = Tesselator.getInstance().getBuilder();
			buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			buf.vertex(m, p0, p1, 0F).uv(0F, 1F).color(255, 255, 255, 255).endVertex();
			buf.vertex(m, p1, p1, 0F).uv(1F, 1F).color(255, 255, 255, 255).endVertex();
			buf.vertex(m, p1, p0, 0F).uv(1F, 0F).color(255, 255, 255, 255).endVertex();
			buf.vertex(m, p0, p0, 0F).uv(0F, 0F).color(255, 255, 255, 255).endVertex();
			BufferUploader.drawWithShader(buf.end());
		}
	}

	public record ItemIcon(ItemStack stack) implements ToastIcon {
		public ItemIcon(Minecraft ignored, String icon) {
			this(ItemStackJS.of(icon));
		}

		@Override
		public void draw(Minecraft mc, PoseStack pose, int size) {
		}
	}

	public record AtlasIcon(TextureAtlasSprite sprite) implements ToastIcon {
		public AtlasIcon(Minecraft mc, String icon) {
			this(mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(icon)));
		}

		@Override
		public void draw(Minecraft mc, PoseStack pose, int size) {
		}
	}

	private final NotificationBuilder notification;

	private final long duration;
	private final ToastIcon icon;
	private final List<FormattedCharSequence> text;
	private int width;

	private long lastChanged;
	private boolean changed;

	public NotificationToast(Minecraft mc, NotificationBuilder notification) {
		this.notification = notification;
		this.duration = notification.duration.toMillis();

		this.icon = ICONS.containsKey(this.notification.iconType) ? ICONS.get(this.notification.iconType).apply(mc, this.notification.icon) : null;

		this.text = new ArrayList<>(2);
		this.width = 0;

		if (notification.title.getContents() != ComponentContents.EMPTY) {
			this.text.addAll(mc.font.split(mc.font.getSplitter().headByWidth(notification.title, 300, Style.EMPTY.applyFormat(ChatFormatting.YELLOW)), 300));
		}

		if (notification.subtitle.getContents() != ComponentContents.EMPTY) {
			this.text.addAll(mc.font.split(mc.font.getSplitter().headByWidth(notification.subtitle, 300, Style.EMPTY), 300));
		}

		for (var l : text) {
			this.width = Math.max(this.width, mc.font.width(l));
		}

		this.width += 12;

		if (this.icon != null) {
			this.width += 24;
		}

		//this.width = Math.max(160, 30 + Math.max(mc.font.width(component), component2 == null ? 0 : mc.font.width(component2));
	}

	@Override
	public int width() {
		return this.width;
	}

	private void drawRectangle(Matrix4f m, int x0, int y0, int x1, int y1, int r, int g, int b) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		var buf = Tesselator.getInstance().getBuilder();
		buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buf.vertex(m, x0, y1, 0F).color(r, g, b, 255).endVertex();
		buf.vertex(m, x1, y1, 0F).color(r, g, b, 255).endVertex();
		buf.vertex(m, x1, y0, 0F).color(r, g, b, 255).endVertex();
		buf.vertex(m, x0, y0, 0F).color(r, g, b, 255).endVertex();
		BufferUploader.drawWithShader(buf.end());
	}

	@Override
	public Toast.Visibility render(PoseStack poseStack, ToastComponent toastComponent, long l) {
		if (this.changed) {
			this.lastChanged = l;
			this.changed = false;
		}

		var mc = toastComponent.getMinecraft();

		poseStack.pushPose();
		poseStack.translate(-2D, 2D, 0D);
		var m = poseStack.last().pose();
		int w = width();
		int h = height() - 4;

		int oc = notification.outlineColor.getRgbJS();
		int ocr = FastColor.ARGB32.red(oc);
		int ocg = FastColor.ARGB32.green(oc);
		int ocb = FastColor.ARGB32.blue(oc);

		int bc = notification.borderColor.getRgbJS();
		int bcr = FastColor.ARGB32.red(bc);
		int bcg = FastColor.ARGB32.green(bc);
		int bcb = FastColor.ARGB32.blue(bc);

		int bgc = notification.backgroundColor.getRgbJS();
		int bgcr = FastColor.ARGB32.red(bgc);
		int bgcg = FastColor.ARGB32.green(bgc);
		int bgcb = FastColor.ARGB32.blue(bgc);

		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		drawRectangle(m, 2, 0, w - 2, h, ocr, ocg, ocb);
		drawRectangle(m, 0, 2, w, h - 2, ocr, ocg, ocb);
		drawRectangle(m, 1, 1, w - 1, h - 1, ocr, ocg, ocb);
		drawRectangle(m, 2, 1, w - 2, h - 1, bcr, bcg, bcb);
		drawRectangle(m, 1, 2, w - 1, h - 2, bcr, bcg, bcb);
		drawRectangle(m, 2, 2, w - 2, h - 2, bgcr, bgcg, bgcb);
		RenderSystem.enableTexture();

		if (icon != null) {
			poseStack.pushPose();
			int off = h / 2;
			poseStack.translate(off, off, 0D);
			icon.draw(mc, poseStack, notification.iconSize);
			poseStack.popPose();
		}

		int th = icon == null ? 6 : 26;
		int tv = (h - text.size() * 10) / 2 + 1;

		for (var i = 0; i < text.size(); i++) {
			var line = text.get(i);
			mc.font.drawShadow(poseStack, line, th, tv + i * 10, 0xFFFFFF);
		}

		/*

		int i = this.width();
		if (i == 160 && this.text.size() <= 1) {
			toastComponent.blit(poseStack, 0, 0, 0, 64, i, this.height());
		} else {
			int j = this.height();
			int k = 28;
			int m = Math.min(4, j - 28);
			this.renderBackgroundRow(poseStack, toastComponent, i, 0, 0, 28);

			for (int n = 28; n < j - m; n += 10) {
				this.renderBackgroundRow(poseStack, toastComponent, i, 16, n, Math.min(16, j - n - m));
			}

			this.renderBackgroundRow(poseStack, toastComponent, i, 32 - m, j - m, m);
		}

		if (this.text == null) {
			toastComponent.getMinecraft().font.draw(poseStack, notification.title, 18.0F, 12.0F, -256);
		} else {
			toastComponent.getMinecraft().font.draw(poseStack, notification.title, 18.0F, 7.0F, -256);

			for (int j = 0; j < this.text.size(); ++j) {
				toastComponent.getMinecraft().font.draw(poseStack, this.text.get(j), 18.0F, (float) (18 + j * 12), -1);
			}
		}
		 */

		poseStack.popPose();

		return l - this.lastChanged < duration ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
	}

	@Override
	public Object getToken() {
		return NO_TOKEN;
	}
}