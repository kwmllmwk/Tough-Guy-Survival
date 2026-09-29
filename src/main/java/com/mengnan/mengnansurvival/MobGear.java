/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * 【13】可携带防具或武器的生物生成时，自带防具或武器的概率增加且必定附魔。
 *
 * <p>做法：在 FinalizeSpawnEvent 中检查怪物各个装备槽，按配置的概率倍率补上装备，
 * 然后给每件装备附上随机附魔。</p>
 */
public final class MobGear {

    private static final RandomSource RANDOM = RandomSource.create();

    /** 会自然生成装备的槽位。 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private MobGear() {}

    /** 为生物已有的装备补上附魔（可按倍率强化）。 */
    public static void applyEquipment(Mob mob, ServerLevel level) {
        double multiplier = MSConfig.COMMON.mobArmorChanceMultiplier.get();
        double enchantChance = MSConfig.COMMON.mobArmorEnchantChance.get();

        if (multiplier <= 0.0D && enchantChance <= 0.0D) {
            return;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack current = mob.getItemBySlot(slot);
            if (current.isEmpty()) {
                continue; // 原版没给装备，不强行新增，只强化已有的
            }
            // 装备已存在：直接按概率附魔
            tryEnchant(mob, slot, current, enchantChance);
        }

        // 手持武器同样必定附魔
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack held = mob.getItemBySlot(slot);
            if (!held.isEmpty() && isWeapon(held)) {
                tryEnchant(mob, slot, held, enchantChance);
            }
        }
    }

    /** 判断物品是否属于武器类（剑 / 斧 / 弓 / 三叉戟 / 弩）。 */
    private static boolean isWeapon(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.SWORDS)
                || stack.is(net.minecraft.tags.ItemTags.AXES)
                || stack.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE)
                || stack.is(net.minecraft.tags.ItemTags.TRIDENT_ENCHANTABLE)
                || stack.is(net.minecraft.tags.ItemTags.CROSSBOW_ENCHANTABLE);
    }

    /** 按概率给一件装备附上随机附魔。 */
    private static void tryEnchant(Mob mob, EquipmentSlot slot, ItemStack stack, double chance) {
        if (chance <= 0.0D || stack.isEmpty()) {
            return;
        }
        if (RANDOM.nextDouble() >= chance) {
            return;
        }
        if (!EnchantmentHelper.canStoreEnchantments(stack)) {
            return;
        }
        var registry = mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            int rolls = 1 + RANDOM.nextInt(3); // 1~3 条附魔
            for (int i = 0; i < rolls; i++) {
                Holder<Enchantment> enchantment = pickApplicable(registry, stack, RANDOM);
                if (enchantment == null) {
                    continue;
                }
                int maxLevel = enchantment.value().getMaxLevel();
                int minLevel = Math.max(1, enchantment.value().getMinLevel());
                int lvl = minLevel + RANDOM.nextInt(Math.max(1, maxLevel - minLevel + 1));
                mutable.set(enchantment, lvl);
            }
        });
    }

    /** 从附魔注册表里挑一个能用在当前物品上的附魔。 */
    private static Holder<Enchantment> pickApplicable(
            net.minecraft.core.Registry<Enchantment> registry, ItemStack stack, RandomSource random) {
        List<Holder.Reference<Enchantment>> all = registry.listElements().toList();
        if (all.isEmpty()) {
            return null;
        }
        // 最多尝试 20 次，避免死循环
        for (int attempt = 0; attempt < 20; attempt++) {
            Holder.Reference<Enchantment> candidate = all.get(random.nextInt(all.size()));
            if (candidate.value().canEnchant(stack)) {
                return candidate;
            }
        }
        return null;
    }
}
