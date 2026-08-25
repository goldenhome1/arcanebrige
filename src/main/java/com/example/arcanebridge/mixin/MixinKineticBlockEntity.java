package com.example.arcanebridge.mixin;

import com.example.arcanebridge.hex.network.PhaseNetworkManager;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class MixinKineticBlockEntity extends BlockEntity {

    public MixinKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Вал-приемник признается генератором внутри графа Create
     */
    @Inject(method = "isSource", at = @At("HEAD"), cancellable = true)
    private void arcaneBridge$isSource(CallbackInfoReturnable<Boolean> cir) {
        if (this.level != null && !this.level.isClientSide) {
            MinecraftServer server = this.level.getServer();
            if (server != null) {
                PhaseNetworkManager manager = PhaseNetworkManager.get(server);
                PhaseNetworkManager.PhaseChannel channel = manager.getChannelByReceiver(this.level.dimension(), this.worldPosition);
                if (channel != null && Math.abs(channel.currentSpeed) > 0.01f) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /**
     * Вал-передатчик нагружает свою сеть стрессом от удаленных станков
     */
    @Inject(method = "calculateStressApplied", at = @At("HEAD"), cancellable = true)
    private void arcaneBridge$calculateStressApplied(CallbackInfoReturnable<Float> cir) {
        if (this.level != null && !this.level.isClientSide) {
            MinecraftServer server = this.level.getServer();
            if (server != null) {
                PhaseNetworkManager manager = PhaseNetworkManager.get(server);
                PhaseNetworkManager.PhaseChannel channel = manager.getChannelByTransmitter(this.level.dimension(), this.worldPosition);
                if (channel != null && channel.rxStress > 0.0f) {
                    cir.setReturnValue(channel.rxStress);
                }
            }
        }
    }
}