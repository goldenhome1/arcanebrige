package com.example.arcanebridge.decipher;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DecryptionRegistry {

    public record DecryptionEntry(
            Item item,
            ResourceLocation advancementId,
            String successDialogueNode,
            String spellName
    ) {}

    private static final Map<Item, DecryptionEntry> REGISTRY = new HashMap<>();

    public static void register(Item item, ResourceLocation advancementId, String successDialogueNode, String spellName) {
        REGISTRY.put(item, new DecryptionEntry(item, advancementId, successDialogueNode, spellName));
    }

    public static Optional<DecryptionEntry> getEntry(Item item) {
        return Optional.ofNullable(REGISTRY.get(item));
    }

    public static Collection<DecryptionEntry> getAllEntries() {
        return REGISTRY.values();
    }
}