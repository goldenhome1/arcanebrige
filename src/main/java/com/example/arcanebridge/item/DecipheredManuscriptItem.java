package com.example.arcanebridge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DecipheredManuscriptItem extends Item {

    private final ResourceLocation advancementId;
    private final String spellName;

    public DecipheredManuscriptItem(Properties properties, ResourceLocation advancementId, String spellName) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.advancementId = advancementId;
        this.spellName = spellName;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Advancement adv = serverPlayer.server.getAdvancements().getAdvancement(this.advancementId);
            if (adv != null) {
                AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(adv);
                if (!progress.isDone()) {
                    for (String criterion : progress.getRemainingCriteria()) {
                        serverPlayer.getAdvancements().award(adv, criterion);
                    }

                    ServerLevel serverLevel = (ServerLevel) level;
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.4, 0.5, 0.4, 0.15);
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);

                    player.sendSystemMessage(Component.literal("§6[Древнее Знание] §aВы изучили скрижаль: §e«" + this.spellName + "» §aдобавлено в Рунный Блокнот!"));
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                } else {
                    player.sendSystemMessage(Component.literal("§7Это знание (§e" + this.spellName + "§7) уже записано в вашем Рунном Блокноте."));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

        public record DecipheredLore(String note, String hexBranch) {}

    private static final java.util.Map<String, DecipheredLore> DECIPHERED_LORE = java.util.Map.of(
            "spells/phase_kinetics", new DecipheredLore(
                    "§6Символы кинетического резонанса переведены на язык Hex Casting.",
                    "§8[Вектор вращения: Источник / Приёмник]"
            ),
            "spells/phase_fluidics", new DecipheredLore(
                    "§bСимволы гидродинамического резонанса переведены на язык Hex Casting.",
                    "§8[Вектор потока: Исток / Приёмник]"
            )
    );

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.literal("§7Древняя шестиугольная сланцевая плита."));

        DecipheredLore lore = DECIPHERED_LORE.get(this.advancementId.getPath());
        if (lore != null) {
            tooltip.add(Component.literal(lore.note()));
            tooltip.add(Component.literal(lore.hexBranch()));
        } else {
            tooltip.add(Component.literal("§aСимволы расшифрованы и переведены на язык Hex Casting."));
        }

        tooltip.add(Component.literal("§e• Нажмите ПКМ, чтобы переписать формулу в Рунный Блокнот.").withStyle(ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}