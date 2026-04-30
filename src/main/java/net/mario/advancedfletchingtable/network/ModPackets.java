package net.mario.advancedfletchingtable.network;

import net.mario.advancedfletchingtable.AdvancedFletchingTable;
import net.minecraft.util.Identifier;

public class ModPackets {
    public static final Identifier SWITCH_TAB = new Identifier(AdvancedFletchingTable.MOD_ID, "switch_tab");

    public static final byte TAB_UNTINT    = 0;
    public static final byte TAB_CRAFT     = 1;
    public static final byte TAB_CUSTOMIZE = 2;
}
