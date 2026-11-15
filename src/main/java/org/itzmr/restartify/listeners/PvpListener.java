package org.itzmr.restartify.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.itzmr.restartify.config.ConfigManager;
import org.itzmr.restartify.manager.RestartManager;
import org.itzmr.restartify.utils.MessageUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Blocks PvP damage (melee, arrows, tridents, potions, End Crystals, TNT, etc.)
 * while a restart countdown is active.
 */
public final class PvpListener implements Listener {

    private static final long DENY_MESSAGE_COOLDOWN_MS = 2000L;
    private static final long CRYSTAL_TRACK_TTL_MS = 10_000L;

    private static final Set<String> HARMFUL_EFFECT_KEYS = Set.of(
            "instant_damage",
            "harm",
            "poison",
            "wither",
            "weakness",
            "slowness",
            "slow",
            "mining_fatigue",
            "slow_digging",
            "nausea",
            "confusion",
            "blindness",
            "hunger",
            "bad_omen",
            "darkness",
            "oozing",
            "weaving",
            "infested",
            "wind_charged"
    );

    private final ConfigManager config;
    private final RestartManager restartManager;

    private final Map<UUID, Long> lastDenyMessageMillis = new HashMap<>();
    private final Map<UUID, CrystalDetonator> crystalDetonators = new HashMap<>();

    public PvpListener(ConfigManager config, RestartManager restartManager) {
        this.config = config;
        this.restartManager = restartManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!restartManager.isRestartPvpDisabled()) {
            return;
        }

        Entity victimEntity = event.getEntity();

        // Track which player detonated an End Crystal (including chained crystal explosions).
        if (victimEntity instanceof EnderCrystal) {
            Player detonator = resolveAttacker(event.getDamager());
            if (detonator != null) {
                cleanupExpiredCrystals();
                crystalDetonators.put(
                        victimEntity.getUniqueId(),
                        new CrystalDetonator(detonator.getUniqueId(), System.currentTimeMillis())
                );
            }
            return;
        }

        if (!(victimEntity instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        sendDenyMessage(attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!restartManager.isRestartPvpDisabled()) {
            return;
        }

        ProjectileSource shooter = event.getPotion().getShooter();
        if (!(shooter instanceof Player attacker)) {
            return;
        }

        if (!hasHarmfulEffect(event.getPotion().getEffects())) {
            return;
        }

        boolean blockedAny = false;
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player victim && !victim.getUniqueId().equals(attacker.getUniqueId())) {
                event.setIntensity(victim, 0.0D);
                blockedAny = true;
            }
        }

        if (blockedAny) {
            sendDenyMessage(attacker);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastDenyMessageMillis.remove(event.getPlayer().getUniqueId());
    }

    private Player resolveAttacker(Entity damager) {
        if (damager == null) {
            return null;
        }
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player player ? player : null;
        }
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            return source instanceof Player player ? player : null;
        }
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            return (source != null && source != damager) ? resolveAttacker(source) : null;
        }
        if (damager instanceof EnderCrystal) {
            CrystalDetonator detonator = crystalDetonators.get(damager.getUniqueId());
            if (detonator == null) {
                return null;
            }
            if (System.currentTimeMillis() - detonator.timestampMillis() > CRYSTAL_TRACK_TTL_MS) {
                crystalDetonators.remove(damager.getUniqueId());
                return null;
            }
            return Bukkit.getPlayer(detonator.playerId());
        }
        return null;
    }

    private static boolean hasHarmfulEffect(Collection<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (PotionEffect effect : effects) {
            if (effect == null || effect.getType() == null) {
                continue;
            }
            String key = effect.getType().getKey().getKey().toLowerCase(Locale.ROOT);
            if (HARMFUL_EFFECT_KEYS.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private void sendDenyMessage(Player attacker) {
        long now = System.currentTimeMillis();
        UUID uuid = attacker.getUniqueId();
        Long last = lastDenyMessageMillis.get(uuid);
        if (last != null && (now - last) < DENY_MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastDenyMessageMillis.put(uuid, now);
        MessageUtils.send(attacker, config.getPvpDenyComponent());
    }

    private void cleanupExpiredCrystals() {
        if (crystalDetonators.size() < 32) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, CrystalDetonator>> iterator = crystalDetonators.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().timestampMillis() > CRYSTAL_TRACK_TTL_MS) {
                iterator.remove();
            }
        }
    }

    private record CrystalDetonator(UUID playerId, long timestampMillis) {
    }
}
