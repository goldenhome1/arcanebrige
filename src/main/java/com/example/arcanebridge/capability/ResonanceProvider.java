package com.example.arcanebridge.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class ResonanceProvider implements ICapabilitySerializable<Tag> {
    public static final Capability<IResonance> RESONANCE = CapabilityManager.get(new CapabilityToken<>(){});
    private final ResonanceCapability backend = new ResonanceCapability();
    private final LazyOptional<IResonance> lazyOptional = LazyOptional.of(() -> backend);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == RESONANCE ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public Tag serializeNBT() { 
        return backend.writeNBT(); 
    }

    @Override
    public void deserializeNBT(Tag nbt) { 
        backend.readNBT(nbt); 
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }
}