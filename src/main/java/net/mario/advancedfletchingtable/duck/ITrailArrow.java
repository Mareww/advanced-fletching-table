package net.mario.advancedfletchingtable.duck;

import net.minecraft.nbt.NbtCompound;

public interface ITrailArrow {
    int advancedfletchingtable$getTrailColor();
    String advancedfletchingtable$getTrailColorName();
    void advancedfletchingtable$setTrail(int color, String name);
    boolean advancedfletchingtable$hasTrail();
    NbtCompound advancedfletchingtable$getSavedItemNbt();
    void advancedfletchingtable$setSavedItemNbt(NbtCompound nbt);
}
