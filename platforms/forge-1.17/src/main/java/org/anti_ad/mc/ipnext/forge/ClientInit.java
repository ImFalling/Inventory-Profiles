package org.anti_ad.mc.ipnext.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmlclient.ConfigGuiHandler;
import org.anti_ad.mc.common.forge.CommonForgeEventHandler;
import org.anti_ad.mc.common.vanilla.VanillaSound;
import org.anti_ad.mc.ipnext.InventoryProfilesKt;
import org.anti_ad.mc.ipnext.event.Sounds;
import org.anti_ad.mc.ipnext.gui.ConfigScreen;


public class ClientInit implements Runnable {
    @Override
    public void run() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString(),
                        (remote, isServer) -> true));

        MinecraftForge.EVENT_BUS.register(new CommonForgeEventHandler());

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());

        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, () ->
                new ConfigGuiHandler.ConfigGuiFactory((minecraft, screen) -> new ConfigScreen()));

        InventoryProfilesKt.init();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        VanillaSound.INSTANCE.getREGISTER().register(bus);
        Sounds.Companion.registerAll();
    }
}
