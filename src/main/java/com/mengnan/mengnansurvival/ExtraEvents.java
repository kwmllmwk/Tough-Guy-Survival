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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 第三批新增玩法（第 36 ~ 45 项）。
 *
 * <p>放在单独的文件里，避免把已有的 {@link WorldEvents} / {@link PlayerEvents} 撑得过大。</p>
 */
public final class ExtraEvents {

    private static final java.util.Random RNG = new java.util.Random();

    private ExtraEvents() {}

    // ==========================================================
    // 【36】所有僵尸出生时携带皮革帽子
    // 【41】所有蜘蛛出生时带有 1 小时隐身
    // 【43】女巫还会投掷凋零 / 盘丝 / 渗浆 / 虫蚀药水
    // 【45】所有骷髅出生时佩戴下界合金头盔
    // ==========================================================

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();

        // 【36】僵尸：空着头就补一顶皮革帽子
        if (entity instanceof Zombie zombie && MSConfig.COMMON.zombieLeatherCap.get()) {
            equipIfHeadEmpty(zombie, new ItemStack(Items.LEATHER_HELMET));
        }

        // 【45】骷髅（含流浪者 / 沼骸 / 凋灵骷髅）：空着头就补一顶下界合金头盔
        if (entity instanceof AbstractSkeleton skeleton && MSConfig.COMMON.skeletonNetheriteHelmet.get()) {
            equipIfHeadEmpty(skeleton, new ItemStack(Items.NETHERITE_HELMET));
        }

        // 【41】蜘蛛（含洞穴蜘蛛）：自带 1 小时隐身
        if (entity instanceof Spider spider && MSConfig.COMMON.spiderInvisibilityEnabled.get()) {
            spider.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                    MSConfig.COMMON.spiderInvisibilityTicks.get(), 0, false, false));
        }

        // 【43】女巫投出的喷溅药水：按概率换成凋零 / 盘丝 / 渗浆 / 虫蚀
        if (entity instanceof ThrownSplashPotion potion) {
            maybeUpgradeWitchPotion(level, potion);
        }
    }

    /** 只在头部装备槽为空时给生物穿上装备（不覆盖原版已有的装备）。 */
    private static void equipIfHeadEmpty(Mob mob, ItemStack helmet) {
        if (!mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }
        mob.setItemSlot(EquipmentSlot.HEAD, helmet);
    }

    /**
     * 【43】女巫的药水强化。
     *
     * <p>女巫的投掷物是普通的 {@code ThrownSplashPotion}，这里在它刚生成时，
     * 按配置概率把药水内容替换成四种新增药水之一，效果与女巫自己投掷完全一致。</p>
     */
    private static void maybeUpgradeWitchPotion(ServerLevel level, ThrownSplashPotion potion) {
        double chance = MSConfig.COMMON.witchExtraPotionChance.get();
        if (chance <= 0.0D) {
            return;
        }
        // 只有女巫投的才算
        if (!(potion.getOwner() instanceof Witch)) {
            return;
        }
        if (RNG.nextDouble() >= chance) {
            return;
        }

        // 候选：凋零（自定义）、盘丝、渗浆、虫蚀
        var candidates = java.util.List.of(
                MSPotions.WITHER.getDelegate(),
                Potions.WEAVING,
                Potions.OOZING,
                Potions.INFESTED);
        var chosen = candidates.get(RNG.nextInt(candidates.size()));

        potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, chosen));
    }

    // ==========================================================
    // 【37】划船时获得饥饿 I
    // ==========================================================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!MSConfig.COMMON.boatHungerEnabled.get()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        // 所有船 / 木筏（含带箱子的）都继承自 AbstractBoat
        if (player.getVehicle() instanceof AbstractBoat) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER,
                    40, MSConfig.COMMON.boatHungerAmplifier.get(), false, false));
        }
    }

    // ==========================================================
    // 【38】岩浆不断尝试点燃周围四格方块的上表面
    // ==========================================================

    /**
     * 每隔若干刻，扫描玩家附近的岩浆，并尝试在它四邻方块的上表面放火。
     *
     * <p>原版岩浆只会「偶尔」引燃附近的可燃方块，这里改为持续尝试，
     * 因此石头地面上方也会不断冒火。为了控制开销，只扫描玩家附近的区域。</p>
     */
    @SubscribeEvent
    public static void onLevelTickForLava(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!MSConfig.COMMON.lavaIgniteEnabled.get()) {
            return;
        }
        int interval = MSConfig.COMMON.lavaIgniteIntervalTicks.get();
        if (interval <= 0 || level.getGameTime() % interval != 0L) {
            return;
        }
        int range = MSConfig.COMMON.lavaIgniteRange.get();

        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -range; dz <= range; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!level.isLoaded(pos)) {
                            continue;
                        }
                        // 只看岩浆方块
                        if (!level.getBlockState(pos).is(Blocks.LAVA)) {
                            continue;
                        }
                        // 四个水平方向：尝试在邻方块的顶面点火
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockPos above = pos.relative(dir).above();
                            if (!level.isLoaded(above)) {
                                continue;
                            }
                            // 必须是空气，且该位置允许生成火
                            if (level.getBlockState(above).isAir()
                                    && BaseFireBlock.canBePlacedAt(level, above, Direction.UP)) {
                                // flag 11 = 更新方块 + 同步客户端
                                level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================================
    // 【39】末影水晶无法被箭矢摧毁
    // ==========================================================

    /**
     * 箭矢击中末影水晶时直接取消这次命中。
     *
     * <p>注意：三叉戟在 26.3 里也继承 {@code AbstractArrow}，但需求说的是「箭矢」，
     * 因此这里把三叉戟排除在外，三叉戟仍然可以正常击碎水晶。</p>
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!MSConfig.COMMON.endCrystalArrowProof.get()) {
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        if (isTrident(arrow)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        if (hit.getEntity() instanceof EndCrystal) {
            event.setCanceled(true);
        }
    }

    /** 判断弹射物是否为三叉戟（26.3 位于 projectile.arrow 包下）。 */
    private static boolean isTrident(Projectile projectile) {
        return projectile instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident;
    }

    // ==========================================================
    // 【40】下界里炼药锅不再能储水（只能储岩浆与细雪）
    // ==========================================================

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!MSConfig.COMMON.netherNoWaterCauldron.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != Level.NETHER) {
            return;
        }
        // 只有「拿水桶点炼药锅」这一种情况需要拦
        if (!event.getItemStack().is(Items.WATER_BUCKET)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON)) {
            // 禁止方块交互，并把这个动作判为失败（水桶不会被消耗）
            event.setUseBlock(TriState.FALSE);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    // ==========================================================
    // 【44】末影人每隔 40 刻尝试摧毁「头部正前方」的方块
    // ==========================================================

    /**
     * 用「实体逐个 tick」的事件实现，这样不必自己扫描世界里的所有末影人。
     *
     * <p>摧毁的是末影人<b>头部朝向的正前方</b>那一格：
     * 从眼睛位置沿视线方向逐步向前探测，找到第一个非空气方块并将其摧毁。
     * 基岩、传送门框架等无法破坏的方块会被跳过（破坏速度 &lt; 0）。</p>
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Enderman enderman)) {
            return;
        }
        if (!MSConfig.COMMON.endermanBreakBlocks.get()) {
            return;
        }
        int interval = MSConfig.COMMON.endermanBreakIntervalTicks.get();
        if (interval <= 0 || enderman.tickCount % interval != 0) {
            return;
        }
        if (!(enderman.level() instanceof ServerLevel level)) {
            return;
        }

        double reach = MSConfig.COMMON.endermanBreakRange.get();
        Vec3 eye = enderman.getEyePosition();
        Vec3 look = enderman.getLookAngle();

        // 从眼睛出发，沿视线方向一小步一小步往前找第一个方块
        for (double step = 0.5D; step <= reach; step += 0.5D) {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(step)));
            if (!level.isLoaded(pos)) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            // 破坏速度为负数表示不可破坏（基岩、屏障、传送门框架等）
            if (state.getDestroySpeed(level, pos) < 0.0F) {
                return;
            }
            // 不掉落，直接摧毁
            level.destroyBlock(pos, false);
            return;
        }
    }

    // ==========================================================
    // 【46】所有生肉被食用后获得饥饿（时长与腐肉一致）
    // ==========================================================

    /**
     * 生肉清单。
     *
     * <p>原版的 {@code #minecraft:meat} 标签把生肉和熟肉混在一起，无法直接用，
     * 因此这里显式列出「生肉」。鱼不算肉，所以没有包含在内。</p>
     */
    private static final java.util.Set<net.minecraft.world.item.Item> RAW_MEATS = java.util.Set.of(
            Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT);

    @SubscribeEvent
    public static void onItemUseFinish(
            net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!MSConfig.COMMON.rawMeatHungerEnabled.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 吃掉的东西在 getItem()（getResultStack 是剩下的容器）
        if (!RAW_MEATS.contains(event.getItem().getItem())) {
            return;
        }
        // 与腐肉完全相同的参数：饥饿 I、600 刻（30 秒）
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER,
                MSConfig.COMMON.rawMeatHungerTicks.get(),
                MSConfig.COMMON.rawMeatHungerAmplifier.get(), false, true));
    }

    // ==========================================================
    // 【48】恶魂火球击中任何东西后，在落点生成一只点燃的闪电苦力怕
    // ==========================================================

    @SubscribeEvent
    public static void onFireballImpact(ProjectileImpactEvent event) {
        if (!MSConfig.COMMON.ghastFireballCreeper.get()) {
            return;
        }
        if (!(event.getProjectile() instanceof LargeFireball fireball)) {
            return;
        }
        if (!(fireball.level() instanceof ServerLevel level)) {
            return;
        }
        // 命中点的坐标（打中实体/方块都用这个）
        Vec3 hit = event.getRayTraceResult().getLocation();

        var creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.TRIGGERED);
        if (creeper == null) {
            return;
        }
        creeper.snapTo(hit.x, hit.y, hit.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

        // 变成闪电苦力怕：26.3 没有公开的 setPowered，走原版 thunderHit 通道
        var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.snapTo(hit.x, hit.y, hit.z, 0.0F, 0.0F);
            creeper.thunderHit(level, bolt);
        }
        // 点燃（引信开始倒计时）
        creeper.ignite();
        // 避免它刚生成就被当成自然生物清理掉
        creeper.setPersistenceRequired();
        level.addFreshEntity(creeper);
    }
}
