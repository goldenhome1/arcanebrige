package com.example.arcanebridge.capability;

public interface IResonance {
    float getStability();
    void setStability(float stability);
    void addStability(float amount);
    void sync(); // Метод для синхронизации с клиентом
}