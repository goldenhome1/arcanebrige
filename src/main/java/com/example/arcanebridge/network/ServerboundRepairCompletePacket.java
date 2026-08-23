package com.example.arcanebridge.network;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ServerboundRepairCompletePacket {

    public ServerboundRepairCompletePacket() {}

    public ServerboundRepairCompletePacket(FriendlyByteBuf buf) {}

    public static void encode(ServerboundRepairCompletePacket msg, FriendlyByteBuf buf) {}

    public static ServerboundRepairCompletePacket decode(FriendlyByteBuf buf) {
        return new ServerboundRepairCompletePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            ResourceLocation mainItemId = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
            boolean isWrench = mainItemId != null && mainItemId.toString().equals("create:wrench");
            boolean isJammed = offHand.hasTag() && offHand.getTag().getBoolean("Jammed");

            if (isWrench && isJammed) {
                CompoundTag tag = offHand.getTag();
                if (tag != null) {
                    tag.remove("Jammed");
                    if (tag.isEmpty()) offHand.setTag(null);
                }

                ServerLevel level = player.serverLevel();
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.4, 0.4, 0.15);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.6f);

                player.displayClientMessage(Component.literal("§a§l✔ [КАЛИБРОВКА ЗАВЕРШЕНА] §fСервоприводы успешно разблокированы!"), true);
            }
        });
        context.setPacketHandled(true);
    }
}