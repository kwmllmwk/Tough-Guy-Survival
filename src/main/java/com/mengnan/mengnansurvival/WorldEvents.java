/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块与生物相关的改动。
 */
public final class WorldEvents {

    private static final java.util.Random RNG = new java.util.Random();

    /**
     * 【4】记录「本次站立在虫蛀石头上是否已经判定过尖牙」。
     * 玩家离开虫蛀石头后会被移除，从而实现「再次踏上来才会再夹一次」。
     */
    private static final java.util.Set<java.util.UUID> FANG_DONE =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    // ==========================================================
    // 【3】挖掘石头/矿石有一半概率不掉落
    // 【32】石镐及以下无法采集铁矿石等（必须用铜镐）
    // ==========================================================

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(event.getBreaker() instanceof ServerPlayer)) {
            return;
        }

        // 【32】挖掘等级：木镐/石镐/金镐无法采集需要铜镐的矿石
        if (MSConfig.COMMON.copperTierForIronOre.get() && isCopperTierOre(state)) {
            ItemStack tool = event.getTool();
            if (tool.is(Items.WOODEN_PICKAXE) || tool.is(Items.STONE_PICKAXE)
                    || tool.is(Items.GOLDEN_PICKAXE)) {
                event.getDrops().clear();
                event.setDroppedExperience(0);
                event.setCanceled(true);
                return;
            }
        }

        // 【3】随机不掉落
        if (!isStoneOrOre(state)) {
            return;
        }
        double chance = MSConfig.COMMON.oreNoDropChance.get();
        if (chance > 0.0D && RNG.nextDouble() < chance) {
            event.getDrops().clear();
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    /**
     * 需要「铜镐或更高级」才能采集的矿石。
     * 覆盖原版中与铁矿石同级的矿石（铜、铁、青金石）及其深板岩变种。
     */
    private static boolean isCopperTierOre(BlockState state) {
        return state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE);
    }

    /** 判断方块是否属于「石头」或「矿石」范畴。 */
    private static boolean isStoneOrOre(BlockState state) {
        return state.is(BlockTags.ORES)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE) || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.END_STONE) || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT) || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.DRIPSTONE_BLOCK) || state.is(Blocks.SMOOTH_BASALT);
    }

    // ==========================================================
    // 【4】玩家可触及范围内的石头有 1/4 概率变成虫蛀的石头
    // ==========================================================

    @SubscribeEvent
    public static void onLevelTickForInfested(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        double chance = MSConfig.COMMON.infestedStoneChance.get();
        if (chance <= 0.0D) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            double reach = 5.0D;
            for (int i = 0; i < 8; i++) {
                int dx = (int) Math.round((RNG.nextDouble() * 2 - 1) * reach);
                int dy = (int) Math.round((RNG.nextDouble() * 2 - 1) * 3);
                int dz = (int) Math.round((RNG.nextDouble() * 2 - 1) * reach);
                BlockPos pos = player.blockPosition().offset(dx, dy, dz);
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) {
                    if (RNG.nextDouble() < chance) {
                        level.setBlock(pos, Blocks.INFESTED_STONE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * 【4】踏上虫蛀石头时冒出唤魔者尖牙。
     *
     * <p>规则（每次「踏上去」只判定一次）：</p>
     * <ul>
     *   <li>踩到虫蛀石头的瞬间判定一次，成功就生成尖牙；</li>
     *   <li>之后<b>站在原地不会被连续夹</b>；</li>
     *   <li>离开虫蛀石头后重新武装，<b>再次踏上来才会重新触发</b>。</li>
     * </ul>
     *
     * <p>由于原版尖牙在蓄力结束后还要固定再等 8 刻才咬合，原地不动的目标才容易被夹到；
     * 因此默认会预判玩家走位（见 {@code general.infested.leadTarget}），
     * 让「一边走一边踩上去」也能被夹到。</p>
     */
    @SubscribeEvent
    public static void onLevelTickForFangs(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        double chance = MSConfig.COMMON.fangChancePerEntry.get();
        if (chance <= 0.0D) {
            return;
        }
        int count = MSConfig.COMMON.fangCount.get();
        int warmup = MSConfig.COMMON.fangWarmupTicks.get();
        boolean lead = MSConfig.COMMON.fangLeadTarget.get();

        for (ServerPlayer player : level.players()) {
            java.util.UUID id = player.getUUID();

            // 判断此刻是否正站在虫蛀石头上
            boolean onInfested = !player.isSpectator() && !player.isCreative()
                    && player.onGround()
                    && level.getBlockState(player.blockPosition().below()).is(Blocks.INFESTED_STONE);

            if (!onInfested) {
                // 已经离开：重新武装，下次踏上来会再触发一次
                FANG_DONE.remove(id);
                continue;
            }
            // 本次站立已经判定过，站着不动不会再被夹
            if (!FANG_DONE.add(id)) {
                continue;
            }
            // 每次踏上只掷一次骰子，没中也算判定过，避免站着反复重摇
            if (RNG.nextDouble() >= chance) {
                continue;
            }

            BlockPos below = player.blockPosition().below();
            double baseX = below.getX() + 0.5D;
            double baseY = below.getY() + 1.0D;
            double baseZ = below.getZ() + 0.5D;

            // 预判：尖牙真正咬合的时刻 = 生成后 (蓄力 + 1 + 8) 刻
            if (lead) {
                int delay = warmup + 9;
                Vec3 predicted = player.position().add(player.getDeltaMovement().scale(delay));
                BlockPos spot = findFangSpot(level, predicted, player.getY());
                if (spot != null) {
                    baseX = spot.getX() + 0.5D;
                    baseY = spot.getY() + 1.0D;
                    baseZ = spot.getZ() + 0.5D;
                }
            }

            if (count <= 1) {
                spawnFang(level, baseX, baseY, baseZ, 0.0F, warmup);
            } else {
                // 多个尖牙时围绕中心排成一圈，保证覆盖玩家站位
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2.0D / count) * i;
                    spawnFang(level,
                            baseX + Math.cos(angle) * 0.3D,
                            baseY,
                            baseZ + Math.sin(angle) * 0.3D,
                            (float) angle, warmup);
                }
            }
        }
    }

    /**
     * 找出预判位置所在的那一格「可以站立的地面」。
     *
     * <p>要求该方块不是空气、且上方两格没有碰撞体积（玩家能站进去），
     * 否则返回 null 表示放弃预判、退回使用脚下的方块。</p>
     */
    private static BlockPos findFangSpot(ServerLevel level, Vec3 predicted, double playerY) {
        BlockPos column = BlockPos.containing(predicted.x, playerY, predicted.z);
        if (!level.isLoaded(column)) {
            return null;
        }
        BlockPos ground = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
        if (!level.isLoaded(ground) || level.getBlockState(ground).isAir()) {
            return null;
        }
        // 上方两格必须是可站立的空位
        if (!level.getBlockState(ground.above()).getCollisionShape(level, ground.above()).isEmpty()
                || !level.getBlockState(ground.above(2)).getCollisionShape(level, ground.above(2)).isEmpty()) {
            return null;
        }
        return ground;
    }

    /** 生成一个唤魔者尖牙。 */
    private static void spawnFang(ServerLevel level, double x, double y, double z, float rotation, int warmup) {
        net.minecraft.world.entity.projectile.EvokerFangs fang =
                new net.minecraft.world.entity.projectile.EvokerFangs(level, x, y, z, rotation, warmup, null);
        level.addFreshEntity(fang);
    }

    // ==========================================================
    // 【11】末影人死亡时生成三个末影螨
    // ==========================================================

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Enderman enderman)) {
            return;
        }
        if (!(enderman.level() instanceof ServerLevel level)) {
            return;
        }
        int count = MSConfig.COMMON.endermiteCount.get();
        for (int i = 0; i < count; i++) {
            Endermite mite = EntityTypes.ENDERMITE.create(level, EntitySpawnReason.TRIGGERED);
            if (mite == null) {
                continue;
            }
            mite.snapTo(enderman.getX() + (RNG.nextDouble() - 0.5D) * 2.0D,
                    enderman.getY(),
                    enderman.getZ() + (RNG.nextDouble() - 0.5D) * 2.0D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(mite);
        }
    }

    // ==========================================================
    // 生物生成时的处理
    //
    // 注意：NeoForge 26.3 中 FinalizeSpawnEvent 只在「刷怪笼生成」时触发，
    // 自然生成的生物不会触发，因此这些逻辑改挂在 EntityJoinLevelEvent 上。
    // ==========================================================

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();

        // 【13】可装备生物：强化已有装备并必定附魔
        if (entity instanceof Mob mob && entity instanceof Enemy) {
            MobGear.applyEquipment(mob, level);
        }

        // 【17】灾厄巡逻队长额外带出幻术师
        if (entity instanceof Raider raider && raider.isPatrolLeader()) {
            double chance = MSConfig.COMMON.illusionerPatrolChance.get();
            if (RNG.nextDouble() < chance) {
                Illusioner illusioner = EntityTypes.ILLUSIONER.create(level, EntitySpawnReason.PATROL);
                if (illusioner != null) {
                    illusioner.snapTo(raider.getX() + 1.0D, raider.getY(), raider.getZ() + 1.0D,
                            raider.getYRot(), 0.0F);
                    level.addFreshEntity(illusioner);
                }
            }
        }

        // 【28】骆驼尸壳骑士：原版 Husk 有 10% 概率骑骆驼尸壳，这里按倍率补足
        if (entity instanceof Husk husk && !husk.isPassenger()) {
            double multiplier = MSConfig.COMMON.riderSpawnMultiplier.get();
            double extra = Math.min(1.0D, 0.1D * multiplier) - 0.1D;
            if (extra > 0.0D && RNG.nextDouble() < extra) {
                spawnCamelHuskRider(level, husk);
            }
        }

        // 【18】僵尸鹦鹉螺骑士：原版 Drowned 持三叉戟时有 50% 概率骑僵尸鹦鹉螺
        if (entity instanceof Drowned drowned && !drowned.isPassenger()) {
            double multiplier = MSConfig.COMMON.riderSpawnMultiplier.get();
            double extra = Math.min(1.0D, 0.5D * multiplier) - 0.5D;
            if (extra > 0.0D && RNG.nextDouble() < extra) {
                spawnZombieNautilusRider(level, drowned);
            }
        }
    }

    /** 让尸壳骑上骆驼尸壳（原版骆驼尸壳骑士的补足实现）。 */
    private static void spawnCamelHuskRider(ServerLevel level, Husk husk) {
        var camel = EntityTypes.CAMEL_HUSK.create(level, EntitySpawnReason.NATURAL);
        if (camel == null) {
            return;
        }
        camel.snapTo(husk.getX(), husk.getY(), husk.getZ(), husk.getYRot(), 0.0F);
        camel.finalizeSpawn(level, level.getCurrentDifficultyAt(camel.blockPosition()),
                EntitySpawnReason.NATURAL, null);
        husk.startRiding(camel, true, true);
        level.addFreshEntity(camel);

        var parched = EntityTypes.PARCHED.create(level, EntitySpawnReason.NATURAL);
        if (parched != null) {
            parched.snapTo(husk.getX(), husk.getY(), husk.getZ(), husk.getYRot(), 0.0F);
            parched.finalizeSpawn(level, level.getCurrentDifficultyAt(parched.blockPosition()),
                    EntitySpawnReason.NATURAL, null);
            parched.startRiding(camel, false, false);
            level.addFreshEntityWithPassengers(parched);
        }
    }

    /** 让溺尸骑上僵尸鹦鹉螺（原版僵尸鹦鹉螺骑士的补足实现）。 */
    private static void spawnZombieNautilusRider(ServerLevel level, Drowned drowned) {
        var nautilus = EntityTypes.ZOMBIE_NAUTILUS.create(level, EntitySpawnReason.JOCKEY);
        if (nautilus == null) {
            return;
        }
        nautilus.snapTo(drowned.getX(), drowned.getY(), drowned.getZ(), drowned.getYRot(), 0.0F);
        nautilus.finalizeSpawn(level, level.getCurrentDifficultyAt(nautilus.blockPosition()),
                EntitySpawnReason.JOCKEY, null);
        drowned.startRiding(nautilus, false, false);
        level.addFreshEntity(nautilus);
    }

    // ==========================================================
    // 【14】雷暴时骷髅权重调整
    // 【31】海洋中自然生成守卫者
    // ==========================================================

    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 【31】海洋生物群系中加入守卫者
        int guardianWeight = MSConfig.COMMON.guardianOceanSpawnWeight.get();
        if (guardianWeight > 0 && event.getMobCategory() == MobCategory.MONSTER
                && level.getBiome(event.getPos()).is(BiomeTags.IS_OCEAN)) {
            boolean already = event.getSpawnerDataList().stream()
                    .anyMatch(w -> w.value().type() == EntityTypes.GUARDIAN);
            if (!already) {
                int min = MSConfig.COMMON.guardianOceanSpawnMin.get();
                int max = Math.max(min, MSConfig.COMMON.guardianOceanSpawnMax.get());
                var count = net.minecraft.util.valueproviders.UniformInt.of(min, max);
                event.addSpawnerData(new net.minecraft.util.random.Weighted<>(
                        new MobSpawnSettings.SpawnerData(EntityTypes.GUARDIAN, count), guardianWeight));
            }
        }

        // 【14】雷暴时权重调整
        boolean thundering = level.isThundering();
        double trapMultiplier = MSConfig.COMMON.skeletonTrapMultiplier.get();
        double skeletonMultiplier = MSConfig.COMMON.skeletonSpawnMultiplier.get();
        if (!thundering || (trapMultiplier == 1.0D && skeletonMultiplier == 1.0D)) {
            return;
        }

        List<net.minecraft.util.random.Weighted<MobSpawnSettings.SpawnerData>> current =
                new ArrayList<>(event.getSpawnerDataList());
        List<net.minecraft.util.random.Weighted<MobSpawnSettings.SpawnerData>> additions = new ArrayList<>();

        for (var weighted : current) {
            EntityType<?> type = weighted.value().type();
            double factor = 1.0D;
            if (type == EntityTypes.SKELETON_HORSE) {
                factor = trapMultiplier;
            } else if (type == EntityTypes.SKELETON) {
                factor = skeletonMultiplier;
            }
            if (factor != 1.0D) {
                int newWeight = Math.max(1, (int) Math.round(weighted.weight() * factor));
                event.removeSpawnerData(weighted);
                additions.add(new net.minecraft.util.random.Weighted<>(weighted.value(), newWeight));
            }
        }
        for (var add : additions) {
            event.addSpawnerData(add);
        }
    }

    /**
     * 【14】雷暴期间主动在玩家附近生成骷髅陷阱马。
     * 原版这一概率写死在 ServerLevel 的雷暴逻辑里，无法通过事件调整，因此这里按倍率补足。
     */
    @SubscribeEvent
    public static void onThunderTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        double multiplier = MSConfig.COMMON.skeletonTrapMultiplier.get();
        if (multiplier <= 0.0D || !level.isThundering()) {
            return;
        }
        if (level.getGameTime() % 100L != 0L) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
        BlockPos pos = player.blockPosition();
        double difficulty = level.getCurrentDifficultyAt(pos).getEffectiveDifficulty();
        // 原版基准概率 1%，按倍率放大
        if (level.getRandom().nextDouble() >= 0.01D * difficulty * multiplier) {
            return;
        }
        BlockPos spawnPos = pos.offset(level.getRandom().nextInt(21) - 10, 0, level.getRandom().nextInt(21) - 10);
        var horse = EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.EVENT);
        if (horse == null) {
            return;
        }
        horse.snapTo(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0F, 0.0F);
        horse.setTrap(true);
        horse.setAge(0);
        horse.setPersistenceRequired();
        level.addFreshEntity(horse);
    }

    // ==========================================================
    // 【21】玩家周围 64 格内的敌对生物获得力量 I（持续刷新）
    // ==========================================================

    @SubscribeEvent
    public static void onLevelTickForStrength(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        int amplifier = MSConfig.COMMON.hostileStrengthAmplifier.get();
        int duration = MSConfig.COMMON.hostileStrengthDurationTicks.get();
        double range = MSConfig.COMMON.hostileStrengthRange.get();

        for (ServerPlayer player : level.players()) {
            AABB box = player.getBoundingBox().inflate(range);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
                if (!(mob instanceof Enemy) || !mob.isAlive()) {
                    continue;
                }
                var existing = mob.getEffect(MobEffects.STRENGTH);
                if (existing == null || existing.getDuration() < duration - 40) {
                    mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, amplifier, false, false));
                }
            }
        }
    }

    // ==========================================================
    // 【18】玩家靠近猪 -> 疣猪兽
    // 【19】玩家靠近鸡、牛、羊 -> 兔子
    // ==========================================================

    @SubscribeEvent
    public static void onLevelTickForTransform(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }

        double pigRange = MSConfig.COMMON.pigToHoglinRange.get();
        double farmRange = MSConfig.COMMON.farmAnimalToRabbitRange.get();

        for (ServerPlayer player : level.players()) {
            for (Pig pig : level.getEntitiesOfClass(Pig.class, player.getBoundingBox().inflate(pigRange))) {
                transformEntity(level, pig, EntityTypes.HOGLIN);
            }
            AABB farmBox = player.getBoundingBox().inflate(farmRange);
            for (Chicken chicken : level.getEntitiesOfClass(Chicken.class, farmBox)) {
                transformEntity(level, chicken, EntityTypes.RABBIT);
            }
            for (Cow cow : level.getEntitiesOfClass(Cow.class, farmBox)) {
                transformEntity(level, cow, EntityTypes.RABBIT);
            }
            for (Sheep sheep : level.getEntitiesOfClass(Sheep.class, farmBox)) {
                transformEntity(level, sheep, EntityTypes.RABBIT);
            }
        }
    }

    /** 把原生物替换为目标生物，并立刻清除原生物产生的掉落物。 */
    private static void transformEntity(ServerLevel level, LivingEntity original, EntityType<?> targetType) {
        if (!original.isAlive()) {
            return;
        }
        Entity replacement = targetType.create(level, EntitySpawnReason.CONVERSION);
        if (replacement == null) {
            return;
        }
        replacement.snapTo(original.getX(), original.getY(), original.getZ(),
                original.getYRot(), original.getXRot());
        if (replacement instanceof Mob newMob) {
            newMob.setPersistenceRequired();
        }

        double x = original.getX();
        double y = original.getY();
        double z = original.getZ();
        original.discard();
        level.addFreshEntity(replacement);
        clearDropsNear(level, x, y, z);
    }

    /** 清除指定位置附近刚生成的掉落物（【18】【19】要求「立刻清除」）。 */
    private static void clearDropsNear(ServerLevel level, double x, double y, double z) {
        AABB box = new AABB(x - 1.5D, y - 1.5D, z - 1.5D, x + 1.5D, y + 1.5D, z + 1.5D);
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            item.discard();
        }
    }

    // ==========================================================
    // 【16】无论上次睡觉是什么时候，晚上必定正常生成幻翼
    // ==========================================================

    @SubscribeEvent
    public static void onPlayerSpawnPhantoms(
            net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent event) {
        if (!MSConfig.COMMON.phantomsEveryNight.get()) {
            return;
        }
        event.setResult(net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent.Result.ALLOW);
    }
}
