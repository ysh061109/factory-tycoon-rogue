package com.ysh.tycoonrogue;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TycoonRogue.MODID)
public class TycoonRogue {
    public static final String MODID = "tycoonrogue";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TycoonRogue(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Factory Tycoon Rogue loaded");
    }
}
