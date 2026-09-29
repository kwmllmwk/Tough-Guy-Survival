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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家相关的所有改动。
 */
public final class PlayerEvents {

    private static final Random RNG = new Random();

    /** 【8】每个玩家距离下次「走动掉落」的剩余刻数。 */
    private static final Map<UUID, Integer> WALK_TIMERS = new ConcurrentHashMap<>();

    /** 【8】【24】上一刻的玩家位置，用来可靠地判断「是否在移动」。 */
    private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();

    /** 【7】上一次「受伤掉落」发生的游戏刻，用于防止着火等持续伤害瞬间清空背包。 */
    private static final Map<UUID, Long> LAST_DAMAGE_DROP = new ConcurrentHashMap<>();

    // ==========================================================
    // 【1】出生或重生时携带近乎无尽的 1 级挖掘疲劳
    // ==========================================================

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyFatigue(player);
            giveWelcomeBookIfFirstTime(player);
        }
    }

    @SubscribeEvent
    /** 重生时同样补上挖掘疲劳。 */
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyFatigue(player);
        }
    }

    /** 给玩家施加近乎永久的挖掘疲劳（等级固定）。 */
    private static void applyFatigue(ServerPlayer player) {
        int duration = MSConfig.COMMON.fatigueDurationTicks.get();
        int amplifier = MSConfig.COMMON.fatigueAmplifier.get();
        player.forceAddEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, amplifier, false, false), null);
    }

    // ==========================================================
    // 【34】玩家第一次进入世界时赠送一本 README 成书
    // ==========================================================

    /** 玩家存档里的标记键：表示已经发过赠书了。 */
    private static final String BOOK_GIVEN_KEY = "mengnansurvival:welcome_book_given";

    /**
     * 只在玩家<b>第一次</b>进入这个世界时发一次书。
     *
     * <p>标记写在玩家自己的持久化数据里（随玩家存档保存），
     * 因此重进世界、重启服务器都不会重复发放。</p>
     */
    private static void giveWelcomeBookIfFirstTime(ServerPlayer player) {
        if (!MSConfig.COMMON.giveWelcomeBook.get()) {
            return;
        }
        var data = player.getPersistentData();
        if (data.getBooleanOr(BOOK_GIVEN_KEY, false)) {
            return;
        }
        data.putBoolean(BOOK_GIVEN_KEY, true);

        ItemStack book = WelcomeBook.create();
        // 背包放不下时直接掉在脚下，保证玩家一定能拿到
        if (!player.getInventory().add(book)) {
            player.drop(book, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    // ==========================================================
    // 每刻的玩家逻辑
    // ==========================================================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        boolean moved = updateAndCheckMoved(player);

        if (!player.isCreative()) {
            handleArmorSlowness(player);
            handleBucketHazards(player, moved);
            handleTrialChamber(player);
            handleWalkDrop(player, moved);
            handleSulfurPool(player);
            handleNaturalRegen(player);
        } else {
            handleBucketHazards(player, moved);
        }
    }

    /**
     * 用「两刻之间的位移」判断玩家是否在移动。
     *
     * <p>之前用 deltaMovement 判断在行走/疾跑时不可靠，这里改为直接比较位置，
     * 行走、疾跑、跳跃、划船都能稳定识别。</p>
     */
    private static boolean updateAndCheckMoved(ServerPlayer player) {
        Vec3 now = player.position();
        Vec3 last = LAST_POS.put(player.getUUID(), now);
        if (last == null) {
            return false;
        }
        double horizontal = Math.abs(now.x - last.x) + Math.abs(now.z - last.z);
        double vertical = Math.abs(now.y - last.y);
        // 0.005 格/刻 以上视为确实在移动
        boolean moved = horizontal > 0.005D || vertical > 0.005D;
        // 划船时位置变化同样会被上面的位移检测捕捉到，这里额外兜底
        if (!moved && player.isPassenger() && player.getVehicle() instanceof Boat) {
            moved = true;
        }
        return moved;
    }

    /** 【9】铁制或钻石制装备 -> 缓慢 I。 */
    private static void handleArmorSlowness(ServerPlayer player) {
        int amplifier = MSConfig.COMMON.armorSlowAmplifier.get();
        if (wearsIronOrDiamond(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, amplifier, false, false));
        }
    }

    /** 判断玩家四个护甲位里是否穿着铁制或钻石制装备。 */
    private static boolean wearsIronOrDiamond(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
                    || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)
                    || stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
                    || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)) {
                return true;
            }
        }
        return false;
    }

    // ==========================================================
    // 【24】手持岩浆桶被点燃；背包里有水桶且移动时倒出
    // ==========================================================

    private static void handleBucketHazards(ServerPlayer player, boolean moved) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (MSConfig.COMMON.lavaBucketIgnites.get()) {
            if (main.is(Items.LAVA_BUCKET) || off.is(Items.LAVA_BUCKET)) {
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 60));
            }
        }

        double spillChance = MSConfig.COMMON.waterBucketSpillChance.get();
        if (spillChance <= 0.0D || !player.isAlive() || !moved) {
            return;
        }
        if (RNG.nextDouble() >= spillChance) {
            return;
        }

        // 不要求手持：快捷栏与背包中任意一格的水桶都可能倒出
        var inventory = player.getInventory();
        List<Integer> waterSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.WATER_BUCKET)) {
                waterSlots.add(i);
            }
        }
        if (waterSlots.isEmpty()) {
            return;
        }
        int slot = waterSlots.get(RNG.nextInt(waterSlots.size()));
        if (spillWater(player)) {
            inventory.setItem(slot, new ItemStack(Items.BUCKET));
        }
    }

    /** 在玩家脚下放出一格水源。 */
    private static boolean spillWater(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos base = player.blockPosition();
        for (BlockPos candidate : new BlockPos[]{base, base.below()}) {
            if (!level.getFluidState(candidate).isEmpty()) {
                continue;
            }
            BlockState state = level.getBlockState(candidate);
            if (!state.canBeReplaced(Fluids.WATER)) {
                continue;
            }
            // 采用原版水桶的做法：flag 11 = 更新方块 + 同步客户端，水源会正常流动
            level.setBlock(candidate, Fluids.WATER.defaultFluidState().createLegacyBlock(), 11);
            level.updateNeighborsAt(candidate, Blocks.WATER, null);
            return true;
        }
        return false;
    }

    // ==========================================================
    // 【25】进入试炼密室获得不祥之兆
    // ==========================================================

    private static void handleTrialChamber(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long time = level.getGameTime();
        int interval = MSConfig.COMMON.badOmenIntervalTicks.get();
        if (time % interval != 0L) {
            return;
        }
        if (!isInTrialChamber(level, player.blockPosition())) {
            return;
        }
        int duration = MSConfig.COMMON.badOmenDurationTicks.get();
        player.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, duration, 0, false, true));
    }

    /** 判断该位置是否属于试炼密室（退化方案：检测附近铜类方块）。 */
    private static boolean isInTrialChamber(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                    if (isCopperLike(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 判断方块是否属于铜类（铜块 / 铜格栅 / 铜灯等）或试炼相关方块。 */
    private static boolean isCopperLike(BlockState state) {
        // 26.3 提供了统一的 copper 标签，覆盖所有氧化程度的铜块、铜格栅、铜灯等
        return state.is(net.minecraft.tags.BlockTags.COPPER)
                || state.is(Blocks.TRIAL_SPAWNER)
                || state.is(Blocks.VAULT);
    }

    // ==========================================================
    // 【29】硫磺池：受到其反胃效果时同时中毒
    // ==========================================================

    private static void handleSulfurPool(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 只有处在硫磺池施加的反胃效果下才处理
        if (!player.hasEffect(MobEffects.NAUSEA)) {
            return;
        }
        if (!isNearPotentSulfur(level, player.blockPosition())) {
            return;
        }
        int duration = MSConfig.COMMON.sulfurPoisonDurationTicks.get();
        player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0, false, true));
    }

    /** 检测附近是否存在硫磺池方块（BottleSulfurBlock / POTENT_SULFUR）。 */
    private static boolean isNearPotentSulfur(ServerLevel level, BlockPos pos) {
        int r = (int) Math.ceil(MSConfig.COMMON.sulfurDetectRange.get());
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    if (level.getBlockState(p).is(Blocks.POTENT_SULFUR)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ==========================================================
    // 【8】行走/疾跑/跳跃/划船时每 5~15 秒随机掉落一格物品
    // ==========================================================

    private static void handleWalkDrop(ServerPlayer player, boolean moved) {
        if (moved) {
            int remaining = WALK_TIMERS.getOrDefault(player.getUUID(), -1);
            if (remaining < 0) {
                remaining = rollWalkInterval();
            }
            remaining--;
            if (remaining <= 0) {
                dropRandomItem(player);
                remaining = rollWalkInterval();
            }
            WALK_TIMERS.put(player.getUUID(), remaining);
        } else {
            WALK_TIMERS.remove(player.getUUID());
        }
    }

    /** 随机出下一次「走动掉落」的间隔（游戏刻）。 */
    private static int rollWalkInterval() {
        int min = MSConfig.COMMON.walkDropMinSeconds.get();
        int max = MSConfig.COMMON.walkDropMaxSeconds.get();
        if (max < min) {
            max = min;
        }
        int seconds = min + RNG.nextInt(max - min + 1);
        return seconds * 20;
    }

    // ==========================================================
    // 【7】受到一次伤害时，随机掉落一格物品
    // ==========================================================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 【22 / 17】溺水窒息：凋零 + 失明 + 反胃
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.DROWN)) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    MSConfig.COMMON.drownWitherDurationTicks.get(), 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                    MSConfig.COMMON.drownBlindnessDurationTicks.get(), 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,
                    MSConfig.COMMON.drownNauseaDurationTicks.get(), 0, false, true));
        }

        // 【27】摔落伤害 > 1 点
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL) && event.getAmount() > 1.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    MSConfig.COMMON.fallSlowTicks.get(), 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                    MSConfig.COMMON.fallBlindnessTicks.get(), 0, false, true));
        }

        // 【31】被守卫者攻击 -> 饥饿 V
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.monster.Guardian) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER,
                    MSConfig.COMMON.guardianHungerDurationTicks.get(),
                    MSConfig.COMMON.guardianHungerAmplifier.get(), false, true));
        }

        // 【7】受伤掉落物品（带冷却，避免着火等持续伤害瞬间清空背包）
        if (player.isCreative() || player.isSpectator() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        int cooldown = MSConfig.COMMON.damageDropCooldownTicks.get();
        Long last = LAST_DAMAGE_DROP.get(player.getUUID());
        if (last != null && now - last < cooldown) {
            return;
        }
        LAST_DAMAGE_DROP.put(player.getUUID(), now);

        int count = MSConfig.COMMON.damageDropCount.get();
        for (int i = 0; i < count; i++) {
            dropRandomItem(player);
        }
    }

    /**
     * 从物品栏 + 背包中随机挑一格非空格子丢出。
     */
    private static boolean dropRandomItem(ServerPlayer player) {
        var inventory = player.getInventory();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty()) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        int slot = candidates.get(RNG.nextInt(candidates.size()));
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack dropped = stack.copy();
        inventory.setItem(slot, ItemStack.EMPTY);
        player.drop(dropped, true, net.minecraft.util.Prediction.SERVER_ONLY);
        return true;
    }

    // ==========================================================
    // 【30】食物回血（饥饿 / 饱和度）
    // ==========================================================

    /**
     * 原版 FoodData 的回血分支是「二选一」的：
     * <pre>
     *   if (饱食度 &gt;= 20 且 饱和度 &gt; 0)  -&gt; 快速回血（每 10 刻）
     *   else if (饱食度 &gt;= 18)           -&gt; 自然回血（每 80 刻）
     * </pre>
     *
     * <p>三种模式见 {@link MSConfig.FoodRegenMode}：</p>
     * <ul>
     *   <li><b>NONE（默认）</b>：两条分支的治疗都被取消，玩家只能靠药水效果回血。</li>
     *   <li><b>NO_SATURATION</b>：只取消快速回血分支，并代为按自然回血的节奏补一次治疗
     *       （因为只要饱和度 &gt; 0，原版就永远不会走自然回血分支）。</li>
     *   <li><b>NORMAL</b>：不做任何干预。</li>
     * </ul>
     */
    private static final Map<UUID, Integer> REGEN_TIMER = new ConcurrentHashMap<>();

    /** 标记「这次治疗是我们自己发起的」，避免被下面的取消逻辑误伤。 */
    private static final java.util.Set<UUID> SELF_REGEN = ConcurrentHashMap.newKeySet();

    private static void handleNaturalRegen(ServerPlayer player) {
        if (MSConfig.COMMON.foodRegenMode.get() != MSConfig.FoodRegenMode.NO_SATURATION) {
            REGEN_TIMER.remove(player.getUUID());
            return;
        }
        var food = player.getFoodData();
        // 只有「饱和度 > 0 且饱食度 = 20」时原版才走快速回血分支，
        // 也只有这种时候需要我们代为执行自然回血。
        if (food.getSaturationLevel() > 0.0F && food.getFoodLevel() >= 20 && player.isHurt()) {
            int timer = REGEN_TIMER.merge(player.getUUID(), 1, Integer::sum);
            if (timer >= 80) {
                REGEN_TIMER.put(player.getUUID(), 0);
                SELF_REGEN.add(player.getUUID());
                try {
                    player.heal(1.0F);
                    food.addExhaustion(6.0F);
                } finally {
                    SELF_REGEN.remove(player.getUUID());
                }
            }
        } else {
            // 其余情况交给原版自己的自然回血处理
            REGEN_TIMER.remove(player.getUUID());
        }
    }

    /**
     * 取消由食物（饥饿 / 饱和度）产生的回血。
     *
     * <p>判别方式：</p>
     * <ul>
     *   <li>食物回血每次只有 1 点或更少，因此 <b>治疗量 &gt; 1 的一律放行</b>
     *       （瞬间治疗药水是 4 点，不会被误伤）；</li>
     *   <li>拥有<b>再生效果</b>时放行（药水、金苹果靠的就是它）；</li>
     *   <li>食物回血只在饱食度 ≥ 18 时发生，因此低于该值的一律放行。</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        MSConfig.FoodRegenMode mode = MSConfig.COMMON.foodRegenMode.get();
        if (mode == MSConfig.FoodRegenMode.NORMAL) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 我们自己补的自然回血要放行
        if (SELF_REGEN.contains(player.getUUID())) {
            return;
        }
        // 再生效果带来的回血放行
        if (player.hasEffect(MobEffects.REGENERATION)) {
            return;
        }
        // 大额治疗（瞬间治疗药水等）放行
        if (event.getAmount() > 1.0F) {
            return;
        }
        var food = player.getFoodData();
        if (mode == MSConfig.FoodRegenMode.NO_SATURATION) {
            // 只拦截快速回血：饱食度 = 20 且饱和度 > 0 时原版只会走这条分支
            if (food.getFoodLevel() >= 20 && food.getSaturationLevel() > 0.0F) {
                event.setCanceled(true);
            }
        } else {
            // NONE：饱食度 ≥ 18 时原版的两条食物回血分支都可能生效，全部拦截
            if (food.getFoodLevel() >= 18) {
                event.setCanceled(true);
            }
        }
    }

    // ==========================================================
    // 【6】完成一次睡眠后头顶生成 4 只幻翼
    // ==========================================================

    /**
     * 用 PlayerWakeUpEvent 判定「睡了一整觉」。
     *
     * <p>这里刻意不再用 isBrightOutside()（它在刚醒来的那一刻可能仍为 false），
     * 改为判断世界时间是否已经进入白天，并排除被中途叫醒的情况。</p>
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 被中途叫醒（wakeImmediately）不算完成睡眠
        if (event.wakeImmediately()) {
            return;
        }
        // 确认时间已经变成白天（26.3 用「世界时钟」表示时间，0~23999 为一天）
        long dayTime = level.getOverworldClockTime() % 24000L;
        if (dayTime >= 12000L) {
            return;
        }
        spitPhantoms(player, MSConfig.COMMON.phantomsOnSleep.get());
    }

    /** 在玩家头顶生成指定数量的幻翼（自带防火效果）。 */
    private static void spitPhantoms(ServerPlayer player, int count) {
        if (count <= 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        var type = net.minecraft.world.entity.EntityTypes.PHANTOM;
        for (int i = 0; i < count; i++) {
            var phantom = type.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (phantom == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D / count) * i;
            double x = player.getX() + Math.cos(angle) * 2.0D;
            double z = player.getZ() + Math.sin(angle) * 2.0D;
            double y = player.getY() + 3.0D;
            phantom.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
            // 幻翼带有防火效果
            phantom.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                    Integer.MAX_VALUE, 0, false, false));
            level.addFreshEntity(phantom);
        }
    }

    // ==========================================================
    // 【10】穿过地狱门时按概率损坏
    // ==========================================================

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double chance = MSConfig.COMMON.portalBreakChance.get();
        if (chance <= 0.0D || RNG.nextDouble() >= chance) {
            return;
        }
        breakNearbyPortal(player);
    }

    /** 破坏玩家附近的地狱门方块（保留黑曜石框架）。 */
    private static void breakNearbyPortal(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = player.blockPosition();
        int radius = 4;
        boolean broke = false;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.NETHER_PORTAL)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                broke = true;
            }
        }
        if (broke) {
            level.updateNeighborsAt(center, Blocks.NETHER_PORTAL, null);
        }
    }

    // ==========================================================
    // 【12】玩家死亡时在原地生成戴着皮革头盔、以玩家命名的僵尸
    // ==========================================================

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!MSConfig.COMMON.corpseZombieEnabled.get()) {
            return;
        }
        if (!event.isWasDeath()) {
            return;
        }
        Player original = event.getOriginal();
        if (!(original.level() instanceof ServerLevel level)) {
            return;
        }
        spawnCorpseZombie(level, original);
    }

    /** 生成戴着皮革头盔、以玩家命名的僵尸。 */
    private static void spawnCorpseZombie(ServerLevel level, Player player) {
        Zombie zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(
                level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (zombie == null) {
            return;
        }
        zombie.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        zombie.setCustomName(Component.literal(player.getGameProfile().name()));
        zombie.setCustomNameVisible(true);
        zombie.setPersistenceRequired();
        level.addFreshEntity(zombie);
    }
}
