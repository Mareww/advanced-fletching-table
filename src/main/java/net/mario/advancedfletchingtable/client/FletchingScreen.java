package net.mario.advancedfletchingtable.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.mario.advancedfletchingtable.AdvancedFletchingTable;
import net.mario.advancedfletchingtable.network.ModPackets;
import net.mario.advancedfletchingtable.screen.FletchingScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FletchingScreen extends HandledScreen<FletchingScreenHandler> {

    private static final Identifier TEXTURE_UNTINT =
            new Identifier(AdvancedFletchingTable.MOD_ID, "textures/gui/untint_gui.png");
    private static final Identifier TEXTURE_CRAFT =
            new Identifier(AdvancedFletchingTable.MOD_ID, "textures/gui/craft_gui.png");
    private static final Identifier TEXTURE_CUSTOMIZE =
            new Identifier(AdvancedFletchingTable.MOD_ID, "textures/gui/customize_gui.png");

    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 166;

    public FletchingScreen(FletchingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = this.backgroundWidth - this.textRenderer.getWidth(this.title) - 5;
        this.titleY = 6;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (this.width  - this.backgroundWidth)  / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        Identifier texture = switch (this.handler.getMode()) {
            case FletchingScreenHandler.MODE_CRAFT     -> TEXTURE_CRAFT;
            case FletchingScreenHandler.MODE_CUSTOMIZE -> TEXTURE_CUSTOMIZE;
            default                                    -> TEXTURE_UNTINT;
        };
        context.drawTexture(texture, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        for (int i = FletchingScreenHandler.PLAYER_INV_SIZE; i < this.handler.slots.size(); i++) {
            Slot slot = this.handler.slots.get(i);
            if (slot.hasStack()) {
                context.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.playerInventoryTitle,
                this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    private int lastMode = -1;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int mode = this.handler.getMode();
        if (mode != lastMode) {
            lastMode = mode;
            this.handler.rebuildSlots(mode);
        }
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - this.x;
        int relY = (int) mouseY - this.y;
        if (relY >= 1 && relY <= 13) {
            int mode = this.handler.getMode();
            var cfg = net.mario.advancedfletchingtable.AdvancedFletchingTable.CONFIG;
            if (cfg.enableCraftTab && mode != FletchingScreenHandler.MODE_CRAFT && relX >= 3 && relX <= 30) {
                sendTabSwitch(ModPackets.TAB_CRAFT);
                return true;
            }
            if (cfg.enableUntintTab && mode != FletchingScreenHandler.MODE_UNTINT && relX >= 32 && relX <= 61) {
                sendTabSwitch(ModPackets.TAB_UNTINT);
                return true;
            }
            if (cfg.enableCustomizeTab && mode != FletchingScreenHandler.MODE_CUSTOMIZE && relX >= 63 && relX <= 110) {
                sendTabSwitch(ModPackets.TAB_CUSTOMIZE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendTabSwitch(byte tab) {
        var buf = PacketByteBufs.create();
        buf.writeByte(tab);
        ClientPlayNetworking.send(ModPackets.SWITCH_TAB, buf);
    }
}
