package net.example.taczkillfeed.network;

import net.example.taczkillfeed.client.KillfeedOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class KillfeedPacket {
    public static final int TYPE_GUN = 0;
    public static final int TYPE_KNIFE = 1;
    public static final int TYPE_GRENADE = 2;

    private final String killerName;
    private final int killerColor;
    private final String victimName;
    private final int victimColor;
    private final ItemStack gunStack;
    private final UUID killerUUID;
    private final UUID victimUUID;
    private final boolean isHeadshot;
    private final int killStreak;
    private final String assistName;
    private final int killType;

    public KillfeedPacket(String killerName, int killerColor, String victimName, int victimColor,
                          ItemStack gunStack, UUID killerUUID, UUID victimUUID, boolean isHeadshot,
                          int killStreak, String assistName, int killType) {
        this.killerName = killerName != null ? killerName : "?";
        this.killerColor = killerColor;
        this.victimName = victimName != null ? victimName : "?";
        this.victimColor = victimColor;
        this.gunStack = gunStack != null ? gunStack : ItemStack.EMPTY;
        this.killerUUID = killerUUID != null ? killerUUID : new UUID(0, 0);
        this.victimUUID = victimUUID != null ? victimUUID : new UUID(0, 0);
        this.isHeadshot = isHeadshot;
        this.killStreak = killStreak;
        this.assistName = assistName != null ? assistName : "";
        this.killType = killType;
    }

    public static void encode(KillfeedPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.killerName);
        buf.writeInt(msg.killerColor);
        buf.writeUtf(msg.victimName);
        buf.writeInt(msg.victimColor);
        buf.writeItem(msg.gunStack);
        buf.writeUUID(msg.killerUUID);
        buf.writeUUID(msg.victimUUID);
        buf.writeBoolean(msg.isHeadshot);
        buf.writeInt(msg.killStreak);
        buf.writeUtf(msg.assistName);
        buf.writeInt(msg.killType);
    }

    public static KillfeedPacket decode(FriendlyByteBuf buf) {
        String killer = buf.readUtf();
        int kColor = buf.readInt();
        String victim = buf.readUtf();
        int vColor = buf.readInt();
        ItemStack gunStack = buf.readItem();
        UUID killerUUID = buf.readUUID();
        UUID victimUUID = buf.readUUID();
        boolean isHeadshot = buf.readBoolean();
        int killStreak = buf.readInt();
        String assistName = buf.readUtf();
        int killType = buf.readInt();
        return new KillfeedPacket(killer, kColor, victim, vColor, gunStack, killerUUID, victimUUID, isHeadshot, killStreak, assistName, killType);
    }

    public static void handle(KillfeedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.runWhenOn(Dist.CLIENT, () -> () ->
                        KillfeedOverlay.addEntry(msg)
                )
        );
        ctx.get().setPacketHandled(true);
    }

    public String getKillerName() { return killerName; }
    public int getKillerColor() { return killerColor; }
    public String getVictimName() { return victimName; }
    public int getVictimColor() { return victimColor; }
    public ItemStack getGunStack() { return gunStack; }
    public UUID getKillerUUID() { return killerUUID; }
    public UUID getVictimUUID() { return victimUUID; }
    public boolean isHeadshot() { return isHeadshot; }
    public int getKillStreak() { return killStreak; }
    public String getAssistName() { return assistName; }
    public int getKillType() { return killType; }
}
