package acidglow.centereddoors;

import acidglow.centereddoors.item.DoorAdjusterItem;
import acidglow.centereddoors.registry.ModDoors;
import acidglow.centereddoors.test.CenteredDoorsGameTests;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AcidglowsCenteredDoors.MODID)
public class AcidglowsCenteredDoors {
    public static final String MODID = "acidglowscentereddoors";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
            MODID
    );

    public static final DeferredItem<DoorAdjusterItem> DOOR_ADJUSTER = ITEMS.registerItem(
            "door_adjuster",
            properties -> new DoorAdjusterItem(properties.stacksTo(1))
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CENTERED_DOORS_TAB = CREATIVE_MODE_TABS.register(
            "centered_doors",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.acidglowscentereddoors"))
                    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .icon(() -> DOOR_ADJUSTER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(DOOR_ADJUSTER.get()))
                    .build()
    );

    public AcidglowsCenteredDoors(IEventBus modEventBus) {
        ModDoors.register(BLOCKS);
        modEventBus.addListener(CenteredDoorsGameTests::register);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        NeoForge.EVENT_BUS.addListener(DoorAdjusterItem::onRightClickBlock);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DOOR_ADJUSTER);
        }
    }
}
