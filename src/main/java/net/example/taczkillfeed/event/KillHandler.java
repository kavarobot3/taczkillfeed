package net.example.taczkillfeed.event;

import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import net.example.taczkillfeed.TaczKillfeed;
import net.example.taczkillfeed.network.KillfeedPacket;
import net.example.taczkillfeed.network.ModMessages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = TaczKillfeed.MOD_ID)
public class KillHandler {
    private static final Map<UUID, Integer> killStreaks = new HashMap<>();
    private static final Map<UUID, Map<UUID, Float>> damageTracker = new HashMap<>();
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final long ASSIST_TIMEOUT_MS = 5000L;
    private static final Set<UUID> gunProcessed = new HashSet<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker == victim) return;

        UUID vid = victim.getUUID();
        UUID aid = attacker.getUUID();

        damageTracker.computeIfAbsent(vid, k -> new HashMap<>()).merge(aid, event.getAmount(), Float::sum);
        lastDamageTime.put(vid, System.currentTimeMillis());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer == victim) return;

        UUID vid = victim.getUUID();
        UUID kid = killer.getUUID();

        if (gunProcessed.contains(vid)) {
            gunProcessed.remove(vid);
            cleanStreakOnDeath(victim);
            return;
        }

        int streak = killStreaks.merge(kid, 1, (old, one) -> old + 1);
        cleanStreakOnDeath(victim);

        String assistName = findAssist(vid, kid);

        int killType = KillfeedPacket.TYPE_KNIFE;
        ItemStack weapon = killer.getMainHandItem();

        if (event.getSource().is(DamageTypes.EXPLOSION) || event.getSource().is(DamageTypes.PLAYER_EXPLOSION)) {
            killType = KillfeedPacket.TYPE_GRENADE;
        } else if (isKnife(weapon)) {
            killType = KillfeedPacket.TYPE_KNIFE;
        } else {
            killType = KillfeedPacket.TYPE_KNIFE;
        }

        int killerColor = getTeamColor(killer);
        int victimColor = getTeamColor(victim);

        String killerName = killer.getDisplayName().getString();
        String victimName = victim.getDisplayName().getString();
        if (killerName.length() > 100) killerName = killerName.substring(0, 100);
        if (victimName.length() > 100) victimName = victimName.substring(0, 100);

        KillfeedPacket packet = new KillfeedPacket(
                killerName, killerColor, victimName, victimColor,
                weapon, kid, vid, false,
                streak, assistName, killType
        );

        TaczKillfeed.LOGGER.info("Killfeed: {} killed {} (streak={}, type={})", killerName, victimName, streak, killType);
        sendPacket(packet, killer);
    }

    @SubscribeEvent
    public static void onGunKill(EntityKillByGunEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        LivingEntity attacker = event.getAttacker();
        LivingEntity victim = event.getKilledEntity();
        if (!(attacker instanceof Player) || !(victim instanceof Player)) return;

        UUID vid = victim.getUUID();
        UUID kid = attacker.getUUID();
        gunProcessed.add(vid);
        if (gunProcessed.size() > 200) gunProcessed.clear();

        int streak = killStreaks.merge(kid, 1, (old, one) -> old + 1);
        cleanStreakOnDeath(victim);

        String assistName = findAssist(vid, kid);

        boolean isHeadshot = event.isHeadShot();
        ResourceLocation gunId = event.getGunId();

        ItemStack gunStack = ItemStack.EMPTY;
        ItemStack mainHand = attacker.getMainHandItem();
        ResourceLocation handId = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
        if (handId != null && handId.equals(gunId)) {
            gunStack = mainHand.copy();
        }
        if (gunStack.isEmpty() && gunId != null) {
            Item gunItem = ForgeRegistries.ITEMS.getValue(gunId);
            if (gunItem != null && gunItem != Items.AIR) gunStack = new ItemStack(gunItem);
        }

        int killerColor = getTeamColor(attacker);
        int victimColor = getTeamColor(victim);

        String killerName = attacker.getDisplayName().getString();
        String victimName = victim.getDisplayName().getString();
        if (killerName.length() > 100) killerName = killerName.substring(0, 100);
        if (victimName.length() > 100) victimName = victimName.substring(0, 100);

        KillfeedPacket packet = new KillfeedPacket(
                killerName, killerColor, victimName, victimColor,
                gunStack, kid, vid, isHeadshot,
                streak, assistName, KillfeedPacket.TYPE_GUN
        );

        TaczKillfeed.LOGGER.info("Killfeed: {} killed {} (streak={})", killerName, victimName, streak);
        sendPacket(packet, event.getAttacker());
    }

    private static void cleanStreakOnDeath(LivingEntity victim) {
        killStreaks.remove(victim.getUUID());
        damageTracker.remove(victim.getUUID());
        lastDamageTime.remove(victim.getUUID());
    }

    private static String findAssist(UUID victimId, UUID killerId) {
        Map<UUID, Float> damageMap = damageTracker.remove(victimId);
        if (damageMap == null || damageMap.isEmpty()) return "";

        long now = System.currentTimeMillis();
        Long lastTime = lastDamageTime.remove(victimId);
        if (lastTime == null || now - lastTime > ASSIST_TIMEOUT_MS) return "";

        UUID bestAssist = null;
        float bestDamage = 0f;
        for (var entry : damageMap.entrySet()) {
            if (entry.getKey().equals(killerId)) continue;
            if (entry.getValue() > bestDamage) {
                bestDamage = entry.getValue();
                bestAssist = entry.getKey();
            }
        }
        if (bestAssist == null || bestDamage < 5f) return "";

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "";
        var player = server.getPlayerList().getPlayer(bestAssist);
        return player != null ? player.getDisplayName().getString() : "";
    }

    private static void sendPacket(KillfeedPacket packet, LivingEntity source) {
        if (source.getServer() == null) return;
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        ModMessages.sendToCapableClients(packet, players);
    }

    private static boolean isKnife(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        String path = id.getPath().toLowerCase();
        return path.contains("knife") || path.contains("bayonet") || path.contains("m9") || path.contains("karambit")
                || path.contains("dagger") || path.contains("blade") || path.contains("katana")
                || path.contains("tac_knife");
    }

    private static int getTeamColor(LivingEntity entity) {
        if (entity.getTeam() != null) {
            String teamName = entity.getTeam().getName();
            if (teamName.equalsIgnoreCase("ter")) return 0xEAD16F;
            if (teamName.equalsIgnoreCase("konter")) return 0x7092BE;
        }
        return 0xCCCCCC;
    }
}
