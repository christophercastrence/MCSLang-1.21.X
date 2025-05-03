package net.chris.mcscript.item;

import net.chris.mcscript.MCScript;
import net.chris.mcscript.item.custom.ChiselItem;
import net.chris.mcscript.item.custom.ScriptExecItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // Registers Items into the game
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MCScript.MOD_ID);

    public static final RegistryObject<Item> EXECUTER = ITEMS.register("executor",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(32)));

    public static final RegistryObject<Item> SCRIPTEXECUTOR = ITEMS.register("scriptexecutor",
            () -> new ScriptExecItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
