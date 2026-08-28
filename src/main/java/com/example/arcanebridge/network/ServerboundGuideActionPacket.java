package com.example.arcanebridge.network;

import com.example.arcanebridge.decipher.DecryptionRegistry;
import com.example.arcanebridge.entity.ArcaneGuideEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundGuideActionPacket {
    private final int entityId;
    private final String actionType;
    private final String targetId;

    public ServerboundGuideActionPacket(int entityId, String actionType, String targetId) {
        this.entityId = entityId;
        this.actionType = actionType;
        this.targetId = targetId;
    }

    public ServerboundGuideActionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.actionType = buf.readUtf();
        this.targetId = buf.readUtf();
    }

        private void handleDecryption(ServerPlayer player, ArcaneGuideEntity guide) {
        ServerLevel level = player.serverLevel();
        boolean foundAny = false;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            var entryOpt = DecryptionRegistry.getEntry(stack.getItem());
            if (entryOpt.isPresent()) {
                var entry = entryOpt.get();
                Advancement adv = level.getServer().getAdvancements().getAdvancement(entry.advancementId());

                if (adv != null) {
                    AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
                    if (progress.isDone()) {
                        player.sendSystemMessage(Component.literal("§6[Мастер Резонанса] §7Этот манускрипт (§e" + entry.spellName() + "§7) уже расшифрован и занесён в ваш блокнот!"));
                        continue;
                    }

                                        // Списываем 1 запечатанную скрижаль
                    stack.shrink(1);
                    for (String criterion : progress.getRemainingCriteria()) {
                        player.getAdvancements().award(adv, criterion);
                    }

                    // Выдаем расшифрованную скрижаль в инвентарь игроку (или дропаем рядом)
                    ItemStack decipheredStack = new ItemStack(entry.decipheredItem());
                    if (!player.getInventory().add(decipheredStack)) {
                        player.drop(decipheredStack, false);
                    }

                    // Анимация и спецэффекты гида
                    guide.setAnimState(ArcaneGuideEntity.STATE_EXPLAIN, 60);
                    level.sendParticles(ParticleTypes.ENCHANT, guide.getX(), guide.getY() + 1.2, guide.getZ(), 25, 0.4, 0.5, 0.4, 0.2);
                    level.sendParticles(ParticleTypes.END_ROD, guide.getX(), guide.getY() + 1.5, guide.getZ(), 10, 0.2, 0.3, 0.2, 0.05);

                    level.playSound(null, guide.getX(), guide.getY(), guide.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);
                    level.playSound(null, guide.getX(), guide.getY(), guide.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    player.sendSystemMessage(Component.literal("§6[Мастер Резонанса] §aСкрижаль расшифрована! Вы получили §e«" + entry.spellName() + "»§a."));
                    foundAny = true;
                    break;
                }
            }
        }

        if (!foundAny) {
            player.sendSystemMessage(Component.literal("§6[Мастер Резонанса] §cУ вас нет запечатанных манускриптов, требующих расшифровки."));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.actionType);
        buf.writeUtf(this.targetId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.level() != null) {
                Entity entity = player.level().getEntity(this.entityId);
                                if (entity instanceof ArcaneGuideEntity guide) {
                    if ("LOCATE".equals(this.actionType)) {
                        guide.performStructureScan(player, this.targetId);
                    } else if ("DECIPHER".equals(this.actionType)) {
                        handleDecryption(player, guide);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}