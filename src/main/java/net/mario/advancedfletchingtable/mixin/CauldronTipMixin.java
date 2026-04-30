package net.mario.advancedfletchingtable.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.marew.advancedcauldron.registry.ModCauldronBehaviors", remap = false)
public class CauldronTipMixin {

    private static final ThreadLocal<Integer> TRAIL_COLOR = new ThreadLocal<>();
    private static final ThreadLocal<String> TRAIL_COLOR_NAME = new ThreadLocal<>();

    @Inject(method = "tryTipArrow", at = @At("HEAD"), remap = false)
    private static void captureInputArrow(BlockState state, World world, BlockPos pos,
            PlayerEntity player, Hand hand, ItemStack stack,
            CallbackInfoReturnable<ActionResult> cir) {
        if (!net.mario.advancedfletchingtable.AdvancedFletchingTable.CONFIG.preserveTrailColorCauldron) {
            TRAIL_COLOR.set(null); TRAIL_COLOR_NAME.set(null); return;
        }
        if (stack.hasNbt() && stack.getNbt().contains("TrailColor")) {
            TRAIL_COLOR.set(stack.getNbt().getInt("TrailColor"));
            TRAIL_COLOR_NAME.set(stack.getNbt().getString("TrailColorName"));
        } else {
            TRAIL_COLOR.set(null);
            TRAIL_COLOR_NAME.set(null);
        }
    }

    @ModifyArg(
        method = "tryTipArrow",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1661;method_7394(Lnet/minecraft/class_1799;)Z"),
        index = 0,
        remap = false
    )
    private static ItemStack addTrailColorToTippedArrows(ItemStack tippedArrows) {
        Integer color = TRAIL_COLOR.get();
        String name = TRAIL_COLOR_NAME.get();
        if (color != null && name != null) {
            tippedArrows.getOrCreateNbt().putInt("TrailColor", color);
            tippedArrows.getOrCreateNbt().putString("TrailColorName", name);
            TRAIL_COLOR.set(null);
            TRAIL_COLOR_NAME.set(null);
        }
        return tippedArrows;
    }
}
