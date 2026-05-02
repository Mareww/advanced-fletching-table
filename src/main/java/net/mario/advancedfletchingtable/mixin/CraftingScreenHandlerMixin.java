package net.mario.advancedfletchingtable.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {

    @Inject(method = "updateResult",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/inventory/CraftingResultInventory;setStack(ILnet/minecraft/item/ItemStack;)V",
                     shift = At.Shift.AFTER))
    private static void preserveTrailColor(ScreenHandler handler, World world, PlayerEntity player,
                                            RecipeInputInventory craftingInventory,
                                            CraftingResultInventory resultInventory,
                                            CallbackInfo ci) {
        ItemStack result = resultInventory.getStack(0);
        if (result.isEmpty()) return;
        // Cap any arrow type from crafting table to 1 (includes wood variants like warped_arrow)
        String path = net.minecraft.registry.Registries.ITEM.getId(result.getItem()).getPath();
        int tableMax = net.mario.advancedfletchingtable.AdvancedFletchingTable.CONFIG.craftingTableArrowCount;
        if (path.endsWith("arrow") && !path.contains("tipped") && result.getCount() > tableMax) {
            result.setCount(tableMax);
            return;
        }
        if (!net.mario.advancedfletchingtable.AdvancedFletchingTable.CONFIG.preserveTrailColorCraftingTable) return;
        if (!result.hasNbt() || !result.getNbt().contains("Potion")) return;
        for (int i = 0; i < craftingInventory.size(); i++) {
            ItemStack in = craftingInventory.getStack(i);
            if (in.hasNbt() && in.getNbt().contains("TrailColor")) {
                result.getOrCreateNbt().putInt("TrailColor", in.getNbt().getInt("TrailColor"));
                result.getOrCreateNbt().putString("TrailColorName", in.getNbt().getString("TrailColorName"));
                return;
            }
        }
    }
}
