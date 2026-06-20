package net.example.taczkillfeed.client;

import net.example.taczkillfeed.network.KillfeedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class KillfeedOverlay {
    private static final List<KillfeedEntry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final long DISPLAY_DURATION = 6000L;
    private static final long FADE_DURATION = 1000L;

    public static void addEntry(KillfeedPacket packet) {
        ENTRIES.add(new KillfeedEntry(packet));
        if (ENTRIES.size() > 5) {
            ENTRIES.remove(0);
        }
    }

    public static void renderKillfeed(GuiGraphics guiGraphics, float partialTick) {
        long now = System.currentTimeMillis();
        ENTRIES.removeIf(entry -> now - entry.spawnTime > DISPLAY_DURATION);
        if (ENTRIES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = guiGraphics.guiWidth();
        int xRight = screenWidth - 10;
        int yStart = 10;

        UUID localUUID = mc.player != null ? mc.player.getUUID() : null;

        int index = 0;
        for (KillfeedEntry entry : ENTRIES) {
            long elapsed = now - entry.spawnTime;
            float alpha = 1.0f;
            if (elapsed > DISPLAY_DURATION - FADE_DURATION) {
                alpha = 1.0f - ((float) (elapsed - (DISPLAY_DURATION - FADE_DURATION)) / FADE_DURATION);
            }
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));

            boolean isLocalPlayerInvolved = localUUID != null &&
                    (localUUID.equals(entry.killerUUID) || localUUID.equals(entry.victimUUID));

            renderEntry(guiGraphics, font, entry, xRight, yStart + (index * 24), alpha, isLocalPlayerInvolved);
            index++;
        }
    }

    private static void renderEntry(GuiGraphics guiGraphics, Font font, KillfeedEntry entry,
                                    int xRight, int y, float alpha, boolean isLocalPlayerInvolved) {
        int alphaBits = ((int) (alpha * 255)) << 24;
        int bgAlpha = ((int) (alpha * 200)) << 24;

        String killer = entry.killerName != null ? entry.killerName : "?";
        String victim = entry.victimName != null ? entry.victimName : "?";

        String streakText = "";
        int streakColor = 0;
        if (entry.killStreak >= 5) {
            streakText = "ON FIRE";
            streakColor = 0xFF8800;
        } else if (entry.killStreak >= 2) {
            streakText = entry.killStreak + "x";
            streakColor = 0xFFD700;
        }

        int killerWidth = font.width(killer);
        int streakWidth = streakText.isEmpty() ? 0 : font.width(streakText);
        int victimWidth = font.width(victim);
        int iconSize = 16;
        int gap = 6;
        int smallGap = 2;

        int hsIconWidth = 0;
        if (entry.isHeadshot) {
            hsIconWidth = 12;
        }

        String assistText = "";
        int assistWidth = 0;
        if (!entry.assistName.isEmpty()) {
            assistText = "A: " + entry.assistName;
            assistWidth = font.width(assistText);
        }

        int totalWidth = killerWidth +
                (streakWidth > 0 ? smallGap + streakWidth : 0) +
                gap + iconSize + gap + victimWidth +
                (hsIconWidth > 0 ? smallGap + hsIconWidth : 0);
        int assistLineWidth = assistWidth > 0 ? assistWidth : 0;
        int maxWidth = Math.max(totalWidth, assistLineWidth);

        int padX = 6;
        int left = xRight - maxWidth - (padX * 2);
        int right = xRight;
        int top = y;
        int entryHeight = 20;
        int totalHeight = entryHeight + (assistWidth > 0 ? 12 : 0);
        int bottom = y + totalHeight;

        int bgColor;
        switch (entry.killType) {
            case KillfeedPacket.TYPE_KNIFE:
                bgColor = 0x80003366;
                break;
            case KillfeedPacket.TYPE_GRENADE:
                bgColor = 0x80663300;
                break;
            default:
                bgColor = bgAlpha | 0x000000;
                break;
        }
        guiGraphics.fill(left, top, right, bottom, bgColor);

        if (isLocalPlayerInvolved) {
            int borderColor = alphaBits | 0xFF0000;
            guiGraphics.fill(left - 1, top - 1, right + 1, top, borderColor);
            guiGraphics.fill(left - 1, bottom, right + 1, bottom + 1, borderColor);
            guiGraphics.fill(left - 1, top, left, bottom, borderColor);
            guiGraphics.fill(right, top, right + 1, bottom, borderColor);
        }

        int currentX = left + padX;
        int textY = y + 6;

        int killerColor = (entry.killerColor & 0xFFFFFF) | alphaBits;
        guiGraphics.drawString(font, killer, currentX, textY, killerColor, false);
        currentX += killerWidth;

        if (!streakText.isEmpty()) {
            currentX += smallGap;
            int sc = (streakColor & 0xFFFFFF) | alphaBits;
            guiGraphics.drawString(font, streakText, currentX, textY, sc, false);
            currentX += streakWidth;
        }

        currentX += gap;

        if (!entry.gunStack.isEmpty()) {
            guiGraphics.renderFakeItem(entry.gunStack, currentX, y + 2);
            currentX += iconSize + gap;
        }

        if (entry.isHeadshot) {
            currentX += smallGap;
            drawHeadshotIcon(guiGraphics, currentX, y + 6, alphaBits);
            currentX += hsIconWidth;
        }

        int victimColor = (entry.victimColor & 0xFFFFFF) | alphaBits;
        guiGraphics.drawString(font, victim, currentX, textY, victimColor, false);

        if (!assistText.isEmpty()) {
            int assistY = y + entryHeight - 2;
            int assistColor = (0xAAAAAA & 0xFFFFFF) | alphaBits;
            guiGraphics.drawString(font, assistText, left + padX, assistY, assistColor, false);
        }
    }

    private static void drawHeadshotIcon(GuiGraphics guiGraphics, int x, int y, int alphaBits) {
        int cx = x + 6;
        int cy = y + 6;
        int color = (0xFFD700 & 0xFFFFFF) | alphaBits;

        guiGraphics.fill(cx - 5, cy - 1, cx + 5, cy + 0, color);
        guiGraphics.fill(cx - 5, cy + 0, cx + 5, cy + 1, color);
        guiGraphics.fill(cx - 1, cy - 5, cx + 0, cy + 5, color);
        guiGraphics.fill(cx + 0, cy - 5, cx + 1, cy + 5, color);

        guiGraphics.fill(cx - 5, cy - 5, cx - 4, cy - 4, color);
        guiGraphics.fill(cx + 4, cy - 5, cx + 5, cy - 4, color);
        guiGraphics.fill(cx - 5, cy + 4, cx - 4, cy + 5, color);
        guiGraphics.fill(cx + 4, cy + 4, cx + 5, cy + 5, color);

        guiGraphics.fill(cx - 5, cy - 5, cx - 4, cy + 5, color);
        guiGraphics.fill(cx + 4, cy - 5, cx + 5, cy + 5, color);
        guiGraphics.fill(cx - 5, cy - 5, cx + 5, cy - 4, color);
        guiGraphics.fill(cx - 5, cy + 4, cx + 5, cy + 5, color);
    }

    static class KillfeedEntry {
        final String killerName;
        final int killerColor;
        final String victimName;
        final int victimColor;
        final ItemStack gunStack;
        final UUID killerUUID;
        final UUID victimUUID;
        final boolean isHeadshot;
        final int killStreak;
        final String assistName;
        final int killType;
        final long spawnTime;

        KillfeedEntry(KillfeedPacket packet) {
            this.killerName = packet.getKillerName();
            this.killerColor = packet.getKillerColor();
            this.victimName = packet.getVictimName();
            this.victimColor = packet.getVictimColor();
            this.gunStack = packet.getGunStack();
            this.killerUUID = packet.getKillerUUID();
            this.victimUUID = packet.getVictimUUID();
            this.isHeadshot = packet.isHeadshot();
            this.killStreak = packet.getKillStreak();
            this.assistName = packet.getAssistName();
            this.killType = packet.getKillType();
            this.spawnTime = System.currentTimeMillis();
        }
    }
}
