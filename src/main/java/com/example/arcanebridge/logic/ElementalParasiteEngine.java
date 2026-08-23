package com.example.arcanebridge.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ElementalParasiteEngine {
    private static final Random random = new Random();

    public static boolean isElementalActive(Player player) {
        // Активен строго при перегрузке частоты Elemental (флаг или стабильность <= 75%)
        return player.getPersistentData().getBoolean("ArcaneEleOverload") && 
               player.getPersistentData().getFloat("ArcaneStability") <= 75.0f;
    }

    public static void triggerParasite(Player player, String message) {
        if (player.level().isClientSide) return;
        ServerLevel level = (ServerLevel) player.level();

        player.sendSystemMessage(Component.literal("§d[Эфирный паразит]: §7" + message));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 0.7f, 1.7f);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
    }

    @SubscribeEvent
    public static void onEatFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isElementalActive(player)) return;

        ItemStack item = event.getItem();
        if (item.isEdible() && random.nextInt(100) < 60) {
            String[] foodQuotes = {
                    "А что это ты такое жуешь? Мог бы и аметист сгрызть, для проводимости.",
                    "Опять органика? Твои энергетические каналы от этого слипнутся!",
                    "Ты серьезно тратишь время на еду, пока вокруг рассеивается эфир?"
            };
            triggerParasite(player, foodQuotes[random.nextInt(foodQuotes.length)]);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || !isElementalActive(player)) return;

        if (random.nextInt(100) < 30) {
            String[] breakQuotes = {
                    "Зачем ты сломал этот блок? Он тут триста лет спокойно лежал!",
                    "Отличный удар. А энтропию в пространстве за собой кто убирать будет?",
                    "Минус один блок в структуре мира. Слышишь, как реальность трещит?",
                    "Эй! Этот блок идеально гармонировал с моей аурой!"
            };
            triggerParasite(player, breakQuotes[random.nextInt(breakQuotes.length)]);
        }
    }

    @SubscribeEvent
    public static void onInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player == null || !isElementalActive(player)) return;

        String blockId = event.getLevel().getBlockState(event.getPos()).getBlock().getDescriptionId();
        if ((blockId.contains("button") || blockId.contains("lever")) && random.nextInt(100) < 50) {
            String[] clickQuotes = {
                    "Ты точно уверен, что нажал правильную кнопку?",
                    "Щелк! И ничего не произошло... Или мир только что надломился?",
                    "Я бы на твоем месте не дергал этот переключатель."
            };
            triggerParasite(player, clickQuotes[random.nextInt(clickQuotes.length)]);
        }
    }
}