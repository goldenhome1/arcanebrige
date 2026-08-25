package com.example.arcanebridge.mixin;

import com.example.arcanebridge.hex.network.PhaseNetworkManager;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KineticBlockEntity.class)
public abstract class MixinKineticBlockEntity {

    /**
     * 1. Вал-приемник объявляет Create скорость генерации (RPM)
     */
    @Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void arcaneBridge$getGeneratedSpeed(CallbackInfoReturnable<Float> cir) {
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            PhaseNetworkManager manager = PhaseNetworkManager.get(level.getServer());
            PhaseNetworkManager.PhaseChannel channel = manager.getChannelByReceiver(level.dimension(), be.getBlockPos());
            if (channel != null && Math.abs(channel.currentSpeed) > 0.01f) {
                cir.setReturnValue(channel.currentSpeed);
            }
        }
    }

    /**
     * 2. Вал-приемник выделяет доступную мощность (Stress Capacity) для сети
     */
    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void arcaneBridge$calculateAddedStressCapacity(CallbackInfoReturnable<Float> cir) {
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            PhaseNetworkManager manager = PhaseNetworkManager.get(level.getServer());
            PhaseNetworkManager.PhaseChannel channel = manager.getChannelByReceiver(level.dimension(), be.getBlockPos());
            if (channel != null && Math.abs(channel.currentSpeed) > 0.01f) {
                cir.setReturnValue(channel.txCapacity > 0 ? channel.txCapacity : 102400.0f);
            }
        }
    }

    /**
     * 3. Вал-передатчик забирает нагрузку удаленной сети станков
     */
    @Inject(method = "calculateStressApplied", at = @At("HEAD"), cancellable = true, remap = false)
    private void arcaneBridge$calculateStressApplied(CallbackInfoReturnable<Float> cir) {
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            PhaseNetworkManager manager = PhaseNetworkManager.get(level.getServer());
            PhaseNetworkManager.PhaseChannel channel = manager.getChannelByTransmitter(level.dimension(), be.getBlockPos());
            if (channel != null && channel.rxStress > 0.0f) {
                cir.setReturnValue(channel.rxStress);
            }
        }
    }
}