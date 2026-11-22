package com.cesoti2006.villagediplomacy.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * A line of dialogue attached to an entity, so the client can draw it in a
 * speech bubble above that villager instead of (or as well as) the chat box.
 * The text already carries the mod's own colour codes.
 */
public class DialogueBubblePacket {

    private static final int MAX_LENGTH = 320;

    private final int entityId;
    private final String text;
    private final int accent;

    public DialogueBubblePacket(int entityId, String text, int accent) {
        this.entityId = entityId;
        this.text = text;
        this.accent = accent;
    }

    public DialogueBubblePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.text = buf.readUtf(MAX_LENGTH);
        this.accent = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(text, MAX_LENGTH);
        buf.writeInt(accent);
    }

    public static void handle(DialogueBubblePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.cesoti2006.villagediplomacy.client.DialogueBubbles.put(
                        packet.entityId, packet.text, packet.accent)));
        context.get().setPacketHandled(true);
    }
}
