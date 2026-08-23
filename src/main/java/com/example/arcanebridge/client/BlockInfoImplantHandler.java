package com.example.arcanebridge.client;

import com.example.arcanebridge.logic.CyberwareHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class BlockInfoImplantHandler {

    private static boolean callbacksInjected = false;
    private static Boolean lastAccessState = null;

    private static final Set<String> ALLOWED_ITEMS = Set.of(
            "create:goggles",
            "create:infernal_goggles",
            "cyber_ware_port:hud_lens",
            "cyber_ware_port:hud_jack"
    );

    private static final Set<String> HUD_IMPLANTS = Set.of(
            "cyber_ware_port:cybereyes",
            "cyber_ware_port:cybereye_upgrades_hudjack",
            "cyber_ware_port:hud_jack",
            "cyber_ware_port:hud_lens"
    );

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        // 1. Однократная инъекция колбэков в ядро Jade
        if (!callbacksInjected) {
            injectJadeCallbacks();
        }

        boolean hasAccess = hasBlockInfoAccess(player);

        // 2. Мгновенная реакция при смене статуса (надели очки / клик в [H])
        if (lastAccessState == null || lastAccessState != hasAccess) {
            lastAccessState = hasAccess;
            syncJadeGeneralConfig(hasAccess);
        }

        // 3. Если доступа нет — принудительно обнуляем буферы и альфу каждый тик
        if (!hasAccess) {
            suppressJadeRenderer();
        }
    }

    /**
     * Инъекция прокси-слушателей в ядро Jade
     */
    private static void injectJadeCallbacks() {
        try {
            Class<?> regClass = Class.forName("snownee.jade.impl.WailaClientRegistration");
            Field instField = regClass.getDeclaredField("INSTANCE");
            instField.setAccessible(true);
            Object regInstance = instField.get(null);
            if (regInstance == null) return;

            ClassLoader cl = BlockInfoImplantHandler.class.getClassLoader();

            // А. Перехватчик BeforeRender: блокирует отрисовку перед вызовом матриц рендера
            Class<?> beforeRenderClass = Class.forName("snownee.jade.api.callback.JadeBeforeRenderCallback");
            Object beforeRenderProxy = Proxy.newProxyInstance(
                    cl,
                    new Class<?>[]{beforeRenderClass},
                    (proxy, method, args) -> {
                        if ("beforeRender".equals(method.getName())) {
                            Player player = Minecraft.getInstance().player;
                            if (!hasBlockInfoAccess(player)) {
                                return true; // true = прервать рендер
                            }
                            return false;
                        }
                        if ("hashCode".equals(method.getName())) return 101;
                        if ("equals".equals(method.getName())) return proxy == args[0];
                        return null;
                    }
            );
            injectIntoContainer(regInstance, "beforeRenderCallback", beforeRenderProxy);

            // Б. Перехватчик RayTrace: обнуляет цель трассировки лучей
            Class<?> rayTraceClass = Class.forName("snownee.jade.api.callback.JadeRayTraceCallback");
            Object rayTraceProxy = Proxy.newProxyInstance(
                    cl,
                    new Class<?>[]{rayTraceClass},
                    (proxy, method, args) -> {
                        if ("onRayTrace".equals(method.getName())) {
                            Player player = Minecraft.getInstance().player;
                            if (!hasBlockInfoAccess(player)) {
                                return null; // null = сбросить цель и остановить тик Jade
                            }
                            return args[1]; // вернуть текущий accessor
                        }
                        if ("hashCode".equals(method.getName())) return 102;
                        if ("equals".equals(method.getName())) return proxy == args[0];
                        return null;
                    }
            );
            injectIntoContainer(regInstance, "rayTraceCallback", rayTraceProxy);

            callbacksInjected = true;
        } catch (ClassNotFoundException e) {
            callbacksInjected = true;
        } catch (Throwable t) {
            callbacksInjected = true;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void injectIntoContainer(Object regInstance, String containerFieldName, Object proxy) {
        try {
            Field containerField = regInstance.getClass().getDeclaredField(containerFieldName);
            containerField.setAccessible(true);
            Object container = containerField.get(regInstance);
            if (container == null) return;

            // Внедряемся в приватный внутренний список CallbackContainer на 0-й индекс
            for (Field f : container.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(container);
                if (val instanceof List list) {
                    if (!list.contains(proxy)) {
                        list.add(0, proxy);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Прямое переключение опции «Отобразить подсказку» в конфигурации Jade
     */
    private static void syncJadeGeneralConfig(boolean enable) {
        // Метод 1: Через публичный IWailaConfig.get().getGeneral()
        try {
            Class<?> iWailaClass = Class.forName("snownee.jade.api.config.IWailaConfig");
            Method getMethod = iWailaClass.getMethod("get");
            Object config = getMethod.invoke(null);
            if (config != null) {
                Method getGen = config.getClass().getMethod("getGeneral");
                Object general = getGen.invoke(config);
                if (general != null) {
                    Method setDisplay = general.getClass().getMethod("setDisplayTooltip", boolean.class);
                    setDisplay.invoke(general, enable);
                }
            }
        } catch (Throwable ignored) {}

        // Метод 2: Через статическое поле Jade.CONFIG
        try {
            Class<?> jadeClass = Class.forName("snownee.jade.Jade");
            Field configField = jadeClass.getDeclaredField("CONFIG");
            configField.setAccessible(true);
            Object jsonConfig = configField.get(null);
            if (jsonConfig != null) {
                Method getMethod = jsonConfig.getClass().getMethod("get");
                Object wailaConfig = getMethod.invoke(jsonConfig);
                if (wailaConfig != null) {
                    Method getGen = wailaConfig.getClass().getMethod("getGeneral");
                    Object general = getGen.invoke(wailaConfig);
                    if (general != null) {
                        Method setDisplay = general.getClass().getMethod("setDisplayTooltip", boolean.class);
                        setDisplay.invoke(general, enable);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Принудительное гашение всех буферов рендерера Jade
     */
    private static void suppressJadeRenderer() {
        // 1. Сброс tooltipRenderer в WailaTickHandler
        try {
            Class<?> tickHandlerClass = Class.forName("snownee.jade.overlay.WailaTickHandler");
            Method instanceMethod = tickHandlerClass.getMethod("instance");
            Object tickHandler = instanceMethod.invoke(null);
            if (tickHandler != null) {
                Field trField = tickHandlerClass.getDeclaredField("tooltipRenderer");
                trField.setAccessible(true);
                trField.set(tickHandler, null);
            }
        } catch (Throwable ignored) {}

        // 2. Сброс альфы, шлейфа и геометрии в OverlayRenderer
        try {
            Class<?> overlayClass = Class.forName("snownee.jade.overlay.OverlayRenderer");

            Field alphaField = overlayClass.getDeclaredField("alpha");
            alphaField.setAccessible(true);
            alphaField.setFloat(null, 0.0F);

            Field shownField = overlayClass.getDeclaredField("shown");
            shownField.setAccessible(true);
            shownField.setBoolean(null, false);

            Field lingerField = overlayClass.getDeclaredField("lingerTooltip");
            lingerField.setAccessible(true);
            lingerField.set(null, null);

            Field morphField = overlayClass.getDeclaredField("morphRect");
            morphField.setAccessible(true);
            morphField.set(null, null);
        } catch (Throwable ignored) {}
    }

    /**
     * Проверка допуска: Create Goggles, Curios или Cyberware HUD Jack + тумблер в [H]
     */
    public static boolean hasBlockInfoAccess(Player player) {
        if (player == null || !HudConfig.showBlockInfo) return false;

        // 1. Ванильный слот шлема
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (ALLOWED_ITEMS.contains(getItemId(headStack.getItem()))) return true;

        // 2. Слоты Curios
        boolean hasCurio = CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(stack -> ALLOWED_ITEMS.contains(getItemId(stack.getItem()))).isPresent())
                .orElse(false);

        if (hasCurio) return true;

        // 3. Импланты Cyberware (HUD Jack / Cybereyes / HUD Lens)
        for (String implantId : HUD_IMPLANTS) {
            if (CyberwareHelper.hasCyberware(player, implantId)) {
                return true;
            }
        }

        return false;
    }

    private static String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}