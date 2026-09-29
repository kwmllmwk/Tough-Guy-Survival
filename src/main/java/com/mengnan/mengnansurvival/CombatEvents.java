/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * 战斗与敌对生物相关的改动。
 */
public final class CombatEvents {

    private static final java.util.Random RNG = new java.util.Random();

    // ==========================================================
    // 【5】自然生成的苦力怕变成闪电苦力怕
    // ==========================================================

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof Creeper creeper)) {
            return;
        }
        if (!(creeper.level() instanceof ServerLevel level)) {
            return;
        }
        if (creeper.isPowered()) {
            return;
        }
        double chance = MSConfig.COMMON.chargedCreeperConvertChance.get();
        if (chance <= 0.0D || RNG.nextDouble() >= chance) {
            return;
        }
        // 26.3 没有公开的 setPowered；原版 thunderHit 会把 powered 置为 true，直接调用它
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.snapTo(creeper.getX(), creeper.getY(), creeper.getZ(), 0.0F, 0.0F);
            creeper.thunderHit(level, bolt);
        }
    }

    // ==========================================================
    // 【5】闪电苦力怕：靠近玩家即被点燃 + 自带 1 小时隐身
    // 【23】末影人激怒距离
    // 【15】猪灵无视金装，仍然攻击玩家
    // ==========================================================

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long time = level.getGameTime();
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        // 每 10 刻处理一次与玩家相关的判定
        if (time % 10L == 0L) {
            double igniteRange = MSConfig.COMMON.chargedCreeperIgniteRange.get();
            double angerRange = MSConfig.COMMON.endermanAngerRange.get();
            double piglinRange = MSConfig.COMMON.piglinAngerRange.get();
            boolean piglinIgnoreGold = MSConfig.COMMON.piglinIgnoreGoldArmor.get();

            for (ServerPlayer player : players) {
                // 【5】点燃靠近的闪电苦力怕
                for (Creeper creeper : level.getEntitiesOfClass(
                        Creeper.class, player.getBoundingBox().inflate(igniteRange))) {
                    if (!creeper.isPowered()) {
                        continue;
                    }
                    // 点燃状态是持久的，不会因玩家远离而恢复
                    creeper.ignite();
                }

                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }

                // 【23】直接激怒附近的末影人
                for (Enderman enderman : level.getEntitiesOfClass(
                        Enderman.class, player.getBoundingBox().inflate(angerRange))) {
                    if (enderman.isAlive()) {
                        enderman.setTarget(player);
                    }
                }

                // 【15】猪灵：即使玩家穿金装也主动攻击
                if (piglinIgnoreGold) {
                    boolean angered = false;
                    for (Piglin piglin : level.getEntitiesOfClass(
                            Piglin.class, player.getBoundingBox().inflate(piglinRange))) {
                        if (!piglin.isAlive()) {
                            continue;
                        }
                        // 原版会因金装把玩家从目标记忆中排除，导致相关事件根本不触发；
                        // 这里直接设置攻击目标，绕过那套记忆。
                        if (piglin.getTarget() != player) {
                            piglin.setTarget(player);
                        }
                        angered = true;
                    }
                    // 让附近的猪灵一起被激怒，更接近原版行为（每个玩家只调用一次）
                    if (angered && PiglinAi.isWearingSafeArmor(player)) {
                        PiglinAi.angerNearbyPiglins(level, player, false);
                    }
                }
            }
        }

        // 每 40 刻把所有闪电苦力怕的隐身补满到 1 小时（覆盖天然被雷劈中的情况）
        if (time % 40L == 0L) {
            int invisTicks = MSConfig.COMMON.chargedCreeperInvisibilityTicks.get();
            for (ServerPlayer player : players) {
                for (Creeper creeper : level.getEntitiesOfClass(
                        Creeper.class, player.getBoundingBox().inflate(96.0D))) {
                    if (!creeper.isPowered()) {
                        continue;
                    }
                    var existing = creeper.getEffect(MobEffects.INVISIBILITY);
                    if (existing == null || existing.getDuration() < invisTicks - 100) {
                        creeper.addEffect(new MobEffectInstance(
                                MobEffects.INVISIBILITY, invisTicks, 0, false, false));
                    }
                }
            }
        }
    }

    // ==========================================================
    // 【33】只要在下雨，就必定是雷暴
    // 【26】雷暴时，附近没有有效避雷针就必定劈玩家（无视头顶遮挡）
    // ==========================================================

    @SubscribeEvent
    public static void onWeatherTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long time = level.getGameTime();

        // ---- 【33】雨天必定雷暴 ----
        if (time % 40L == 0L && MSConfig.COMMON.thunderWhileRaining.get()) {
            var weather = level.getWeatherData();
            if (weather.isRaining() && !weather.isThundering()) {
                weather.setThundering(true);
                // 给一个足够长的雷暴剩余时间，避免立刻结束
                if (weather.getThunderTime() < 6000) {
                    weather.setThunderTime(6000);
                }
            }
        }

        // ---- 【26】雷暴期间主动在玩家身上落雷 ----
        int interval = MSConfig.COMMON.lightningIntervalTicks.get();
        if (interval <= 0 || time % interval != 0L) {
            return;
        }
        ServerPlayer victim = HomingProjectiles.pickLightningVictim(level);
        if (victim == null) {
            return;
        }
        // 无视头顶是否有方块，直接在玩家位置落雷
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }
        bolt.snapTo(victim.getX(), victim.getY(), victim.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(bolt);
    }

    // ==========================================================
    // 【26】自然生成的雷也转向玩家（避雷针按原版范围优先）
    // ==========================================================

    @SubscribeEvent
    public static void onLightningJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LightningBolt bolt)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // findLightningTarget 内部已按原版规则检查 128 格内的避雷针；有避雷针时返回 null
        ServerPlayer target = HomingProjectiles.findLightningTarget(level, bolt.position());
        if (target != null) {
            bolt.snapTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        }
    }
}
