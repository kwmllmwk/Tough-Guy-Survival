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
        // 注册自定义药水（女巫投掷的凋零药水）
        MSPotions.register(modBus);

        // 注册配置（游戏内可编辑）。
        // 文件名显式指定，避免随 NeoForge 版本变化（旧版叫 -common.toml，新版会变成 -local.toml）。
        container.registerConfig(resolveConfigType(), MSConfig.SPEC, "mengnansurvival-common.toml");

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
        // 第三批新增玩法（第 36 ~ 45 项）
        NeoForge.EVENT_BUS.register(ExtraEvents.class);
    }

    /**
     * 取得「本地配置文件」的配置类型，兼容新旧 NeoForge。
     *
     * <p>FancyModLoader 12.0.8 起把 {@code ModConfig.Type} 的取值改名了：</p>
     * <ul>
     *   <li>12.0.7 及更早（NeoForge 26.3.0.35-beta 及更早）：{@code COMMON / SERVER / CLIENT / STARTUP}</li>
     *   <li>12.0.8 及以后（NeoForge 26.3.0.37-beta 起）：{@code LOCAL / SYNCED / CLIENT / STARTUP}</li>
     * </ul>
     *
     * <p>如果在字节码里直接写死某个常量，换一个 NeoForge 版本就会抛
     * {@code NoSuchFieldError} 而无法启动。所以这里按名字反射查找：
     * 先找 {@code COMMON}，找不到再找 {@code LOCAL}。</p>
     */
    private static ModConfig.Type resolveConfigType() {
        for (String name : new String[]{"COMMON", "LOCAL"}) {
            try {
                return ModConfig.Type.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // 当前 NeoForge 没有这个常量，继续试下一个
            }
        }
        // 兜底：STARTUP 在两个版本里都存在（只是不支持游戏内热重载）
        return ModConfig.Type.STARTUP;
    }
}
