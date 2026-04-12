package com.qisumei.c4;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.qisumei.c4.entity.C4Entity;
import com.qisumei.c4.entity.ModEntities;
import com.qisumei.c4.item.C4Item;
import com.qisumei.c4.sound.ModSounds;
import com.qisumei.c4.network.PacketHandler;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(qis4c4.MODID)
public class qis4c4 {
    public static final String MODID = "qis4c4";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 物品注册
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // 如果不需要方块，删除 BLOCKS 注册器
    // public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    // C4 物品
    public static final RegistryObject<C4Item> QISC4_ITEM =
            ITEMS.register("c4", C4Item::new);

    // 如果不需要方块，删除方块注册
    // public static final RegistryObject<Block> QIS_C4 = ...

    // C4 实体类型
    public static final RegistryObject<net.minecraft.world.entity.EntityType<C4Entity>> C4_ENTITY = ModEntities.C4_ENTITY;

    public qis4c4() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册网络包
        PacketHandler.register();

        // 注册内容
        ITEMS.register(modBus);
        // 如果删除 BLOCKS，注释掉这行
        // BLOCKS.register(modBus);
        ModSounds.register(modBus);
        ModEntities.register(modBus);

        LOGGER.info("Loaded mod {}", MODID);

        modBus.addListener(this::onBuildCreativeModeTabContents);
    }

    private void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(QISC4_ITEM.get());
        }
    }
}