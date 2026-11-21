package com.cesoti2006.villagediplomacy.client;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.util.ModLang;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        }
    }

    private static boolean isEnabled() {
        return VillageDiplomacyConfig.SPEC.isLoaded()
                && VillageDiplomacyConfig.INSTANCE.enableNameplates.get();
    }
}
