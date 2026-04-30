package net.mario.advancedfletchingtable;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mario.advancedfletchingtable.network.ModPackets;
import net.mario.advancedfletchingtable.screen.FletchingScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedFletchingTable implements ModInitializer {

    public static final String MOD_ID = "advancedfletchingtable";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ScreenHandlerType<FletchingScreenHandler> FLETCHING_SCREEN_HANDLER;
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        FLETCHING_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MOD_ID, "fletching"),
                new ScreenHandlerType<>(FletchingScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.SWITCH_TAB, (server, player, handler, buf, responseSender) -> {
            byte tab = buf.readByte();
            server.execute(() -> {
                if (player.currentScreenHandler instanceof FletchingScreenHandler fh) {
                    fh.dropInputsToPlayer(player);
                    int mode = tab == ModPackets.TAB_CRAFT     ? FletchingScreenHandler.MODE_CRAFT
                             : tab == ModPackets.TAB_CUSTOMIZE ? FletchingScreenHandler.MODE_CUSTOMIZE
                             : FletchingScreenHandler.MODE_UNTINT;
                    // Respect config tab toggles
                    if (mode == FletchingScreenHandler.MODE_CRAFT     && !CONFIG.enableCraftTab)     return;
                    if (mode == FletchingScreenHandler.MODE_UNTINT    && !CONFIG.enableUntintTab)    return;
                    if (mode == FletchingScreenHandler.MODE_CUSTOMIZE && !CONFIG.enableCustomizeTab) return;
                    fh.setMode(mode);
                }
            });
        });

        LOGGER.info("Advanced Fletching Table initialized.");
    }
}
