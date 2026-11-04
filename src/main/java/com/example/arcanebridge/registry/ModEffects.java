package com.example.arcanebridge.registry;

import com.example.arcanebridge.ArcaneBridge;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ArcaneBridge.MODID);

    // 1. «Фазовый якорь»: поглощение Источника из банки Ars Nouveau. Отключает шлейф, нормализует ману.
    public static final RegistryObject<MobEffect> PHASE_ANCHOR = MOB_EFFECTS.register("phase_anchor",
            () -> new CustomStatusEffect(MobEffectCategory.BENEFICIAL, 0x22D3EE));

    // 2. «Кристаллическое насыщение»: медитация в жеоде аметиста. Усмиряет спонтанные щупальца, снижает перегрузку ударов на 75%.
    public static final RegistryObject<MobEffect> CRYSTALLINE_SATIATION = MOB_EFFECTS.register("crystalline_satiation",
            () -> new CustomStatusEffect(MobEffectCategory.BENEFICIAL, 0xC084FC));

    // 3. «Калиброванные сервоприводы»: продувка андезитовым сплавом. Снимает сбои WASD и заклинивание брони.
    public static final RegistryObject<MobEffect> CALIBRATED_SERVOS = MOB_EFFECTS.register("calibrated_servos",
            () -> new CustomStatusEffect(MobEffectCategory.BENEFICIAL, 0xFB923C));

    // 4. «Эфирный рассинхрон»: дебафф активного шлейфа. -15% скорости атаки, +40% заметность мобам, штраф маны.
    public static final RegistryObject<MobEffect> ETHEREAL_DRAG = MOB_EFFECTS.register("ethereal_drag",
            () -> new CustomStatusEffect(MobEffectCategory.HARMFUL, 0x818CF8)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, "c0b76e3d-82d2-4e3a-9694-8ec5bf124e41", -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // 5. «Кинетический форсаж»: суперспособность механики (Shift). Прирост скорости атаки мечом до х3 (по +50% за деление).
    public static final RegistryObject<MobEffect> KINETIC_OVERCLOCK = MOB_EFFECTS.register("kinetic_overclock",
            () -> new CustomStatusEffect(MobEffectCategory.BENEFICIAL, 0xF59E0B)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, "a5682894-3d91-4c12-8e4f-2dfa616781ab", 0.50D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    public static class CustomStatusEffect extends MobEffect {
        public CustomStatusEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
