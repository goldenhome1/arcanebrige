package com.example.arcanebridge.item;

import com.example.arcanebridge.entity.ArcaneGuideEntity;
import com.example.arcanebridge.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GuideCoreItem extends Item {

    public GuideCoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(face);

        if (level instanceof ServerLevel serverLevel) {
            ArcaneGuideEntity guide = ModEntities.ARCANE_GUIDE.get().create(serverLevel);
            if (guide != null) {
                double x = spawnPos.getX() + 0.5D;
                double y = spawnPos.getY();
                double z = spawnPos.getZ() + 0.5D;

                // Поворачиваем проекцию лицом к игроку
                Vec3 lookDir = context.getPlayer() != null ? context.getPlayer().position().subtract(x, y, z) : new Vec3(0, 0, 1);
                float yaw = (float) (net.minecraft.util.Mth.atan2(lookDir.z, lookDir.x) * (180.0D / Math.PI)) - 90.0F;

                guide.moveTo(x, y, z, yaw, 0.0F);
                guide.setYHeadRot(yaw);
                guide.setYBodyRot(yaw);
                
                                                // Запуск послойной материализации снизу вверх на 30 тиков
                guide.setAnimState(ArcaneGuideEntity.STATE_MATERIALIZE, 30);
                serverLevel.addFreshEntity(guide);

                // Эффекты голографической проекции и глитч-частицы
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 0.9, z, 20, 0.25, 0.4, 0.25, 0.05);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y + 1.2, z, 15, 0.3, 0.4, 0.3, 0.2);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.5, z, 8, 0.2, 0.3, 0.2, 0.1);

                serverLevel.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                serverLevel.playSound(null, spawnPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.2F);

                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }
}