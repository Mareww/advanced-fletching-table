package net.mario.advancedfletchingtable.screen;

import net.mario.advancedfletchingtable.AdvancedFletchingTable;
import net.mario.advancedfletchingtable.mixin.ScreenHandlerAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public class FletchingScreenHandler extends ScreenHandler {

    public static final int MODE_UNTINT    = 0;
    public static final int MODE_CRAFT     = 1;
    public static final int MODE_CUSTOMIZE = 2;

    // Inventory indices (never change)
    public static final int CRAFT_FEATHER = 0;
    public static final int CRAFT_STICK   = 1;
    public static final int CRAFT_FLINT   = 2;
    public static final int CRAFT_ARROW   = 3;
    public static final int CRAFT_GLOW    = 4;
    public static final int CRAFT_OUT     = 5;
    public static final int UNTINT_ARROW  = 6;
    public static final int UNTINT_MILK   = 7;
    public static final int UNTINT_OUT    = 8;
    public static final int CUST_ARROW    = 9;
    public static final int CUST_DYE      = 10;
    public static final int CUST_GLOW     = 11;
    public static final int CUST_OUT      = 12;
    public static final int INV_SIZE      = 13;

    public static final int PLAYER_INV_SIZE = 36;

    final Inventory inventory;
    private final ArrayPropertyDelegate properties;
    private boolean uncraftMode         = false;
    private boolean collectingAll       = false;
    private boolean craftOutFromSystem  = false; // true when CRAFT_OUT holds a system-computed result

    public FletchingScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(AdvancedFletchingTable.FLETCHING_SCREEN_HANDLER, syncId);
        checkSize(inventory, INV_SIZE);
        this.inventory  = inventory;
        this.properties = new ArrayPropertyDelegate(1);
        this.addProperties(this.properties);
        inventory.onOpen(playerInventory.player);

        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    public FletchingScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(INV_SIZE));
        rebuildSlots(MODE_CRAFT);
    }

    // ── Slot management ───────────────────────────────────────────────────────

    public void rebuildSlots(int mode) {
        ScreenHandlerAccessor acc = (ScreenHandlerAccessor)(Object)this;
        DefaultedList<ItemStack> tracked  = acc.getTrackedStacks();
        DefaultedList<ItemStack> prevTrack = acc.getPreviousTrackedStacks();
        while (this.slots.size() > PLAYER_INV_SIZE) {
            this.slots.remove(this.slots.size() - 1);
            tracked.remove(tracked.size() - 1);
            prevTrack.remove(prevTrack.size() - 1);
        }

        if (mode == MODE_CRAFT) {
            addSlot(new Slot(inventory, CRAFT_FEATHER, 42, 27) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CRAFT && !uncraftMode && s.isOf(Items.FEATHER);
                }
                @Override public void onTakeItem(PlayerEntity player, ItemStack s) {
                    super.onTakeItem(player, s);
                    if (uncraftMode) consumeUncraft(player);
                }
            });
            addSlot(new Slot(inventory, CRAFT_STICK, 65, 27) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CRAFT && !uncraftMode && isStick(s);
                }
                @Override public void onTakeItem(PlayerEntity player, ItemStack s) {
                    super.onTakeItem(player, s);
                    if (uncraftMode) consumeUncraft(player);
                }
            });
            addSlot(new Slot(inventory, CRAFT_FLINT, 88, 27) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CRAFT && !uncraftMode && s.isOf(Items.FLINT);
                }
                @Override public void onTakeItem(PlayerEntity player, ItemStack s) {
                    super.onTakeItem(player, s);
                    if (uncraftMode) consumeUncraft(player);
                }
            });
            addSlot(new Slot(inventory, CRAFT_ARROW, 42, 48) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CRAFT && !uncraftMode && isArrowLike(s);
                }
            });
            addSlot(new Slot(inventory, CRAFT_GLOW, 65, 48) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CRAFT && !uncraftMode && s.isOf(Items.GLOWSTONE_DUST);
                }
            });
            addSlot(new Slot(inventory, CRAFT_OUT, 118, 27) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CRAFT
                            && AdvancedFletchingTable.CONFIG.enableUncrafting
                            && isUncraftableArrowType(s);
                }
                @Override public boolean canTakeItems(net.minecraft.entity.player.PlayerEntity player) {
                    return !collectingAll;
                }
                @Override public void onTakeItem(PlayerEntity player, ItemStack s) {
                    super.onTakeItem(player, s);
                    if (uncraftMode) {
                        // Player took the arrow back — cancel uncraft
                        clearUncraftPreview();
                        uncraftMode = false;
                        updateOutput();
                    } else {
                        onOutputTaken(player);
                    }
                }
            });
        } else if (mode == MODE_UNTINT) {
            addSlot(new Slot(inventory, UNTINT_ARROW, 55, 35) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_UNTINT && isArrowLike(s);
                }
            });
            addSlot(new Slot(inventory, UNTINT_MILK, 78, 35) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_UNTINT
                            && (s.isOf(Items.WATER_BUCKET) || s.isOf(Items.MILK_BUCKET));
                }
            });
            addSlot(new Slot(inventory, UNTINT_OUT, 108, 35) {
                @Override public boolean canInsert(ItemStack s) { return false; }
                @Override public void onTakeItem(PlayerEntity player, ItemStack s) {
                    super.onTakeItem(player, s); onOutputTaken(player);
                }
            });
        } else {
            addSlot(new Slot(inventory, CUST_ARROW, 42, 35) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CUSTOMIZE && isArrowLike(s);
                }
            });
            addSlot(new Slot(inventory, CUST_DYE, 65, 35) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CUSTOMIZE && s.getItem() instanceof DyeItem;
                }
            });
            addSlot(new Slot(inventory, CUST_GLOW, 88, 35) {
                @Override public boolean canInsert(ItemStack s) {
                    return getMode() == MODE_CUSTOMIZE && s.isOf(Items.GLOWSTONE_DUST);
                }
            });
            addSlot(new Slot(inventory, CUST_OUT, 118, 35) {
                @Override public boolean canInsert(ItemStack s) { return false; }
                @Override public void onTakeItem(PlayerEntity player, ItemStack s) {
                    super.onTakeItem(player, s); onOutputTaken(player);
                }
            });
        }
    }

    // ── Mode ──────────────────────────────────────────────────────────────────

    public int getMode() { return properties.get(0); }

    public void setMode(int mode) {
        uncraftMode = false;
        craftOutFromSystem = false;
        properties.set(0, mode);
        rebuildSlots(mode);
        clearAllOutputs();
        updateOutput();
        sendContentUpdates();
    }

    private void clearAllOutputs() {
        inventory.setStack(CRAFT_OUT,  ItemStack.EMPTY);
        inventory.setStack(UNTINT_OUT, ItemStack.EMPTY);
        inventory.setStack(CUST_OUT,   ItemStack.EMPTY);
    }

    private void clearUncraftPreview() {
        inventory.setStack(CRAFT_FEATHER, ItemStack.EMPTY);
        inventory.setStack(CRAFT_STICK,   ItemStack.EMPTY);
        inventory.setStack(CRAFT_FLINT,   ItemStack.EMPTY);
    }

    private boolean craftInputsEmpty() {
        return inventory.getStack(CRAFT_FEATHER).isEmpty()
            && inventory.getStack(CRAFT_STICK).isEmpty()
            && inventory.getStack(CRAFT_FLINT).isEmpty()
            && inventory.getStack(CRAFT_ARROW).isEmpty()
            && inventory.getStack(CRAFT_GLOW).isEmpty();
    }

    public void dropInputsToPlayer(PlayerEntity player) {
        // Drop all non-output inventory slots (including uncraft arrow in CRAFT_OUT)
        for (int i = 0; i < INV_SIZE; i++) {
            if (i == UNTINT_OUT || i == CUST_OUT) continue;
            if (i == CRAFT_OUT && !uncraftMode) continue; // skip craft result, not uncraft arrow
            ItemStack s = inventory.getStack(i);
            if (!s.isEmpty()) { player.giveItemStack(s.copy()); inventory.setStack(i, ItemStack.EMPTY); }
        }
        uncraftMode = false;
        clearAllOutputs();
    }

    // ── Uncraft ───────────────────────────────────────────────────────────────

    /** True if this arrow type is valid for uncrafting (ignores count and NBT quantity). */
    private static boolean isUncraftableArrowType(ItemStack arrow) {
        if (arrow.isOf(Items.SPECTRAL_ARROW) || arrow.isOf(Items.TIPPED_ARROW)) return false;
        if (arrow.hasNbt() && (arrow.getNbt().contains("TrailColor")
                || arrow.getNbt().contains("Potion"))) return false;
        String path = Registries.ITEM.getId(arrow.getItem()).getPath();
        if (!path.endsWith("arrow")) return false;
        String prefix = path.substring(0, path.length() - "arrow".length());
        if (prefix.isEmpty()) return true; // vanilla arrow
        if (prefix.equals("bamboo_")) return true;
        String stickPath = prefix + "stick";
        return Registries.ITEM.getIds().stream()
                .anyMatch(id -> id.getPath().equals(stickPath)
                        && Registries.ITEM.get(id) != Items.AIR);
    }

    /**
     * Returns [feather*sets, stick*sets, flint*sets] or null if the arrow can't be uncrafted.
     * Requires at least craftedArrowCount arrows to yield 1 set, preventing craft→uncraft duplication.
     */
    private ItemStack[] computeUncraft(ItemStack arrow) {
        if (!isUncraftableArrowType(arrow)) return null;
        int craftCount = AdvancedFletchingTable.CONFIG.craftedArrowCount;
        int sets = arrow.getCount() / craftCount;
        if (sets < 1) return null; // not enough arrows for even one set
        String path = Registries.ITEM.getId(arrow.getItem()).getPath();
        String prefix = path.substring(0, path.length() - "arrow".length());
        ItemStack stick;
        if (prefix.isEmpty()) {
            stick = new ItemStack(Items.STICK, sets);
        } else if (prefix.equals("bamboo_")) {
            stick = new ItemStack(Items.BAMBOO, sets);
        } else {
            String stickPath = prefix + "stick";
            var stickItem = Registries.ITEM.getIds().stream()
                    .filter(id -> id.getPath().equals(stickPath))
                    .map(Registries.ITEM::get)
                    .filter(item -> item != Items.AIR)
                    .findFirst().orElse(null);
            stick = stickItem != null ? new ItemStack(stickItem, sets) : null;
        }
        if (stick == null) return null;
        return new ItemStack[]{
            new ItemStack(Items.FEATHER, sets),
            stick,
            new ItemStack(Items.FLINT, sets)
        };
    }

    /** Called when a player takes one of the uncraft result items. Gives remaining results and consumes the arrow. */
    private void consumeUncraft(PlayerEntity player) {
        // Consume the arrow in CRAFT_OUT
        inventory.setStack(CRAFT_OUT, ItemStack.EMPTY);
        // Give any remaining uncraft result items (the taken slot is already empty)
        for (int idx : new int[]{CRAFT_FEATHER, CRAFT_STICK, CRAFT_FLINT}) {
            ItemStack r = inventory.getStack(idx);
            if (!r.isEmpty()) {
                ItemStack give = r.copy();
                inventory.setStack(idx, ItemStack.EMPTY);
                if (!player.giveItemStack(give) && !give.isEmpty())
                    player.dropItem(give, false, false);
            }
        }
        uncraftMode = false;
        player.playSound(SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundCategory.PLAYERS, 1.0f, 1.2f);
        updateOutput();
    }

    // ── Output logic ──────────────────────────────────────────────────────────

    private void onOutputTaken(PlayerEntity player) {
        craftOutFromSystem = false;
        if (getMode() == MODE_CRAFT) {
            boolean hasFeather = !inventory.getStack(CRAFT_FEATHER).isEmpty() && inventory.getStack(CRAFT_FEATHER).isOf(Items.FEATHER);
            boolean hasStick   = !inventory.getStack(CRAFT_STICK).isEmpty()   && isStick(inventory.getStack(CRAFT_STICK));
            boolean hasFlint   = !inventory.getStack(CRAFT_FLINT).isEmpty()   && inventory.getStack(CRAFT_FLINT).isOf(Items.FLINT);
            boolean hasArrow   = !inventory.getStack(CRAFT_ARROW).isEmpty()   && inventory.getStack(CRAFT_ARROW).getItem() instanceof ArrowItem;
            boolean hasGlow    = !inventory.getStack(CRAFT_GLOW).isEmpty()    && inventory.getStack(CRAFT_GLOW).isOf(Items.GLOWSTONE_DUST);
            if (hasArrow && hasGlow) {
                decrement(CRAFT_ARROW); decrement(CRAFT_GLOW);
            } else if (hasFeather && hasStick && hasFlint && hasGlow) {
                decrement(CRAFT_FEATHER); decrement(CRAFT_STICK); decrement(CRAFT_FLINT); decrement(CRAFT_GLOW);
            } else if (hasFeather && hasStick && hasGlow) {
                decrement(CRAFT_FEATHER); decrement(CRAFT_STICK); decrement(CRAFT_GLOW);
            } else if (hasFeather && hasStick && hasFlint) {
                decrement(CRAFT_FEATHER); decrement(CRAFT_STICK); decrement(CRAFT_FLINT);
            }
            player.playSound(SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if (getMode() == MODE_UNTINT) {
            inventory.setStack(UNTINT_ARROW, ItemStack.EMPTY);
            if (!inventory.getStack(UNTINT_MILK).isEmpty())
                inventory.setStack(UNTINT_MILK, new ItemStack(Items.BUCKET));
            player.playSound(SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 0.5f, 1.4f);
        } else {
            ItemStack ca = inventory.getStack(CUST_ARROW);
            ItemStack cd = inventory.getStack(CUST_DYE);
            ItemStack cg = inventory.getStack(CUST_GLOW);
            boolean needsGlow = AdvancedFletchingTable.CONFIG.requireGlowstoneForColor;
            int count = needsGlow
                    ? Math.min(ca.getCount(), Math.min(cd.getCount(), cg.getCount()))
                    : Math.min(ca.getCount(), cd.getCount());
            ca.decrement(count);
            cd.decrement(count);
            if (!cg.isEmpty()) cg.decrement(Math.min(count, cg.getCount()));
            player.playSound(SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }
        inventory.setStack(CRAFT_OUT,  getMode() == MODE_CRAFT   ? ItemStack.EMPTY : inventory.getStack(CRAFT_OUT));
        inventory.setStack(UNTINT_OUT, getMode() == MODE_UNTINT  ? ItemStack.EMPTY : inventory.getStack(UNTINT_OUT));
        inventory.setStack(CUST_OUT,   getMode() == MODE_CUSTOMIZE? ItemStack.EMPTY : inventory.getStack(CUST_OUT));
        updateOutput();
    }

    private void decrement(int slot) {
        ItemStack s = inventory.getStack(slot);
        if (!s.isEmpty()) s.decrement(1);
    }

    public void updateOutput() {
        if (getMode() == MODE_CRAFT) {
            ItemStack arrowInOut = inventory.getStack(CRAFT_OUT);
            boolean hasUncraftArrow = !arrowInOut.isEmpty() && isArrowLike(arrowInOut);

            ItemStack craftResult = computeCraft();

            if (hasUncraftArrow && !craftOutFromSystem && (uncraftMode || craftInputsEmpty())) {
                // Player placed this arrow for uncrafting
                if (!isUncraftableArrowType(arrowInOut)) {
                    // Invalid type — clear it
                    clearUncraftPreview();
                    inventory.setStack(CRAFT_OUT, ItemStack.EMPTY);
                    uncraftMode = false;
                } else {
                    ItemStack[] result = computeUncraft(arrowInOut);
                    if (result != null) {
                        uncraftMode = true;
                        inventory.setStack(CRAFT_FEATHER, result[0]);
                        inventory.setStack(CRAFT_STICK,   result[1]);
                        inventory.setStack(CRAFT_FLINT,   result[2]);
                    } else {
                        // Valid type but insufficient count — keep arrow, no preview
                        clearUncraftPreview();
                        uncraftMode = false;
                    }
                }
            } else if (!hasUncraftArrow || craftOutFromSystem) {
                // Either empty slot, or a system result that must be kept in sync with inputs
                if (uncraftMode) { clearUncraftPreview(); uncraftMode = false; }
                inventory.setStack(CRAFT_OUT, craftResult);
                craftOutFromSystem = !craftResult.isEmpty();
            }
            // else: player-placed arrow with inputs occupied — leave alone
            inventory.setStack(UNTINT_OUT, ItemStack.EMPTY);
            inventory.setStack(CUST_OUT,   ItemStack.EMPTY);
        } else if (getMode() == MODE_UNTINT) {
            inventory.setStack(CRAFT_OUT,  ItemStack.EMPTY);
            inventory.setStack(UNTINT_OUT, computeUntint());
            inventory.setStack(CUST_OUT,   ItemStack.EMPTY);
        } else {
            inventory.setStack(CRAFT_OUT,  ItemStack.EMPTY);
            inventory.setStack(UNTINT_OUT, ItemStack.EMPTY);
            inventory.setStack(CUST_OUT,   computeCustomize());
        }
        sendContentUpdates();
    }

    private ItemStack computeCraft() {
        ItemStack feather = inventory.getStack(CRAFT_FEATHER);
        ItemStack stick   = inventory.getStack(CRAFT_STICK);
        ItemStack flint   = inventory.getStack(CRAFT_FLINT);
        ItemStack arrow   = inventory.getStack(CRAFT_ARROW);
        ItemStack glow    = inventory.getStack(CRAFT_GLOW);
        boolean hasFeather = !feather.isEmpty() && feather.isOf(Items.FEATHER);
        boolean hasStick   = !stick.isEmpty()   && isStick(stick);
        boolean hasFlint   = !flint.isEmpty()   && flint.isOf(Items.FLINT);
        boolean hasArrow   = !arrow.isEmpty()   && isArrowLike(arrow);
        boolean hasGlow    = !glow.isEmpty()    && glow.isOf(Items.GLOWSTONE_DUST);
        int spectral = AdvancedFletchingTable.CONFIG.spectralArrowCount;
        int crafted  = AdvancedFletchingTable.CONFIG.craftedArrowCount;
        if (hasArrow && hasGlow)                           return new ItemStack(Items.SPECTRAL_ARROW, spectral);
        if (hasFeather && hasStick && hasFlint && hasGlow) return new ItemStack(Items.SPECTRAL_ARROW, spectral);
        if (hasFeather && hasStick && hasFlint)            return arrowForStick(stick, crafted);
        return ItemStack.EMPTY;
    }

    private ItemStack computeUntint() {
        ItemStack arrow  = inventory.getStack(UNTINT_ARROW);
        ItemStack liquid = inventory.getStack(UNTINT_MILK);
        if (arrow.isEmpty() || !(isArrowLike(arrow))) return ItemStack.EMPTY;
        ItemStack result = arrow.copy();
        if (liquid.isOf(Items.WATER_BUCKET)) {
            if (!arrow.hasNbt() || !arrow.getNbt().contains("TrailColor")) return ItemStack.EMPTY;
            result.getNbt().remove("TrailColor");
            result.getNbt().remove("TrailColorName");
            if (result.getNbt().isEmpty()) result.setNbt(null);
            return result;
        } else if (liquid.isOf(Items.MILK_BUCKET)) {
            if (!arrow.hasNbt() || !arrow.getNbt().contains("Potion")) return ItemStack.EMPTY;
            // Find the plain (non-tipped) variant of this arrow item
            Identifier arrowId = Registries.ITEM.getId(arrow.getItem());
            String plainPath = arrowId.getPath().replace("tipped_", ""); // "tipped_oak_arrow" → "oak_arrow"
            var plainItem = Registries.ITEM.get(new Identifier(arrowId.getNamespace(), plainPath));
            if (plainItem == Items.AIR)
                plainItem = Registries.ITEM.get(new Identifier("mstv-mweaponv", plainPath));
            if (plainItem == Items.AIR) plainItem = Items.ARROW;
            ItemStack plain = new ItemStack(plainItem, result.getCount());
            // Preserve TrailColor if present
            if (result.hasNbt() && result.getNbt().contains("TrailColor")) {
                plain.getOrCreateNbt().putInt("TrailColor", result.getNbt().getInt("TrailColor"));
                plain.getOrCreateNbt().putString("TrailColorName", result.getNbt().getString("TrailColorName"));
            }
            return plain;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack computeCustomize() {
        ItemStack arrow = inventory.getStack(CUST_ARROW);
        ItemStack dye   = inventory.getStack(CUST_DYE);
        ItemStack glow  = inventory.getStack(CUST_GLOW);
        if (arrow.isEmpty() || !(isArrowLike(arrow))) return ItemStack.EMPTY;
        if (dye.isEmpty()   || !(dye.getItem() instanceof DyeItem))     return ItemStack.EMPTY;
        boolean needsGlow = AdvancedFletchingTable.CONFIG.requireGlowstoneForColor;
        if (needsGlow && (glow.isEmpty() || !glow.isOf(Items.GLOWSTONE_DUST))) return ItemStack.EMPTY;
        int count = needsGlow
                ? Math.min(arrow.getCount(), Math.min(dye.getCount(), glow.getCount()))
                : Math.min(arrow.getCount(), dye.getCount());
        ItemStack out = arrow.copyWithCount(count);
        NbtCompound nbt = out.getOrCreateNbt();
        float[] c = ((DyeItem) dye.getItem()).getColor().getColorComponents();
        nbt.putInt("TrailColor", ((int)(c[0]*255) << 16) | ((int)(c[1]*255) << 8) | (int)(c[2]*255));
        nbt.putString("TrailColorName", Registries.ITEM.getId(dye.getItem()).getPath().replace("_dye", ""));
        return out;
    }

    private static boolean isArrowLike(ItemStack s) {
        if (s.getItem() instanceof ArrowItem) return true;
        return Registries.ITEM.getId(s.getItem()).getPath().endsWith("arrow");
    }

    private static boolean isStick(ItemStack stack) {
        if (stack.isOf(Items.BAMBOO)) return true;
        String p = Registries.ITEM.getId(stack.getItem()).getPath();
        return p.equals("stick") || p.endsWith("_stick");
    }

    private static ItemStack arrowForStick(ItemStack stick, int count) {
        // Raw bamboo → bamboo_arrow
        if (stick.isOf(Items.BAMBOO)) {
            var found = Registries.ITEM.getIds().stream()
                    .filter(id -> id.getPath().equals("bamboo_arrow"))
                    .map(Registries.ITEM::get)
                    .filter(item -> item != Items.AIR)
                    .findFirst().orElse(null);
            return found != null ? new ItemStack(found, count) : new ItemStack(Items.ARROW, count);
        }
        String path = Registries.ITEM.getId(stick.getItem()).getPath();
        if (path.endsWith("_stick")) {
            String ap = path.substring(0, path.length() - "stick".length()) + "arrow";
            var found = Registries.ITEM.getIds().stream()
                    .filter(id -> id.getPath().equals(ap))
                    .map(Registries.ITEM::get)
                    .filter(item -> item != Items.AIR)
                    .findFirst().orElse(null);
            if (found != null) return new ItemStack(found, count);
        }
        return new ItemStack(Items.ARROW, count);
    }

    // ── Screen handler overrides ───────────────────────────────────────────────

    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        collectingAll = (actionType == net.minecraft.screen.slot.SlotActionType.PICKUP_ALL);
        super.onSlotClick(slotIndex, button, actionType, player);
        collectingAll = false;
        updateOutput();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        if (invSlot < 0 || invSlot >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack orig = slot.getStack();
        ItemStack copy = orig.copy();
        int outputSlot = this.slots.size() - 1;

        if (invSlot == outputSlot) {
            if (!this.insertItem(orig, 0, PLAYER_INV_SIZE, true)) return ItemStack.EMPTY;
            slot.onQuickTransfer(orig, copy);
            // onTakeItem is triggered by onQuickTransfer — uncraft/output handled there
        } else if (invSlot < PLAYER_INV_SIZE) {
            boolean placed = false;
            // Try CRAFT_OUT first — arrows go there for uncrafting when inputs are empty
            Slot out = this.slots.get(outputSlot);
            if (out.canInsert(orig) && this.insertItem(orig, outputSlot, outputSlot + 1, false)) {
                placed = true;
            }
            if (!placed) {
                for (int i = PLAYER_INV_SIZE; i < outputSlot; i++) {
                    if (this.slots.get(i).canInsert(orig) && this.insertItem(orig, i, i + 1, false)) {
                        placed = true; break;
                    }
                }
            }
            if (!placed) return ItemStack.EMPTY;
        } else {
            if (!this.insertItem(orig, 0, PLAYER_INV_SIZE, false)) return ItemStack.EMPTY;
        }

        if (orig.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        if (orig.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, orig);
        updateOutput();
        return copy;
    }

    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
        this.dropInventory(player, inventory);
    }
}
