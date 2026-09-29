/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 猛男生存 —— 籽岷《猛男生存》数据包的高版本 NeoForge 移植。
 *
 * <p>所有玩法改动都通过 NeoForge 事件总线实现，没有 mixin，兼容性好。
 * 所有概率与数值都可在游戏内「模组列表 -> 猛男生存 -> Config」中调整。</p>
 */
@Mod(MengnanSurvival.MODID)
public class MengnanSurvival {

    public static final String MODID = "mengnansurvival";

    public MengnanSurvival(IEventBus modBus, ModContainer container) {
        // 注册配置（游戏内可编辑）
        container.registerConfig(ModConfig.Type.COMMON, MSConfig.SPEC);

        // 注册配置界面工厂。
        // 缺少这一步时，模组列表里的 Config 按钮会打不开（NeoForge 需要显式提供界面工厂）。
        container.registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                net.neoforged.neoforge.client.gui.ConfigurationScreen::new);

        // 玩法逻辑挂在游戏事件总线上。
        // 注意：这里的监听方法全部是 static，因此必须传入 Class 而不是实例；
        // 传实例会让 NeoForge 要求方法为非 static，从而在启动时报
        // "Expected @SubscribeEvent method ... to NOT be static"。
        NeoForge.EVENT_BUS.register(PlayerEvents.class);
        NeoForge.EVENT_BUS.register(WorldEvents.class);
        NeoForge.EVENT_BUS.register(CombatEvents.class);
        // 追踪弹射物的逻辑在 HomingProjectiles 里。
        // 必须一并注册，否则那里的监听方法永远不会被调用（之前的追踪箭失效就是这个原因）。
        NeoForge.EVENT_BUS.register(HomingProjectiles.class);
    }
}
