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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * 箭矢/三叉戟追踪玩家，以及闪电目标选择的辅助逻辑。
 */
public final class HomingProjectiles {

    /**
     * 三叉戟实体类名。
     * 注意：26.3 里它位于 {@code world.entity.projectile.arrow} 包下（早期版本在上一层）。
     * 包名写错会导致追踪对三叉戟完全失效，所以这里显式记下正确路径。
     */
    private static final String TRIDENT_CLASS = "net.minecraft.world.entity.projectile.arrow.ThrownTrident";

    private HomingProjectiles() {}

    // ==========================================================
    // 箭矢 / 三叉戟追踪玩家
    // ==========================================================

    /**
     * 每一刻让玩家附近的箭矢与三叉戟朝玩家转向。
     *
     * <p>两种模式（配置项 <code>general.homing.perfectTracking</code>）：</p>
     * <ul>
     *   <li><b>false（默认，严格转向）</b>：把当前朝向朝目标方向旋转一个受上限约束的角度，
     *       由 <code>turnRateDegreesPerSecond</code> 控制转速。这是真正的「转向」，
     *       箭头会画出可见的弧线逐渐咬住目标。</li>
     *   <li><b>true</b>：每刻直接把方向对准目标，命中率接近 100%。</li>
     * </ul>
     *
     * <p>两种模式都<b>保持速度大小（速率）不变</b>，只改变方向，因此不会凭空加速。</p>
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        double range = MSConfig.COMMON.projectileHomingRange.get();
        double rangeSqr = range * range;
        boolean perfect = MSConfig.COMMON.projectilePerfectTracking.get();
        // 每刻允许旋转的最大角度
        double maxTurnRadians = Math.toRadians(MSConfig.COMMON.projectileTurnRate.get() / 20.0D);

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            AABB searchBox = player.getBoundingBox().inflate(range);

            for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, searchBox)) {
                if (!arrow.isAlive() || arrow.isNoPhysics()) {
                    continue;
                }
                steer(arrow, players, rangeSqr, perfect, maxTurnRadians);
            }

            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, searchBox)) {
                if (!isTrident(projectile) || !projectile.isAlive() || projectile.noPhysics) {
                    continue;
                }
                steer(projectile, players, rangeSqr, perfect, maxTurnRadians);
            }
        }
    }

    /** 判断一个弹射物是否为三叉戟（沿继承链比较类名）。 */
    private static boolean isTrident(Projectile projectile) {
        Class<?> c = projectile.getClass();
        while (c != null) {
            if (TRIDENT_CLASS.equals(c.getName())) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    /**
     * 让弹射物朝最近的玩家转向。
     *
     * <p>注意：不能用 projectile.getOwner() 当目标——那是「射箭的人」，
     * 会导致箭矢回溯射向射手。</p>
     */
    private static void steer(Entity projectile, List<ServerPlayer> allPlayers,
                              double rangeSqr, boolean perfect, double maxTurnRadians) {
        // 目标：范围内最近的玩家
        ServerPlayer target = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayer candidate : allPlayers) {
            if (!candidate.isAlive() || candidate.isSpectator() || candidate.isCreative()) {
                continue;
            }
            if (candidate.level() != projectile.level()) {
                continue;
            }
            double d = candidate.position().distanceToSqr(projectile.position());
            if (d > rangeSqr) {
                continue;
            }
            if (d < bestDist) {
                bestDist = d;
                target = candidate;
            }
        }
        if (target == null) {
            return;
        }

        Vec3 current = projectile.getDeltaMovement();
        double speed = current.length();
        // 已插在地上或速度趋近 0 时不干预
        if (speed < 1.0E-3D) {
            return;
        }

        // 瞄准玩家躯干中心
        Vec3 aimPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 toTarget = aimPoint.subtract(projectile.position());
        if (toTarget.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 desiredDir = toTarget.normalize();
        Vec3 currentDir = current.scale(1.0D / speed);

        Vec3 newDir;
        if (perfect) {
            newDir = desiredDir;
        } else {
            double dot = Math.clamp(currentDir.dot(desiredDir), -1.0D, 1.0D);
            double angle = Math.acos(dot);
            if (angle < 1.0E-4D) {
                return; // 已经对准，无需转向
            }
            // 本次最多旋转 maxTurnRadians
            double turn = Math.min(angle, maxTurnRadians);
            Vec3 axis = currentDir.cross(desiredDir);
            if (axis.lengthSqr() < 1.0E-8D) {
                // 正好反向：任选一个垂直轴
                axis = currentDir.cross(new Vec3(0.0D, 1.0D, 0.0D));
                if (axis.lengthSqr() < 1.0E-8D) {
                    axis = currentDir.cross(new Vec3(1.0D, 0.0D, 0.0D));
                }
            }
            newDir = rotateAroundAxis(currentDir, axis.normalize(), turn);
        }

        // 关键：保持原速率，只改变方向 —— 不凭空加速
        projectile.setDeltaMovement(newDir.scale(speed));
    }

    /** Rodrigues 旋转公式：把向量 v 绕单位轴 k 旋转 angle 弧度。 */
    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 k, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos)
                .add(k.cross(v).scale(sin))
                .add(k.scale(k.dot(v) * (1.0D - cos)));
    }

    // ==========================================================
    // 闪电目标选择辅助
    // ==========================================================

    /**
     * 找出应该被雷劈的玩家。
     *
     * <p>只要处于雷暴中且附近没有有效避雷针，就返回玩家——
     * <b>不再要求玩家头顶露天</b>，因此地下/屋内也会被劈。</p>
     */
    public static ServerPlayer findLightningTarget(ServerLevel level, Vec3 strikePos) {
        if (!MSConfig.COMMON.lightningTargetsPlayer.get() || !level.isThundering()) {
            return null;
        }
        // 避雷针按原版规则优先：范围内有避雷针就不再管玩家
        if (hasLightningRodNearby(level, BlockPos.containing(strikePos))) {
            return null;
        }

        ServerPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }
            double d = player.position().distanceToSqr(strikePos);
            if (d < bestDist) {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }

    /** 在没有避雷针的雷暴天气里，选出应该被劈的玩家（用于主动落雷）。 */
    public static ServerPlayer pickLightningVictim(ServerLevel level) {
        if (!MSConfig.COMMON.lightningTargetsPlayer.get() || !level.isThundering()) {
            return null;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return null;
        }
        ServerPlayer pick = players.get(level.getRandom().nextInt(players.size()));
        if (!pick.isAlive() || pick.isSpectator() || pick.isCreative()) {
            return null;
        }
        // 该玩家附近有有效避雷针时，这雷就不该劈他
        if (hasLightningRodNearby(level, pick.blockPosition())) {
            return null;
        }
        return pick;
    }

    /**
     * 按原版规则检测避雷针：
     * 用 POI 管理器在配置半径（默认 128 格，同原版）内查找，
     * 且避雷针必须位于地表（Y == 世界表面高度 - 1）。
     */
    public static boolean hasLightningRodNearby(ServerLevel level, BlockPos center) {
        int radius = MSConfig.COMMON.lightningRodRange.get();
        return level.getPoiManager().findClosest(
                holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.LIGHTNING_ROD),
                rodPos -> rodPos.getY() == level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        rodPos.getX(), rodPos.getZ()) - 1,
                center,
                radius,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).isPresent();
    }

    /** 判断某个方块是否为避雷针。 */
    public static boolean isLightningRod(BlockState state) {
        // LIGHTNING_ROD 在 26.3 是 WeatheringCopperCollection，不是普通 Block，因此用标签判断
        return state.is(net.minecraft.tags.BlockTags.LIGHTNING_RODS);
    }
}
