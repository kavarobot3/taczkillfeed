package com.qisumei.c4.entity;

import com.qisumei.c4.ModInitializer;
import com.qisumei.c4.network.PacketHandler;
import com.qisumei.c4.network.UIMessagePacket;
import com.qisumei.c4.qis4c4;
import com.qisumei.c4.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class C4Entity extends Entity {

    private static final EntityDataAccessor<Integer> TICKS_LEFT =
            SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_PLAYER_PLACED =
            SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DEFUSING_PLAYER_UUID =
            SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> COUNTDOWN_SOUND_PLAYED =
            SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.BOOLEAN);

    private int nextBeepTick;
    private boolean announced20;
    private boolean announced4;

    private long defusingStartTime = 0;
    private static final int DEFUSE_DURATION_MS = 5000;
    private long lastProgressTime = 0;
    private BlockPos myPos;

    private static final float WIDTH = 0.7f;
    private static final float HEIGHT = 0.2f;

    public C4Entity(EntityType<? extends C4Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public C4Entity(Level level, BlockPos pos, boolean playerPlaced) {
        this(qis4c4.C4_ENTITY.get(), level);
        this.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
        this.myPos = pos;
        this.entityData.set(TICKS_LEFT, 20 * 40);
        this.entityData.set(IS_PLAYER_PLACED, playerPlaced);
        this.entityData.set(DEFUSING_PLAYER_UUID, "");
        this.entityData.set(COUNTDOWN_SOUND_PLAYED, false);
        this.nextBeepTick = calculateNextInterval(20 * 40);
        this.announced20 = false;
        this.announced4 = false;

        // 设置记分板状态和坐标
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModInitializer.updateC4State(server, ModInitializer.STATE_PLACED);
            ModInitializer.updateC4Position(server, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TICKS_LEFT, 20 * 40);
        this.entityData.define(IS_PLAYER_PLACED, true);
        this.entityData.define(DEFUSING_PLAYER_UUID, "");
        this.entityData.define(COUNTDOWN_SOUND_PLAYED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(TICKS_LEFT, tag.getInt("TicksLeft"));
        this.entityData.set(IS_PLAYER_PLACED, tag.getBoolean("PlayerPlaced"));
        this.nextBeepTick = tag.getInt("NextBeepTick");
        this.announced20 = tag.getBoolean("Announced20");
        this.announced4 = tag.getBoolean("Announced4");
        int x = tag.getInt("PosX");
        int y = tag.getInt("PosY");
        int z = tag.getInt("PosZ");
        this.myPos = new BlockPos(x, y, z);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TicksLeft", this.entityData.get(TICKS_LEFT));
        tag.putBoolean("PlayerPlaced", this.entityData.get(IS_PLAYER_PLACED));
        tag.putInt("NextBeepTick", this.nextBeepTick);
        tag.putBoolean("Announced20", this.announced20);
        tag.putBoolean("Announced4", this.announced4);
        if (myPos != null) {
            tag.putInt("PosX", myPos.getX());
            tag.putInt("PosY", myPos.getY());
            tag.putInt("PosZ", myPos.getZ());
        }
    }

    @Override
    public void tick() {
        super.tick();

        // 在服务端播放倒计时音效（只播放一次）
        if (!this.level().isClientSide && !this.entityData.get(COUNTDOWN_SOUND_PLAYED)) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.C4_COUNTDOWN.get(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            this.entityData.set(COUNTDOWN_SOUND_PLAYED, true);
        }

        if (this.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        int ticksLeft = this.entityData.get(TICKS_LEFT);

        String defusingPlayerUuid = this.entityData.get(DEFUSING_PLAYER_UUID);
        if (!defusingPlayerUuid.isEmpty() && defusingStartTime > 0) {
            Player player = serverLevel.getPlayerByUUID(UUID.fromString(defusingPlayerUuid));
            if (player != null && player.distanceTo(this) <= 3.0) {
                long elapsed = System.currentTimeMillis() - defusingStartTime;
                if (elapsed >= DEFUSE_DURATION_MS) {
                    defuse(player);
                    return;
                } else {
                    int currentSecond = (int)(elapsed / 1000);
                    int lastSecond = (int)(lastProgressTime / 1000);
                    if (currentSecond != lastSecond) {
                        int remaining = (int)((DEFUSE_DURATION_MS - elapsed) / 1000);
                        sendUIMessage("§e 拆除中... " + remaining + " 秒后完成 ", 1000);
                        lastProgressTime = System.currentTimeMillis();
                    }
                }
            } else {
                resetDefusing(player);
            }
        }

        if (ticksLeft <= 0) {
            explode();
            return;
        }

        ticksLeft--;
        this.entityData.set(TICKS_LEFT, ticksLeft);

        if (ticksLeft <= this.nextBeepTick) {
            playAlarmSound(serverLevel);
            this.nextBeepTick = ticksLeft - calculateNextInterval(ticksLeft);
        }

        if (!announced20 && ticksLeft <= 20 * 20) {
            broadcastMessage(serverLevel, "§e 还剩 20 秒。");
            announced20 = true;
        }

        if (!announced4 && ticksLeft <= 4 * 20) {
            broadcastMessage(serverLevel, "§c 炸弹即将爆炸！");
            announced4 = true;
        }
    }

    // 安全发送UI消息（只在服务端执行）
    private void sendUIMessage(String message, int duration) {
        if (!this.level().isClientSide) {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new UIMessagePacket(message, duration));
        }
    }

    @Override
    @Nonnull
    public AABB getBoundingBoxForCulling() {
        return new AABB(this.getX() - WIDTH/2, this.getY(), this.getZ() - WIDTH/2,
                this.getX() + WIDTH/2, this.getY() + HEIGHT, this.getZ() + WIDTH/2);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.8f;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    public void startDefusing(Player player) {
        if (!player.getMainHandItem().is(Items.SHEARS)) return;

        String currentDefusingPlayer = this.entityData.get(DEFUSING_PLAYER_UUID);
        if (!currentDefusingPlayer.isEmpty() && defusingStartTime > 0) {
            if (currentDefusingPlayer.equals(player.getUUID().toString())) {
                return;
            }
        }

        this.entityData.set(DEFUSING_PLAYER_UUID, player.getUUID().toString());
        this.defusingStartTime = System.currentTimeMillis();
        this.lastProgressTime = System.currentTimeMillis();

        // 发送开始拆除提示
        sendUIMessage("§e 开始拆除C4... 5 秒后自动拆除 ", 3000);
    }

    public void resetDefusing(Player player) {
        String currentDefusingPlayer = this.entityData.get(DEFUSING_PLAYER_UUID);
        if (currentDefusingPlayer.isEmpty() || defusingStartTime == 0) return;

        if (currentDefusingPlayer.equals(player.getUUID().toString())) {
            sendUIMessage("§c 拆除中断！进度已重置 ", 2000);
        }

        this.entityData.set(DEFUSING_PLAYER_UUID, "");
        this.defusingStartTime = 0;
        this.lastProgressTime = 0;
    }

    private void defuse(Player player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModInitializer.updateC4State(server, ModInitializer.STATE_DEFUSED);
            ModInitializer.clearC4Position(server);

            // 静默停止倒计时音效
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(),
                    "stopsound @a * qis4c4:c4countdown"
            );
        }

        sendUIMessage("§a 炸弹已被拆除！", 2000);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.CTW_SOUND.get(),
                SoundSource.BLOCKS, 1.5f, 1.0f);

        this.discard();
    }

    private void explode() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModInitializer.updateC4State(server, ModInitializer.STATE_EXPLODED);
            ModInitializer.clearC4Position(server);

            // 静默停止倒计时音效
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(),
                    "stopsound @a * qis4c4:c4countdown"
            );
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.TW_SOUND.get(),
                SoundSource.BLOCKS, 5.0f, 0.8f);

        this.level().explode(null, this.getX(), this.getY(), this.getZ(),
                4.0f, Level.ExplosionInteraction.NONE);

        this.discard();
    }

    private void playAlarmSound(ServerLevel world) {
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.ALARM_SOUND(),
                SoundSource.BLOCKS, 1.5f, 1.0f);
    }

    private void broadcastMessage(ServerLevel world, String message) {
        world.getServer().getPlayerList().getPlayers().forEach(p ->
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(message))
        );
    }

    private int calculateNextInterval(int remainingTicks) {
        float progress = 1 - (remainingTicks / (40f * 20));
        return Math.max(8, (int)(40 * (1 - progress * 0.8f)));
    }

    @SuppressWarnings("unused")
    public int getRemainingTicks() {
        return this.entityData.get(TICKS_LEFT);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}