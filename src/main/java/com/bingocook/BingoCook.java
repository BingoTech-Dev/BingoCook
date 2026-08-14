package com.bingocook;

import com.bingocook.cooking.CookingEvents;
import com.bingocook.cooking.CookingRegistries;
import com.bingocook.cooking.DataMaps;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BingoCook.MODID)
public class BingoCook {

    public static final String MODID = "bingocook";

    public BingoCook(IEventBus modEventBus) {
        CookingRegistries.register(modEventBus);

        // Register the "item -> elemental values" data map type
        modEventBus.addListener(DataMaps::registerDataMapTypes);

        NeoForge.EVENT_BUS.register(CookingEvents.INSTANCE);
    }
}
