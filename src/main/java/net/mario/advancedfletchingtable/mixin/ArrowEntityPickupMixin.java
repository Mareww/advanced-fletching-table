package net.mario.advancedfletchingtable.mixin;

import net.mario.advancedfletchingtable.duck.ITrailArrow;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArrowEntity.class)
public abstract class ArrowEntityPickupMixin {

    @Inject(method = "initFromStack", at = @At("TAIL"))
    private void copyTrailFromStack(ItemStack stack, CallbackInfo ci) {
        ITrailArrow self = (ITrailArrow)(Object)this;
        if (stack.hasNbt()) {
            self.advancedfletchingtable$setSavedItemNbt(stack.getNbt().copy());
            if (stack.getNbt().contains("TrailColor")) {
                self.advancedfletchingtable$setTrail(
                        stack.getNbt().getInt("TrailColor"),
                        stack.getNbt().getString("TrailColorName"));
            }
        }
    }

    @Inject(method = "asItemStack", at = @At("RETURN"))
    private void copyTrailToItem(CallbackInfoReturnable<ItemStack> cir) {
        ITrailArrow self = (ITrailArrow)(Object)this;
        NbtCompound saved = self.advancedfletchingtable$getSavedItemNbt();
        if (saved != null) {
            NbtCompound nbt = cir.getReturnValue().getOrCreateNbt();
            for (String key : saved.getKeys()) {
                nbt.put(key, saved.get(key).copy());
            }
        } else if (self.advancedfletchingtable$hasTrail()) {
            NbtCompound nbt = cir.getReturnValue().getOrCreateNbt();
            nbt.putInt("TrailColor", self.advancedfletchingtable$getTrailColor());
            nbt.putString("TrailColorName", self.advancedfletchingtable$getTrailColorName());
        }
    }
}
