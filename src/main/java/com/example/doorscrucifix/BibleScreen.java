package com.example.doorscrucifix;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Paged list of online players. Click a name to teleport to them (or bring them to you). */
public class BibleScreen extends Screen {
    private static final int PER_PAGE = 7;

    private final List<NetworkPlayerInfo> players = new ArrayList<>();
    private int page = 0;
    private boolean bring = false;

    public BibleScreen() {
        super(new StringTextComponent("Admin Bible"));
    }

    private ITextComponent modeText() {
        return new StringTextComponent(bring ? "Mode: Bring player to me" : "Mode: Teleport to player");
    }

    @Override
    protected void init() {
        players.clear();
        ClientPlayNetHandler conn = this.minecraft.getConnection();
        if (conn != null) {
            for (NetworkPlayerInfo info : conn.getPlayerInfoMap()) {
                if (!info.getGameProfile().getId().equals(this.minecraft.player.getUniqueID())) {
                    players.add(info);
                }
            }
        }
        players.sort(Comparator.comparing(i -> i.getGameProfile().getName().toLowerCase()));

        int cx = this.width / 2;
        int top = this.height / 2 - 90;

        this.addButton(new Button(cx - 100, top, 200, 20, modeText(), b -> {
            bring = !bring;
            b.setMessage(modeText());
        }));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < players.size(); i++) {
            NetworkPlayerInfo info = players.get(start + i);
            this.addButton(new Button(cx - 100, top + 28 + i * 22, 200, 20,
                    new StringTextComponent(info.getGameProfile().getName()), b -> {
                ModNetwork.CHANNEL.sendToServer(new TeleportPacket(info.getGameProfile().getId(), bring));
                this.onClose();
            }));
        }

        int maxPage = Math.max(0, (players.size() - 1) / PER_PAGE);
        int navY = top + 28 + PER_PAGE * 22 + 4;
        Button prev = this.addButton(new Button(cx - 100, navY, 98, 20, new StringTextComponent("< Prev"), b -> {
            page--;
            this.init(this.minecraft, this.width, this.height);
        }));
        Button next = this.addButton(new Button(cx + 2, navY, 98, 20, new StringTextComponent("Next >"), b -> {
            page++;
            this.init(this.minecraft, this.width, this.height);
        }));
        prev.active = page > 0;
        next.active = page < maxPage;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        int cx = this.width / 2;
        int top = this.height / 2 - 90;
        drawCenteredString(ms, this.font, this.title, cx, top - 14, 0xFFD700);
        if (players.isEmpty()) {
            drawCenteredString(ms, this.font, new StringTextComponent("No other players online"), cx, top + 40, 0xAAAAAA);
        }
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
