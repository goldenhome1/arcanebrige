package com.example.arcanebridge.fluid;

import com.example.arcanebridge.network.ClientboundPhaseFluidSyncPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhaseFluidCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    private int channelId = 0;
    private final BlockEntity blockEntity;
    private LazyOptional<IFluidHandler> holder = LazyOptional.empty();

    public PhaseFluidCapabilityProvider(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public void setChannel(int channel) {
        this.channelId = channel;
        this.holder.invalidate();
        this.holder = LazyOptional.of(() -> {
            Level level = blockEntity != null ? blockEntity.getLevel() : null;
            if (level != null && !level.isClientSide()) {
                PhaseFluidSavedData data = PhaseFluidSavedData.getInstance();
                return data != null ? data.getOrCreateChannel(channelId) : new FluidTank(16000);
            } else {
                return ClientboundPhaseFluidSyncPacket.CLIENT_CHANNELS.computeIfAbsent(channelId, id -> new FluidTank(16000));
            }
        });
    }

    public int getChannelId() {
        return channelId;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && channelId > 0) {
            if (!holder.isPresent()) {
                setChannel(this.channelId);
            }
            return holder.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("PhaseFluidChannel", channelId);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.channelId = nbt.getInt("PhaseFluidChannel");
        if (this.channelId > 0) {
            setChannel(this.channelId);
        }
    }
}