package com.cesoti2006.villagediplomacy.client;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.util.ModLang;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws the mod's own nameplates. There is no vanilla name tag involved: the
 * server syncs the identity it generated and this renderer paints it above the
 * entity with the mod's own colours, so future data (quest markers, rank,
 * emotion) can be added without fighting vanilla's label system.
 */
@Mod.EventBusSubscriber(modid = VillageDiplomacy.MODID, value = Dist.CLIENT)
public final class VillagerNameplateRenderer {

    private VillagerNameplateRenderer() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!isEnabled()) return;
        Entity entity = event.getEntity();
        if (entity instanceof Villager || entity instanceof IronGolem) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public static void onRenderLiving(RenderLivingEvent.Post event) {
        if (!isEnabled()) return;

        Entity raw = event.getEntity();
        if (!(raw instanceof LivingEntity entity)) return;
        if (entity.isInvisible()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        double range = VillageDiplomacyConfig.INSTANCE.nameplateRange.get();
        if (player.distanceToSqr(entity) > range * range) return;

        Component plate;
        Component moodLine = null;

        if (entity instanceof Villager villager) {
            VillagerIdentityCache.Identity identity = VillagerIdentityCache.get(villager);
            if (identity == null) return;
            plate = villagerPlate(villager, identity);
            if (!identity.mood().isEmpty()) {
                moodLine = Component.translatable("villagediplomacy.mood." + identity.mood())
                        .withStyle(ChatFormatting.GRAY);
            }
        } else if (entity instanceof IronGolem golem) {
            plate = ModLang.golemTag(golem);
        } else {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Font font = minecraft.font;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.55D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.024F, -0.024F, 0.024F);
        Matrix4f matrix = poseStack.last().pose();

        int plateWidth = font.width(plate);
        font.drawInBatch(plate, -plateWidth / 2.0F, 0.0F, 0xFFFFFFFF, false, matrix,
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0x55000000, event.getPackedLight());

        if (moodLine != null) {
            int moodWidth = font.width(moodLine);
            font.drawInBatch(moodLine, -moodWidth / 2.0F, 10.0F, 0xFFFFFFFF, false, matrix,
                    event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, event.getPackedLight());
        }

        poseStack.popPose();

        renderBubble(event, entity, minecraft, font);
    }

    /**
     * Speech bubble above the nameplate. It is drawn by the client, so each player
     * can choose between chat, bubbles or both, and the bubble never blocks the
     * chat box while you are talking to someone.
     */
    private static void renderBubble(RenderLivingEvent.Post<?, ?> event, LivingEntity entity,
                                     Minecraft minecraft, Font font) {
        if (!VillageDiplomacyConfig.INSTANCE.showDialogueBubbles.get()) return;

        DialogueBubbles.Bubble bubble = DialogueBubbles.get(entity.getId());
        if (bubble == null) return;

        long duration = Math.max(2, VillageDiplomacyConfig.INSTANCE.bubbleSeconds.get()) * 1000L;
        long age = System.currentTimeMillis() - bubble.spokenAt();
        if (age > duration) return;

        int alpha = (int) (255L * Math.min(1.0D, (duration - age) / 600.0D));
        alpha = Math.max(0, Math.min(255, alpha));
        if (alpha <= 4) return;

        List<FormattedCharSequence> lines = font.split(DialogueBubbles.styled(bubble.text()), 118);
        if (lines.isEmpty()) return;

        int textWidth = 0;
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int width = textWidth + 10;
        int height = lines.size() * 10 + 8;
        float left = -width / 2.0F;
        float top = 0.0F;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 1.05D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.024F, -0.024F, 0.024F);
        Matrix4f matrix = poseStack.last().pose();

        int accent = (alpha << 24) | (bubble.accent() & 0x00FFFFFF);
        int fill = (Math.min(alpha, 220) << 24) | 0x00101418;
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();

        // Border, body and a small tail pointing at the villager.
        quad(buffers, matrix, left - 1, top - 1, left + width + 1, top + height + 1, accent, light);
        quad(buffers, matrix, left, top, left + width, top + height, fill, light);
        quad(buffers, matrix, -4, top + height, 4, top + height + 5, accent, light);
        quad(buffers, matrix, -3, top + height, 3, top + height + 4, fill, light);

        int textAlpha = alpha << 24;
        int y = (int) top + 5;
        for (FormattedCharSequence line : lines) {
            int lineWidth = font.width(line);
            font.drawInBatch(line, -(lineWidth / 2.0F), y, textAlpha | 0x00FFFFFF, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, light);
            y += 10;
        }

        poseStack.popPose();
    }

    private static void quad(MultiBufferSource buffers, Matrix4f matrix,
                             float x0, float y0, float x1, float y1, int argb, int light) {
        float a = ((argb >>> 24) & 0xFF) / 255.0F;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        VertexConsumer consumer = buffers.getBuffer(RenderType.textBackground());
        consumer.vertex(matrix, x0, y1, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).uv2(light).endVertex();
        consumer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).uv2(light).endVertex();
        consumer.vertex(matrix, x1, y0, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).uv2(light).endVertex();
        consumer.vertex(matrix, x0, y0, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).uv2(light).endVertex();
    }

    private static Component villagerPlate(Villager villager, VillagerIdentityCache.Identity identity) {
        Component label;
        ChatFormatting labelColor;
        ChatFormatting nameColor;

        if (identity.innkeeper()) {
            label = Component.translatable("villagediplomacy.tag.innkeeper");
            labelColor = ChatFormatting.AQUA;
            nameColor = ChatFormatting.LIGHT_PURPLE;
        } else {
            label = ModLang.professionName(villager.getVillagerData().getProfession());
            labelColor = ModLang.professionColor(villager.getVillagerData().getProfession());
            nameColor = ModLang.nameColor(identity.uuid());
        }

        MutableComponent plate = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(label.copy().withStyle(labelColor))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));

        if (!identity.name().isEmpty()) {
            plate.append(Component.literal(identity.name()).withStyle(nameColor));
        }
        return plate;
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            VillagerIdentityCache.clear();
            DialogueBubbles.clear();
        }
    }

    private static boolean isEnabled() {
        return VillageDiplomacyConfig.SPEC.isLoaded()
                && VillageDiplomacyConfig.INSTANCE.enableNameplates.get();
    }
}
