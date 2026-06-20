package net.example.taczkillfeed;

import net.example.taczkillfeed.event.KillHandler;
import net.example.taczkillfeed.network.ModMessages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TaczKillfeed.MOD_ID)
public class TaczKillfeed {
    public static final String MOD_ID = "tacz_killfeed";
    public static final Logger LOGGER = LogManager.getLogger();

    public TaczKillfeed() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(new KillHandler());
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);
        LOGGER.info("TaCZ Killfeed: инициализация завершена.");
    }
}