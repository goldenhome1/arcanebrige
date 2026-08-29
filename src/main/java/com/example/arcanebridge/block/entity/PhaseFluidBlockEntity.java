package com.example.arcanebridge.block.entity;

import com.example.arcanebridge.fluid.PhaseFluidSavedData;
import com.example.arcanebridge.network.ClientboundPhaseFluidSyncPacket;
import com.example.arcanebridge.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PhaseFluidBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public boolean isReceiver = false;
    private int channelId = 1;
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.empty();

    public PhaseFluidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASE_FLUID_RELAY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public void tune(int channel, boolean receiver) {
        this.channelId = channel;
        this.isReceiver = receiver;
        this.fluidCapability.invalidate();
        this.fluidCapability = LazyOptional.empty();
        setChanged();
        sendData();
    }

    public void setChannel(int channel) {
        tune(channel, this.isReceiver);
    }

    public void toggleMode() {
        tune(this.channelId, !this.isReceiver);
    }

    public int getChannel() {
        return channelId;
    }

    public IFluidHandler getFluidStorage() {
        if (level != null && !level.isClientSide()) {
            PhaseFluidSavedData data = PhaseFluidSavedData.getInstance();
            return data != null ? data.getOrCreateChannel(channelId) : new FluidTank(16000);
        } else {
            return ClientboundPhaseFluidSyncPacket.CLIENT_CHANNELS.computeIfAbsent(channelId, id -> new FluidTank(16000));
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (!fluidCapability.isPresent()) {
                fluidCapability = LazyOptional.of(this::getFluidStorage);
            }
            return fluidCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    §bФазовый Гидро-Резонатор:"));
        tooltip.add(Component.literal("  §7Канал: §e#" + channelId));
        tooltip.add(Component.literal("  §7Режим: " + (this.isReceiver ? "§9Приёмник (RX)" : "§3Источник (TX)")));

        IFluidHandler handler = getFluidStorage();
        FluidStack fluid = handler.getFluidInTank(0);
        if (fluid.isEmpty()) {
            tooltip.add(Component.literal("  §7Содержимое: §8Пусто (0 / " + handler.getTankCapacity(0) + " mB)"));
        } else {
            tooltip.add(Component.literal("  §7Жидкость: §f" + fluid.getDisplayName().getString()));
            tooltip.add(Component.literal("  §7Объём: §b" + fluid.getAmount() + " §7/ §b" + handler.getTankCapacity(0) + " mB"));
        }
        return true;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("ChannelId", this.channelId);
        tag.putBoolean("IsReceiver", this.isReceiver);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        this.channelId = tag.getInt("ChannelId");
        this.isReceiver = tag.getBoolean("IsReceiver");
    }
}