package com.qisumei.c4.client;

import com.qisumei.c4.qis4c4;
import com.qisumei.c4.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = qis4c4.MODID)
public class ClientMessageRenderer {

    private static class MessageData {
        String text;
        long expireTime;

        MessageData(String text, long expireTime) {
            this.text = text;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    private static final Map<UUID, MessageData> currentMessages = new HashMap<>();

    private static long lastRenderTime = 0;
    private static String cachedText = "";
    private static int cachedX = 0;
    private static int cachedY = 0;
    private static final long UPDATE_INTERVAL_MS = 500;
    private static final int Y_OFFSET = +30;

    // 倒计时音效实例
    private static SimpleSoundInstance countdownSound = null;

    public static void showMessage(Component message, int durationMs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID playerId = mc.player.getUUID();
        long expireTime = System.currentTimeMillis() + durationMs;
        currentMessages.put(playerId, new MessageData(message.getString(), expireTime));
        updateCache(mc);
    }

    public static void showMessage(Component message) {
        showMessage(message, 2000);
    }

    public static void clearMessage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            currentMessages.remove(mc.player.getUUID());
        }
    }

    // 播放倒计时音效
    public static void playCountdownSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (countdownSound == null) {
            SoundEvent soundEvent = ModSounds.C4_COUNTDOWN.get();
            countdownSound = SimpleSoundInstance.forUI(soundEvent, 1.0f, 1.0f);
            mc.getSoundManager().play(countdownSound);
        }
    }

    // 停止倒计时音效
    public static void stopCountdownSound() {
        if (countdownSound != null) {
            Minecraft.getInstance().getSoundManager().stop(countdownSound);
            countdownSound = null;
        }
    }

    private static void updateCache(Minecraft mc) {
        if (mc.player == null) return;

        UUID playerId = mc.player.getUUID();
        MessageData msg = currentMessages.get(playerId);

        if (msg == null || msg.isExpired()) {
            if (msg != null && msg.isExpired()) {
                currentMessages.remove(playerId);
            }
            cachedText = "";
            return;
        }

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth = font.width(msg.text);
        cachedX = (screenWidth - textWidth) / 2;
        cachedY = mc.getWindow().getGuiScaledHeight() / 2 + Y_OFFSET;
        cachedText = msg.text;
        lastRenderTime = System.currentTimeMillis();
    }

    private static void cleanExpiredMessages() {
        currentMessages.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        cleanExpiredMessages();

        UUID playerId = mc.player.getUUID();
        MessageData msg = currentMessages.get(playerId);

        if (msg == null || msg.isExpired()) {
            cachedText = "";
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRenderTime >= UPDATE_INTERVAL_MS) {
            updateCache(mc);
        }

        if (cachedText.isEmpty()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        guiGraphics.drawString(font, cachedText, cachedX, cachedY, 0xFFFFFF, false);
    }
}