package net.mario.advancedfletchingtable.mixin;

import net.mario.advancedfletchingtable.screen.FletchingScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.FletchingTableBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FletchingTableBlock.class)
public class FletchingTableBlockMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                       Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient) {
            SimpleInventory inv = new SimpleInventory(FletchingScreenHandler.INV_SIZE);
            NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, p) -> {
                        FletchingScreenHandler h = new FletchingScreenHandler(syncId, playerInventory, inv);
                        int defaultMode = net.mario.advancedfletchingtable.AdvancedFletchingTable.CONFIG.enableCraftTab
                                ? FletchingScreenHandler.MODE_CRAFT
                                : net.mario.advancedfletchingtable.AdvancedFletchingTable.CONFIG.enableUntintTab
                                        ? FletchingScreenHandler.MODE_UNTINT
                                        : FletchingScreenHandler.MODE_CUSTOMIZE;
                        h.setMode(defaultMode);
                        return h;
                    },
                    Text.translatable("container.advancedfletchingtable.craft")
            );
            player.openHandledScreen(factory);
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
