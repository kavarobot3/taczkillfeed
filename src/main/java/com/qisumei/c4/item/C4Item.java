package com.qisumei.c4.item;

import com.qisumei.c4.Config;
import com.qisumei.c4.client.ClientMessageRenderer;
import com.qisumei.c4.entity.C4Entity;
import com.qisumei.c4.qis4c4;
import com.qisumei.c4.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.List;

public class C4Item extends Item {
    private static final int USE_DURATION = 70;
    private static final int CANCEL_THRESHOLD = 10;
    private static final ThreadLocal<Boolean> isCompleted = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> wasSlowed = ThreadLocal.withInitial(() -> false);

    // 安装音效实例（用于控制停止）
    private static SimpleSoundInstance plantSound = null;

    public C4Item() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.BLOCK;
    }

    private static Block getStandingBlock(Player player, Level world) {
        BlockPos playerPos = player.blockPosition();
        BlockPos belowPos = playerPos.below();
        BlockState belowState = world.getBlockState(belowPos);

        if (!belowState.isSolid()) {
            belowPos = belowPos.below();
            belowState = world.getBlockState(belowPos);
        }

        return belowState.getBlock();
    }

    // 播放安装音效（客户端）
    @OnlyIn(Dist.CLIENT)
    private static void playPlantSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 停止之前的音效
        stopPlantSound();

        // 播放新音效
        plantSound = SimpleSoundInstance.forUI(ModSounds.C4_PLANT.get(), 1.0f, 1.0f);
        mc.getSoundManager().play(plantSound);
    }

    // 停止安装音效
    @OnlyIn(Dist.CLIENT)
    private static void stopPlantSound() {
        if (plantSound != null) {
            Minecraft.getInstance().getSoundManager().stop(plantSound);
            plantSound = null;
        }
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 客户端播放安装音效
        if (world.isClientSide) {
            playPlantSound();
        }

        Block standingBlock = getStandingBlock(player, world);
        String blockId = ForgeRegistries.BLOCKS.getKey(standingBlock).toString();

        List<? extends String> allowedBlocks = Config.allowedBlocks;
        if (allowedBlocks == null || !allowedBlocks.contains(blockId)) {
            String allowedList = allowedBlocks != null ? String.join(", ", allowedBlocks) : "minecraft:end_stone_bricks";
            ClientMessageRenderer.showMessage(
                    Component.literal("§c C4只能放置在指定方块上！" ),
                    3000
            );
            if (world.isClientSide) {
                stopPlantSound();
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        isCompleted.set(false);
        wasSlowed.set(false);


        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack stack, @Nonnull Level world, @Nonnull LivingEntity user, int timeCharged) {
        if (!(user instanceof Player player)) return;

        // 客户端停止安装音效
        if (world.isClientSide) {
            stopPlantSound();
        }

        wasSlowed.set(false);

        if (isCompleted.get()) {
            isCompleted.remove();
            wasSlowed.remove();
            return;
        }

        int usedTicks = USE_DURATION - timeCharged;

        if (usedTicks < CANCEL_THRESHOLD) {
            ClientMessageRenderer.showMessage(
                    Component.literal("§c 安装已取消！ "),
                    2000
            );
        } else if (usedTicks < USE_DURATION) {
            ClientMessageRenderer.showMessage(
                    Component.literal("§c 安装中断！"),
                    2000
            );
        }

        isCompleted.remove();
        wasSlowed.remove();
    }

    @Override
    public void onUseTick(@Nonnull Level world, @Nonnull LivingEntity user, @Nonnull ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player) || world.isClientSide) return;

        int usedTicks = USE_DURATION - remainingUseTicks;

        if (usedTicks > 0 && usedTicks < USE_DURATION) {
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.setSprinting(false);
            player.hurtMarked = true;
            wasSlowed.set(true);
        }

        if (remainingUseTicks == 1) {
            isCompleted.set(true);
            wasSlowed.set(false);

            // 客户端停止安装音效（安装完成）
            if (world.isClientSide) {
                stopPlantSound();
            }

            Block standingBlock = getStandingBlock(player, world);
            String blockId = ForgeRegistries.BLOCKS.getKey(standingBlock).toString();

            List<? extends String> allowedBlocks = Config.allowedBlocks;
            if (allowedBlocks == null || !allowedBlocks.contains(blockId)) {

                return;
            }

            BlockPos placePos = player.blockPosition();

            if (!world.getBlockState(placePos).canBeReplaced()) {
                placePos = placePos.above();
                if (!world.getBlockState(placePos).canBeReplaced()) {
                    ClientMessageRenderer.showMessage(
                            Component.literal("§c 无法在此位置安装C4！ "),
                            2000
                    );
                    return;
                }
            }

            world.playSound(null, placePos,
                    SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.BLOCKS, 0.8f, 0.9f);

            if (world instanceof ServerLevel serverLevel) {
                C4Entity c4Entity = new C4Entity(world, placePos, true);
                serverLevel.addFreshEntity(c4Entity);
            }

            ClientMessageRenderer.showMessage(
                    Component.literal("§a C4已安装在脚下！"),
                    3000
            );

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        } else if (usedTicks % 10 == 0 && usedTicks > 0) {
            if (player.getMainHandItem().getItem() instanceof C4Item) {
                int remainingSeconds = (remainingUseTicks / 20) + 1;
                ClientMessageRenderer.showMessage(
                        Component.literal("§e 安装中... " + remainingSeconds + " 秒后完成 "),
                        2000
                );
            }
        }
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack) {
        return USE_DURATION;
    }
}