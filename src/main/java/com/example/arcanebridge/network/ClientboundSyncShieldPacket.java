package com.example.arcanebridge.network;

import com.example.arcanebridge.combat.MobArchetypes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSyncShieldPacket {

    private final int entityId;
    private final CompoundTag shieldData;

    public ClientboundSyncShieldPacket(int entityId, CompoundTag shieldData) {
        this.entityId = entityId;
        this.shieldData = shieldData;
    }

    public static void encode(ClientboundSyncShieldPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeNbt(msg.shieldData);
    }

    public static ClientboundSyncShieldPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundSyncShieldPacket(buffer.readInt(), buffer.readNbt());
    }

    public static void handle(ClientboundSyncShieldPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(msg.entityId);
                if (entity instanceof LivingEntity living && msg.shieldData != null) {
                    CompoundTag data = living.getPersistentData();
                    if (msg.shieldData.contains(MobArchetypes.NBT_SHIELD_LAYERS)) {
                        data.put(MobArchetypes.NBT_SHIELD_LAYERS, msg.shieldData.getList(MobArchetypes.NBT_SHIELD_LAYERS, 10));
                    }
                    data.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, msg.shieldData.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX));
                    data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, msg.shieldData.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}