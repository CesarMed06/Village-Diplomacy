package com.cesoti2006.villagediplomacy.network;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sends the identity the mod gives a villager (name, mood, innkeeper status) to
 * the client so its own nameplates can show it instead of vanilla name tags.
 */
public class SyncVillagerIdentityPacket {

    private final int entityId;
    private final UUID uuid;
    private final String name;
    private final String mood;
    private final boolean innkeeper;

    public SyncVillagerIdentityPacket(int entityId, UUID uuid, String name, String mood, boolean innkeeper) {
        this.entityId = entityId;
        this.uuid = uuid;
        this.name = name;
        this.mood = mood;
        this.innkeeper = innkeeper;
    }

    public SyncVillagerIdentityPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.name = buf.readUtf(64);
        this.mood = buf.readUtf(32);
        this.innkeeper = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUUID(uuid);
        buf.writeUtf(name, 64);
        buf.writeUtf(mood, 32);
        buf.writeBoolean(innkeeper);
    }

    public static void handle(SyncVillagerIdentityPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.cesoti2006.villagediplomacy.client.VillagerIdentityCache.put(
                        packet.entityId, packet.uuid, packet.name, packet.mood, packet.innkeeper)));
        context.get().setPacketHandled(true);
    }
}
