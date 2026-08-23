package com.example.arcanebridge.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "getItemBySlot", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("removal")
    private void arcane$getItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        Player player = (Player) (Object) this;

        String curioSlot = null;
        if (slot == EquipmentSlot.HEAD) curioSlot = "arcane_helmet";
        else if (slot == EquipmentSlot.CHEST) curioSlot = "arcane_chestplate";
        else if (slot == EquipmentSlot.LEGS) curioSlot = "arcane_leggings";
        else if (slot == EquipmentSlot.FEET) curioSlot = "arcane_boots";

        if (curioSlot == null) return;

        ItemStack vanillaStack = cir.getReturnValue();
        String finalCurioSlot = curioSlot;

        final ItemStack[] curioStackRef = {ItemStack.EMPTY};
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getStacksHandler(finalCurioSlot).ifPresent(stacksHandler -> {
                if (stacksHandler.getStacks().getSlots() > 0) {
                    curioStackRef[0] = stacksHandler.getStacks().getStackInSlot(0);
                }
            });
        });
        ItemStack curioStack = curioStackRef[0];

        if (curioStack.isEmpty()) return;
        if (vanillaStack.isEmpty()) {
            if (arcane$isAuthorizedCaller()) {
                cir.setReturnValue(curioStack);
            }
            return;
        }

        if (arcane$isAuthorizedCaller()) {
            String context = arcane$getAuthorizedCallerContext();
            if (context != null) {
                String lowercaseContext = context.toLowerCase();
                String curioItemId = curioStack.getItem().toString().toLowerCase();
                String vanillaItemId = vanillaStack.getItem().toString().toLowerCase();

                boolean curioMatches = arcane$contextMatchesItem(lowercaseContext, curioItemId);
                boolean vanillaMatches = arcane$contextMatchesItem(lowercaseContext, vanillaItemId);

                if (curioMatches && !vanillaMatches) {
                    cir.setReturnValue(curioStack);
                    return;
                }
                if (vanillaMatches && !curioMatches) {
                    cir.setReturnValue(vanillaStack);
                    return;
                }
            }
            cir.setReturnValue(curioStack);
        }
    }

    @Inject(method = "getArmorSlots", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("removal")
    private void arcane$getArmorSlots(CallbackInfoReturnable<Iterable<ItemStack>> cir) {
        Player player = (Player) (Object) this;

        if (arcane$isAuthorizedCaller()) {
            Iterable<ItemStack> vanillaArmor = cir.getReturnValue();
            List<ItemStack> combinedList = new ArrayList<>();

            vanillaArmor.forEach(combinedList::add);

            String[] orderedSlots = {"arcane_boots", "arcane_leggings", "arcane_chestplate", "arcane_helmet"};
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                for (String slotName : orderedSlots) {
                    handler.getStacksHandler(slotName).ifPresent(stacksHandler -> {
                        if (stacksHandler.getStacks().getSlots() > 0) {
                            ItemStack stack = stacksHandler.getStacks().getStackInSlot(0);
                            if (!stack.isEmpty()) {
                                combinedList.add(stack);
                            }
                        }
                    });
                }
            });
            cir.setReturnValue(combinedList);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    @SuppressWarnings("removal")
    private void arcane$tickEtherealArmor(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        Level level = player.level();
        if (level.isClientSide()) return;

        String[] slots = {"arcane_helmet", "arcane_chestplate", "arcane_leggings", "arcane_boots"};
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotName : slots) {
                handler.getStacksHandler(slotName).ifPresent(stacksHandler -> {
                    if (stacksHandler.getStacks().getSlots() > 0) {
                        ItemStack curioStack = stacksHandler.getStacks().getStackInSlot(0);
                        if (!curioStack.isEmpty()) {
                            curioStack.getItem().onArmorTick(curioStack, level, player);
                        }
                    }
                });
            }
        });
    }

    private static String arcane$getAuthorizedCallerContext() {
        return StackWalker.getInstance().walk(stream -> {
            StringBuilder sb = new StringBuilder();
            stream.forEach(frame -> {
                String className = frame.getClassName().toLowerCase();
                if (className.contains("ars_nouveau") || className.contains("ars_elemental") || className.contains("simibubi.create")) {
                    sb.append(frame.getClassName()).append(".").append(frame.getMethodName()).append(" ");
                }
            });
            return sb.length() > 0 ? sb.toString() : null;
        });
    }

    private static boolean arcane$contextMatchesItem(String context, String itemId) {
        if (itemId.contains("cardboard")) {
            return context.contains("create") || context.contains("cardboard") || context.contains("sneak") || context.contains("crouch");
        }
        if (itemId.contains("fire") || itemId.contains("pyro")) {
            return context.contains("fire") || context.contains("pyro") || context.contains("damage") || context.contains("burn") || context.contains("attack");
        }
        if (itemId.contains("air") || itemId.contains("aero")) {
            return context.contains("air") || context.contains("aero") || context.contains("flurry") || context.contains("fall") || context.contains("wind") || context.contains("glide") || context.contains("jump");
        }
        if (itemId.contains("water") || itemId.contains("aqua")) {
            return context.contains("water") || context.contains("aqua") || context.contains("freeze") || context.contains("swim") || context.contains("bubble") || context.contains("tide");
        }
        if (itemId.contains("earth") || itemId.contains("geo")) {
            return context.contains("earth") || context.contains("geo") || context.contains("stone") || context.contains("titan") || context.contains("knockback");
        }

        if (itemId.contains("sorcerer")) return context.contains("sorcerer") || context.contains("novice");
        if (itemId.contains("arcanist")) return context.contains("arcanist") || context.contains("apprentice");
        if (itemId.contains("battlemage")) return context.contains("battlemage") || context.contains("master");

        return false;
    }

    private static boolean arcane$isAuthorizedCaller() {
        return StackWalker.getInstance().walk(stream ->
                stream.anyMatch(frame -> {
                    String className = frame.getClassName().toLowerCase();
                    return className.contains("ars_nouveau") ||
                            className.contains("ars_elemental") ||
                            className.contains("simibubi.create") ||
                            className.contains("arcanebridge");
                })
        );
    }
}
