package net.mario.advancedfletchingtable.mixin;

import net.mario.advancedfletchingtable.duck.ITrailArrow;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArrowItem.class)
public abstract class ArrowEntityMixin {

    @Inject(method = "createArrow", at = @At("RETURN"))
    private void copyTrailToEntity(World world, ItemStack stack, LivingEntity shooter,
                                   CallbackInfoReturnable<PersistentProjectileEntity> cir) {
        if (stack.hasNbt() && stack.getNbt().contains("TrailColor")) {
            int color = stack.getNbt().getInt("TrailColor");
            String name = stack.getNbt().getString("TrailColorName");
            ((ITrailArrow) cir.getReturnValue()).advancedfletchingtable$setTrail(color, name);
        }
    }
}
