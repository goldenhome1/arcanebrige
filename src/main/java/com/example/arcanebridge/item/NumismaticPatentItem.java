package com.example.arcanebridge.item;

import com.example.arcanebridge.data.ConsortiumSavedData;
import com.example.arcanebridge.entity.ConsortiumMerchantEntity;
import com.example.arcanebridge.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NumismaticPatentItem extends Item {

    public NumismaticPatentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (target.level() instanceof ServerLevel serverLevel) {
            ConsortiumSavedData data = ConsortiumSavedData.get(serverLevel);
            if (data.isMerchantActive(serverLevel)) {
                player.displayClientMessage(
                        Component.literal("§6[Консорциум] §cСнабженец уже ведет дела в этом мире по координатам "
                                + data.getPos().toShortString() + "!"),
                        true
                );
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.FAIL;
            }

            if (target instanceof Villager villager) {
                ConsortiumMerchantEntity merchant = ModEntities.CONSORTIUM_MERCHANT.get().create(serverLevel);
                if (merchant != null) {
                    merchant.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot());
                    merchant.yBodyRot = villager.yBodyRot;
                    merchant.yHeadRot = villager.yHeadRot;

                    villager.discard();
                    serverLevel.addFreshEntity(merchant);

                    data.registerMerchant(merchant.getUUID(), serverLevel.dimension(), merchant.blockPosition());

                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, merchant.getX(), merchant.getY() + 1.0, merchant.getZ(), 35, 0.3, 0.5, 0.3, 0.1);
                    serverLevel.playSound(null, merchant.getX(), merchant.getY(), merchant.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 1.0F, 1.0F);

                    player.displayClientMessage(
                            Component.literal("§6[Консорциум] §aПатент успешно заверен! Снабженец заступил на службу.").withStyle(ChatFormatting.GOLD),
                            false
                    );

                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            } else {
                player.displayClientMessage(
                        Component.literal("§6[Консорциум] §eПатент можно применить только к деревенскому жителю!"),
                        true
                );
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Официальный контракт Гильдии Нумизматов."));
        tooltip.add(Component.literal("§eПКМ по деревенскому жителю, чтобы нанять Снабженца."));
        tooltip.add(Component.literal("§cВнимание: В мире может существовать только один Снабженец!"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
