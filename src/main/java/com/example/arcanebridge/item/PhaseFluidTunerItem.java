package com.example.arcanebridge.item;

import com.example.arcanebridge.block.entity.PhaseFluidBlockEntity;
import com.example.arcanebridge.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PhaseFluidTunerItem extends Item {

    public PhaseFluidTunerItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int currentChannel = stack.getOrCreateTag().getInt("SelectedChannel");
            currentChannel = (currentChannel % 10) + 1;
            stack.getTag().putInt("SelectedChannel", currentChannel);

            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("§b[Калибратор] §7Выбран тестовый канал: §e#" + currentChannel), true);
                player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        int channel = stack.getOrCreateTag().getInt("SelectedChannel");
        if (channel <= 0) channel = 1;

        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof PhaseFluidBlockEntity)) {
            level.setBlock(pos, ModBlocks.PHASE_FLUID_RELAY.get().defaultBlockState(), Block.UPDATE_ALL);
            be = level.getBlockEntity(pos);
        }

        if (be instanceof PhaseFluidBlockEntity fluidNode) {
            if (!level.isClientSide() && player != null) {
                if (player.isShiftKeyDown()) {
                    fluidNode.toggleMode();
                    String modeName = fluidNode.isReceiver ? "§9Приёмник (RX)" : "§3Источник (TX)";
                    player.sendSystemMessage(Component.literal("§b[Фазовая Гидравлика] §7Режим переключен: " + modeName));
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, 1.4F);
                } else {
                    fluidNode.setChannel(channel);
                    player.sendSystemMessage(Component.literal("§b[Фазовая Гидравлика] §aГидроузел настроен на канал §e#" + channel));
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.8F);
                }

                ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        int channel = stack.getOrCreateTag().getInt("SelectedChannel");
        if (channel <= 0) channel = 1;
        tooltip.add(Component.literal("§7Тестовый инструмент настройки фазовых жидкостей."));
        tooltip.add(Component.literal("§bАктивный канал: §e#" + channel));
        tooltip.add(Component.literal("§e• ПКМ по блоку: §7настроить канал.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§e• Shift + ПКМ по блоку: §7сменить режим TX/RX.").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§e• Shift + ПКМ в воздух: §7сменить номер канала.").withStyle(ChatFormatting.ITALIC));
    }
}