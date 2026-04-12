package com.qisumei.c4.handler;

import com.qisumei.c4.ModInitializer;  // 新增导入
import com.qisumei.c4.qis4c4;
import com.qisumei.c4.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = qis4c4.MODID)
public class C4CountdownHandler {
    private static class Countdown {
        int ticksLeft;
        int nextBeepTick;
        final BlockPos pos;
        final boolean playerPlaced;
        boolean announced20;
        boolean announced4;

        Countdown(BlockPos pos, boolean playerPlaced) {
            this.pos = pos;
            this.playerPlaced = playerPlaced;
            this.ticksLeft = 20 * 40;
            this.nextBeepTick = calculateNextInterval(ticksLeft);
        }
    }

    private static final Map<BlockPos, Countdown> countdowns = new ConcurrentHashMap<>();

    public static int getRemainingTicks(BlockPos pos) {
        Countdown c = countdowns.get(pos);
        return c != null ? c.ticksLeft : -1;
    }

    public static boolean isCounting(BlockPos pos) {
        return countdowns.containsKey(pos);
    }

    public static void startCountdown(BlockPos pos, ServerLevel world, boolean playerPlaced) {
        countdowns.remove(pos);

        Countdown c = new Countdown(pos, playerPlaced);
        countdowns.put(pos, c);

        // 更新记分板状态：已安装
        MinecraftServer server = world.getServer();
        ModInitializer.updateC4State(server, ModInitializer.STATE_PLACED);

        world.getServer().getPlayerList().getPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal("§c 炸弹已在 " + pos.toShortString() + " 安放，40 秒后爆炸！"))
        );

        playAlarmSound(pos, world);
    }

    public static void onBlockDestroyed(BlockPos pos, ServerLevel world, boolean byPlayer) {
        Countdown c = countdowns.remove(pos);
        if (c != null) {
            if (c.playerPlaced && byPlayer) {
                world.playSound(null,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ModSounds.CTW_SOUND.get(),
                        SoundSource.BLOCKS, 1.5f, 1.0f);

                // 更新记分板状态：已拆除
                ModInitializer.updateC4State(world.getServer(), ModInitializer.STATE_DEFUSED);

                world.getServer().getPlayerList().getPlayers().forEach(p ->
                        p.sendSystemMessage(Component.literal("§a 炸弹已被拆除！"))
                );
            }
        }
    }

    private static void playAlarmSound(BlockPos pos, ServerLevel world) {
        world.playSound(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                ModSounds.ALARM_SOUND(),
                SoundSource.BLOCKS, 1.5f, 1.0f);
    }

    private static int calculateNextInterval(int remainingTicks) {
        float progress = 1 - (remainingTicks / (40f * 20));
        return Math.max(8, (int)(40 * (1 - progress * 0.8f)));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            ServerLevel world = server.getLevel(server.overworld().dimension());
            if (world == null) return;

            Iterator<Map.Entry<BlockPos, Countdown>> iter = countdowns.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<BlockPos, Countdown> entry = iter.next();
                Countdown c = entry.getValue();

                if (--c.ticksLeft <= 0) {
                    explodeC4(c.pos, world);
                    iter.remove();
                    continue;
                }

                if (c.ticksLeft <= c.nextBeepTick) {
                    playAlarmSound(c.pos, world);
                    c.nextBeepTick = c.ticksLeft - calculateNextInterval(c.ticksLeft);
                }

                if (!c.announced20 && c.ticksLeft <= 20 * 20) {
                    broadcastMessage(world, "§e 还剩 20 秒。");
                    c.announced20 = true;
                }

                if (!c.announced4 && c.ticksLeft <= 4 * 20) {
                    broadcastMessage(world, "§c C4即將爆炸！");
                    c.announced4 = true;
                }
            }
        }
    }

    private static void explodeC4(BlockPos pos, ServerLevel world) {
        // 创建爆炸效果，破坏地形
        world.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                4.0f, net.minecraft.world.level.Level.ExplosionInteraction.TNT);

        // 更新记分板状态：已爆炸
        ModInitializer.updateC4State(world.getServer(), ModInitializer.STATE_EXPLODED);

        broadcastMessage(world, "§c 炸弹已爆炸！");
    }

    private static void broadcastMessage(ServerLevel world, String message) {
        world.getServer().getPlayerList().getPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal(message))
        );
    }
}