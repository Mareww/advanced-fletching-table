package net.mario.advancedfletchingtable;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "advancedfletchingtable")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enableCraftTab = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableUntintTab = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableCustomizeTab = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableUncrafting = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
    public int craftedArrowCount = 4;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
    public int craftingTableArrowCount = 1;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
    public int spectralArrowCount = 4;

    @ConfigEntry.Gui.Tooltip
    public boolean requireGlowstoneForColor = true;

    @ConfigEntry.Gui.Tooltip
    public boolean preserveTrailColorCraftingTable = true;

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean preserveTrailColorCauldron = true;
}
