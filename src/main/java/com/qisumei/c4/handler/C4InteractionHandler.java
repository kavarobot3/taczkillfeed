package com.qisumei.c4.handler;

import com.qisumei.c4.entity.C4Entity;
import com.qisumei.c4.qis4c4;
import com.qisumei.c4.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = qis4c4.MODID)
public class C4InteractionHandler {

    private static final Map<UUID, C4Entity> defusingPlayers = new HashMap<>();
    private static final double MAX_DISTANCE = 4.0;

    private static C4Entity getLookedAtC4(Player player) {
        double reachDistance = MAX_DISTANCE;
        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 endPosition = eyePosition.add(lookVector.x * reachDistance, lookVector.y * reachDistance, lookVector.z * reachDistance);

        AABB boundingBox = player.getBoundingBox().inflate(reachDistance);
        var entities = player.level().getEntities(player, boundingBox, e -> e instanceof C4Entity);

        C4Entity closest = null;
        double closestDistance = reachDistance + 1;

        for (var entity : entities) {
            C4Entity c4 = (C4Entity) entity;
            var hitResult = c4.getBoundingBox().clip(eyePosition, endPosition);
            if (hitResult.isPresent()) {
                double distance = eyePosition.distanceTo(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = c4;
                }
            }
        }

        return closest;
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();

        if (!player.getMainHandItem().is(Items.SHEARS)) {
            return;
        }

        C4Entity lookedAtC4 = getLookedAtC4(player);

        if (lookedAtC4 != null) {
            event.setCanceled(true);
            UUID playerId = player.getUUID();
            lookedAtC4.startDefusing(player);
            defusingPlayers.put(playerId, lookedAtC4);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        UUID playerId = player.getUUID();
        C4Entity c4 = defusingPlayers.get(playerId);

        if (c4 != null && !c4.isRemoved()) {
            boolean hasShears = player.getMainHandItem().is(Items.SHEARS);
            C4Entity lookedAtC4 = getLookedAtC4(player);
            boolean isLookingAtCorrectC4 = (lookedAtC4 == c4);

            if (hasShears && isLookingAtCorrectC4) {
                // 继续拆除
            } else {
                c4.resetDefusing(player);
                defusingPlayers.remove(playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        cancelDefusing(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        cancelDefusing(event.getEntity());
    }

    private static void cancelDefusing(Player player) {
        UUID playerId = player.getUUID();
        C4Entity c4 = defusingPlayers.remove(playerId);
        if (c4 != null && !c4.isRemoved()) {
            c4.resetDefusing(player);
        }
    }
}