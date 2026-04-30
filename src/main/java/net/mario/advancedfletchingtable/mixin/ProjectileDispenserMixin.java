package net.mario.advancedfletchingtable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mario.advancedfletchingtable.duck.ITrailArrow;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectileDispenserBehavior.class)
public class ProjectileDispenserMixin {

    @WrapOperation(
        method = "dispenseSilently",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/dispenser/ProjectileDispenserBehavior;createProjectile(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Position;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/projectile/ProjectileEntity;")
    )
    private ProjectileEntity copyTrailToDispensedArrow(
            ProjectileDispenserBehavior self, World world, Position pos, ItemStack stack,
            Operation<ProjectileEntity> original, BlockPointer pointer, ItemStack dispensedStack) {
        ProjectileEntity entity = original.call(self, world, pos, stack);
        if (dispensedStack.hasNbt() && dispensedStack.getNbt().contains("TrailColor")
                && entity instanceof ITrailArrow trail) {
            trail.advancedfletchingtable$setTrail(
                dispensedStack.getNbt().getInt("TrailColor"),
                dispensedStack.getNbt().getString("TrailColorName")
            );
        }
        return entity;
    }
}
