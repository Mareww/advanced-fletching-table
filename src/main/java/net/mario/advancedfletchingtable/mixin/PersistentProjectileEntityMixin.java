package net.mario.advancedfletchingtable.mixin;

import net.mario.advancedfletchingtable.duck.ITrailArrow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin implements ITrailArrow {

    @Unique
    private static final TrackedData<Integer> TRAIL_COLOR =
            DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private static final TrackedData<String> TRAIL_COLOR_NAME =
            DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.STRING);

    @Unique
    private NbtCompound advancedfletchingtable$savedItemNbt = null;

    @Unique
    private DataTracker advancedfletchingtable$tracker() {
        return ((Entity)(Object)this).getDataTracker();
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initTrailTracker(CallbackInfo ci) {
        advancedfletchingtable$tracker().startTracking(TRAIL_COLOR, -1);
        advancedfletchingtable$tracker().startTracking(TRAIL_COLOR_NAME, "");
    }

    @Override public int advancedfletchingtable$getTrailColor()        { return advancedfletchingtable$tracker().get(TRAIL_COLOR); }
    @Override public String advancedfletchingtable$getTrailColorName() { return advancedfletchingtable$tracker().get(TRAIL_COLOR_NAME); }
    @Override public boolean advancedfletchingtable$hasTrail()         { return advancedfletchingtable$tracker().get(TRAIL_COLOR) != -1; }
    @Override public NbtCompound advancedfletchingtable$getSavedItemNbt()         { return advancedfletchingtable$savedItemNbt; }
    @Override public void advancedfletchingtable$setSavedItemNbt(NbtCompound nbt) { advancedfletchingtable$savedItemNbt = nbt; }

    @Override
    public void advancedfletchingtable$setTrail(int color, String name) {
        advancedfletchingtable$tracker().set(TRAIL_COLOR, color);
        advancedfletchingtable$tracker().set(TRAIL_COLOR_NAME, name);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeTrail(NbtCompound nbt, CallbackInfo ci) {
        int color = advancedfletchingtable$tracker().get(TRAIL_COLOR);
        if (color != -1) {
            nbt.putInt("TrailColor", color);
            nbt.putString("TrailColorName", advancedfletchingtable$tracker().get(TRAIL_COLOR_NAME));
        }
        if (advancedfletchingtable$savedItemNbt != null) {
            nbt.put("AFTItemNbt", advancedfletchingtable$savedItemNbt);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readTrail(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("TrailColor")) {
            advancedfletchingtable$tracker().set(TRAIL_COLOR, nbt.getInt("TrailColor"));
            advancedfletchingtable$tracker().set(TRAIL_COLOR_NAME, nbt.getString("TrailColorName"));
        }
        if (nbt.contains("AFTItemNbt")) {
            advancedfletchingtable$savedItemNbt = nbt.getCompound("AFTItemNbt");
        }
    }

    @Redirect(
            method = "tryPickup",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean addTrailOnPickup(PlayerInventory inventory, ItemStack stack) {
        if (advancedfletchingtable$hasTrail()) {
            stack.getOrCreateNbt().putInt("TrailColor", advancedfletchingtable$getTrailColor());
            stack.getOrCreateNbt().putString("TrailColorName", advancedfletchingtable$getTrailColorName());
        }
        return inventory.insertStack(stack);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"),
            require = 0)
    private void suppressCritParticles(World world, ParticleEffect effect,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
        if (advancedfletchingtable$hasTrail() && effect == ParticleTypes.CRIT) return;
        world.addParticle(effect, x, y, z, vx, vy, vz);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void spawnTrailParticles(CallbackInfo ci) {
        int color = advancedfletchingtable$tracker().get(TRAIL_COLOR);
        Entity self = (Entity)(Object)this;
        if (color == -1) return;
        if (!self.getWorld().isClient) return;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = (color         & 0xFF) / 255f;
        for (int i = 0; i < 5; i++) {
            double ox = (Math.random() - 0.5) * 0.2;
            double oy = (Math.random() - 0.5) * 0.2;
            double oz = (Math.random() - 0.5) * 0.2;
            self.getWorld().addParticle(
                    new DustParticleEffect(new Vector3f(r, g, b), 2.0f),
                    self.getX() + ox, self.getY() + oy, self.getZ() + oz,
                    0.0, 0.0, 0.0);
        }
    }
}
