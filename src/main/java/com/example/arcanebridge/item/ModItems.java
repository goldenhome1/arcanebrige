// Добавить в список регистрации предметов:
public static final RegistryObject<Item> GUIDE_CORE = ITEMS.register("guide_core",
        () -> new GuideCoreItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));