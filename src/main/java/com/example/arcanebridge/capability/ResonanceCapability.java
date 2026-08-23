package com.example.arcanebridge.capability;

import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

public class ResonanceCapability implements IResonance {
    private float stability = 100.0f;

    @Override
    public float getStability() { return stability; }

    @Override
    public void setStability(float stability) { 
        this.stability = Math.max(0, Math.min(100, stability)); 
    }

    @Override
    public void addStability(float amount) { 
        setStability(stability + amount); 
    }

    @Override
    public void sync() { 
        /* Сетевая синхронизация будет подключена через NetworkHandler */ 
    }

    public Tag writeNBT() { 
        return FloatTag.valueOf(stability); 
    }

    public void readNBT(Tag tag) { 
        if (tag instanceof NumericTag numericTag) {
            this.stability = numericTag.getAsFloat(); 
        }
    }
}