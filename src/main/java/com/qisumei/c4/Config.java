package com.qisumei.c4;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = qis4c4.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_EXTRA =
            BUILDER
                    .comment("是否开启示例物品的额外效果")
                    .define("enableExtraEffect", true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_BLOCKS = BUILDER
            .comment("允许放置C4的方块ID列表（支持mod:block格式）",
                    "默认: minecraft:end_stone_bricks")
            .defineList("allowed_blocks",
                    Arrays.asList("minecraft:end_stone_bricks"),
                    obj -> obj instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableExtraEffect;
    public static List<? extends String> allowedBlocks = Arrays.asList("minecraft:end_stone_bricks");

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            enableExtraEffect = ENABLE_EXTRA.get();
            allowedBlocks = ALLOWED_BLOCKS.get();
            if (allowedBlocks == null || allowedBlocks.isEmpty()) {
                allowedBlocks = Arrays.asList("minecraft:end_stone_bricks");
            }
        }
    }

    public static boolean isBlockAllowed(String blockId) {
        return allowedBlocks != null && allowedBlocks.contains(blockId);
    }
}