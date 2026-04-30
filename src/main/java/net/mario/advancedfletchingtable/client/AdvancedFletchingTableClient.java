package net.mario.advancedfletchingtable.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.mario.advancedfletchingtable.AdvancedFletchingTable;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.ArrowItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AdvancedFletchingTableClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(AdvancedFletchingTable.FLETCHING_SCREEN_HANDLER, FletchingScreen::new);

        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.getItem() instanceof ArrowItem
                    && stack.hasNbt()
                    && stack.getNbt().contains("TrailColor")) {
                String raw = stack.getNbt().getString("TrailColorName");
                String name = raw.isEmpty() ? "Unknown"
                        : Character.toUpperCase(raw.charAt(0)) + raw.substring(1).replace('_', ' ');
                int color = stack.getNbt().getInt("TrailColor");
                lines.add(Text.literal("Color: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(name)
                                .styled(s -> s.withColor(color))));
            }
        });
    }
}
