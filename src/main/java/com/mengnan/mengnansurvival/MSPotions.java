/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组自定义的药水。
 *
 * <p>Java 版原版<b>没有</b>凋零药水（「衰变药水」是基岩版独占内容，
 * {@code Potions} 里只有 WEAVING / OOZING / INFESTED 等试炼密室药水），
 * 因此这里自行注册一个凋零药水，供女巫投掷使用。</p>
 */
public final class MSPotions {

    /** 药水注册器。 */
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, MengnanSurvival.MODID);

    /**
     * 凋零药水：凋零 I，持续 20 秒。
     *
     * <p>名称 "wither" 会拼成翻译键 {@code item.minecraft.splash_potion.effect.wither}，
     * 对应的中文名写在 {@code assets/mengnansurvival/lang/zh_cn.json} 里。</p>
     */
    public static final DeferredHolder<Potion, Potion> WITHER =
            POTIONS.register("wither", () -> new Potion("wither",
                    new MobEffectInstance(MobEffects.WITHER, 400, 0)));

    private MSPotions() {}

    /** 在模组总线上注册（由主类调用）。 */
    public static void register(IEventBus modBus) {
        POTIONS.register(modBus);
    }
}
