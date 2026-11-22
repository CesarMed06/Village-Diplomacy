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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the mod's own nameplates and speech bubbles. There is no vanilla name
 * tag involved: the server syncs the identity it generated and this renderer
 * paints it above the entity with the mod's own colours, so future data (quest
 * markers, rank, emotion) can be added without fighting vanilla's label system.
 */
@Mod.EventBusSubscriber(modid = VillageDiplomacy.MODID, value = Dist.CLIENT)
public final class VillagerNameplateRenderer {

    private static final int BUBBLE_MAX_WIDTH = 112;
    private static final int BUBBLE_TEXT = 0xFF23262B;
    private static final int BUBBLE_FILL = 0xFFF7F3E6;
    private static final int BUBBLE_EDGE = 0xFF16181C;

    /** Wrapped layout, cached per line so it does not have to be measured each frame. */
    private record BubbleLayout(List<String> lines, int width, int chars) {
    }

    private static final Map<String, BubbleLayout> LAYOUTS = new HashMap<>();
    private static final Map<Integer, Long> LAST_SPEECH = new HashMap<>();
    private static final Map<Integer, Integer> LAST_REVEAL = new HashMap<>();
    private static final Map<Integer, Long> LAST_BLIP = new HashMap<>();

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

        int alpha = (int) (255L * Math.min(1.0D, (duration - age) / 700.0D));
        alpha = Math.max(0, Math.min(255, alpha));
        if (alpha <= 4) return;

        BubbleLayout layout = layout(font, bubble.text());
        if (layout.lines().isEmpty()) return;

        // Animal Crossing style: the line is typed out, not dropped in all at once.
        int revealed = (int) Math.min(layout.chars(), Math.max(0L, age / 26L));
        if (revealed <= 0) return;

        int width = layout.width() + 14;
        int height = layout.lines().size() * 10 + 10;
        float left = -width / 2.0F;
        float top = -height - 8.0F;
        int tailBase = (int) (top + height);
        int tailTip = tailBase + 8;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // Just above the plate, close enough to read as "this villager is talking".
        poseStack.translate(0.0D, entity.getBbHeight() + 0.98D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.024F, -0.024F, 0.024F);
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        int edge = (alpha << 24) | (BUBBLE_EDGE & 0x00FFFFFF);
        int fill = (Math.min(alpha, 242) << 24) | (BUBBLE_FILL & 0x00FFFFFF);
        int accent = (alpha << 24) | (bubble.accent() & 0x00FFFFFF);

        // Pop-in: the bubble grows a little during the first ticks of its life.
        float pop = Math.min(1.0F, age / 160.0F);
        float scale = 0.86F + 0.14F * pop;
        poseStack.scale(scale, scale, 1.0F);

        // Different depths per layer: drawing border and fill on the same plane
        // is what made the old box shimmer while the camera moved.
        roundedRect(buffers, matrix, left, top, left + width, top + height, 3, edge, 0.0F, light);
        roundedRect(buffers, matrix, left + 1, top + 1, left + width - 1, top + height - 1, 2,
                fill, 0.01F, light);

        // Tail: a dark triangle under the box with a lighter one inside.
        triangle(buffers, matrix, -5.5F, tailBase - 1, 5.5F, tailBase - 1, 0.0F, tailTip + 1,
                edge, 0.0F, light);
        triangle(buffers, matrix, -4.0F, tailBase - 1, 4.0F, tailBase - 1, 0.0F, tailTip - 1,
                fill, 0.008F, light);

        // A thin accent line keeps the speaker's colour visible without staining
        // the whole bubble.
        rect(buffers, matrix, left + 4, top + 1.5F, left + width - 4, top + 2.5F, accent, 0.02F, light);

        int textAlpha = alpha << 24;
        int textColor = textAlpha | (BUBBLE_TEXT & 0x00FFFFFF);
        int y = (int) top + 6;
        int remaining = revealed;
        boolean blipped = false;
        for (String line : layout.lines()) {
            if (remaining <= 0) break;
            int visible = Math.min(remaining, DialogueBubbles.plainLength(line));
            String shown = DialogueBubbles.reveal(line, visible);
            int lineWidth = font.width(DialogueBubbles.styled(shown));
            font.drawInBatch(DialogueBubbles.styled(shown), -lineWidth / 2.0F, y, textColor, false,
                    matrix, buffers, Font.DisplayMode.NORMAL, 0, light);

            if (!blipped && visible < DialogueBubbles.plainLength(line)) {
                blipped = true;
                playBlip(minecraft, entity.getId(), revealed);
            }
            remaining -= visible;
            y += 10;
        }

        poseStack.popPose();

        react(minecraft, entity, bubble, alpha);
    }

    private static void playBlip(Minecraft minecraft, int entityId, int revealed) {
        Integer previous = LAST_REVEAL.get(entityId);
        LAST_REVEAL.put(entityId, revealed);
        if (previous != null && revealed - previous < 3) return;

        long now = System.currentTimeMillis();
        Long last = LAST_BLIP.get(entityId);
        if (last != null && now - last < 55L) return;
        LAST_BLIP.put(entityId, now);

        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.85F, 0.16F));
    }

    /** Small coloured puff over the head so mood reads even from a distance. */
    private static void react(Minecraft minecraft, LivingEntity entity, DialogueBubbles.Bubble bubble,
                              int alpha) {
        if (minecraft.level == null) return;
        Long previous = LAST_SPEECH.get(entity.getId());
        if (previous != null && previous == bubble.spokenAt()) return;
        LAST_SPEECH.put(entity.getId(), bubble.spokenAt());
        if (alpha < 200) return;

        int red = (bubble.accent() >> 16) & 0xFF;
        int green = (bubble.accent() >> 8) & 0xFF;
        var particle = red > green + 30 ? ParticleTypes.ANGRY_VILLAGER
                : green > red + 30 ? ParticleTypes.HAPPY_VILLAGER
                : ParticleTypes.NOTE;
        for (int i = 0; i < 3; i++) {
            minecraft.level.addParticle(particle,
                    entity.getX() + (minecraft.level.random.nextDouble() - 0.5D) * 0.5D,
                    entity.getY() + entity.getBbHeight() + 0.35D,
                    entity.getZ() + (minecraft.level.random.nextDouble() - 0.5D) * 0.5D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    private static BubbleLayout layout(Font font, String text) {
        BubbleLayout cached = LAYOUTS.get(text);
        if (cached != null) return cached;

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && font.width(DialogueBubbles.styled(candidate)) > BUBBLE_MAX_WIDTH) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) lines.add(current.toString());

        int width = 0;
        int chars = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(DialogueBubbles.styled(line)));
            chars += DialogueBubbles.plainLength(line);
        }

        BubbleLayout layout = new BubbleLayout(List.copyOf(lines), width, chars);
        if (LAYOUTS.size() > 128) LAYOUTS.clear();
        LAYOUTS.put(text, layout);
        return layout;
    }

    private static void roundedRect(MultiBufferSource buffers, Matrix4f matrix,
                                    float x0, float y0, float x1, float y1, float radius,
                                    int argb, float z, int light) {
        float r = Math.max(0.0F, Math.min(radius, (x1 - x0) / 2.0F));
        rect(buffers, matrix, x0 + r, y0, x1 - r, y1, argb, z, light);
        rect(buffers, matrix, x0, y0 + r, x0 + r, y1 - r, argb, z, light);
        rect(buffers, matrix, x1 - r, y0 + r, x1, y1 - r, argb, z, light);
    }

    private static void rect(MultiBufferSource buffers, Matrix4f matrix,
                             float x0, float y0, float x1, float y1, int argb, float z, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.textBackground());
        put(consumer, matrix, x0, y1, z, argb, light);
        put(consumer, matrix, x1, y1, z, argb, light);
        put(consumer, matrix, x1, y0, z, argb, light);
        put(consumer, matrix, x0, y0, z, argb, light);
    }

    private static void triangle(MultiBufferSource buffers, Matrix4f matrix,
                                 float x0, float y0, float x1, float y1, float xTip, float yTip,
                                 int argb, float z, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.textBackground());
        put(consumer, matrix, x0, y0, z, argb, light);
        put(consumer, matrix, x1, y1, z, argb, light);
        put(consumer, matrix, xTip, yTip, z, argb, light);
    }

    private static void put(VertexConsumer consumer, Matrix4f matrix,
                            float x, float y, float z, int argb, int light) {
        float a = ((argb >>> 24) & 0xFF) / 255.0F;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(0.0F, 0.0F).uv2(light).endVertex();
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
            LAYOUTS.clear();
            LAST_SPEECH.clear();
            LAST_REVEAL.clear();
            LAST_BLIP.clear();
        }
    }

    private static boolean isEnabled() {
        return VillageDiplomacyConfig.SPEC.isLoaded()
                && VillageDiplomacyConfig.INSTANCE.enableNameplates.get();
    }
}
