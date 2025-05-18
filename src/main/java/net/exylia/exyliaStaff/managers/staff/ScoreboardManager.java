package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.scoreboard.ExyliaScoreboardManager;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor del scoreboard para miembros del staff
 */
public class ScoreboardManager {

    private final ExyliaStaff plugin;
    private final ExyliaScoreboardManager scoreboardManager;
    private final Map<UUID, ExyliaScoreboardManager.ExyliaScoreboard> activeStaffScoreboards;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ScoreboardManager(ExyliaStaff plugin, ExyliaScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.activeStaffScoreboards = new HashMap<>();
    }

    /**
     * Muestra el scoreboard de staff a un jugador
     *
     * @param player El jugador al que mostrar el scoreboard
     * @return true si se mostró correctamente, false si no está habilitado o ya estaba activo
     */
    public boolean showStaffScoreboard(Player player) {
        if (hasActiveStaffScoreboard(player)) {
            DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard ya está activo para " + player.getName() + ", no es necesario volver a mostrarlo");
            return true;
        }

        if (!isStaffScoreboardEnabled()) {
            return false;
        }

        String titleString = plugin.getConfigManager().getConfig("config").getString("staff-scoreboard.title", "<gradient:#00D6FF:#0080FF><bold>MODO STAFF</bold></gradient>");
        List<String> lines = plugin.getConfigManager().getConfig("config").getStringList("staff-scoreboard.lines");
        int updateInterval = plugin.getConfigManager().getConfig("config").getInt("staff-scoreboard.update-interval", 20);

        Component title = ColorUtils.translateColors(titleString);

        ExyliaScoreboardManager.ExyliaScoreboard scoreboard = scoreboardManager.getScoreboard(player);

        if (scoreboard != null) {
            scoreboard.destroy();
        }

        scoreboard = scoreboardManager.createScoreboard(player, title, updateInterval);

        int size = lines.size();
        for (int i = 0; i < size; i++) {
            String lineContent = lines.get(i);
            Component lineComponent = ColorUtils.translateColors(lineContent);
            scoreboard.setLine(i, lineComponent, size - i - 1);
        }

        ExyliaScoreboardManager.ExyliaScoreboard finalScoreboard = scoreboard;
        new BukkitRunnable() {
            @Override
            public void run() {
                finalScoreboard.show();
                activeStaffScoreboards.put(player.getUniqueId(), finalScoreboard);
                DebugUtils.logDebug(plugin.isDebugMode(),"Staff scoreboard activado para " + player.getName());
            }
        }.runTaskLater(plugin, 2L);

        return true;
    }

    /**
     * Oculta el scoreboard de staff a un jugador
     *
     * @param player El jugador al que ocultar el scoreboard
     * @return true si se ocultó correctamente, false si no estaba activo
     */
    public boolean hideStaffScoreboard(Player player) {
        if (!hasActiveStaffScoreboard(player)) {
            DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard no está activo para " + player.getName() + ", no es necesario ocultarlo");
            return false;
        }

        ExyliaScoreboardManager.ExyliaScoreboard scoreboard = activeStaffScoreboards.remove(player.getUniqueId());

        scoreboard.hide();

        DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard desactivado para " + player.getName());

        return true;
    }

    /**
     * Verifica si un jugador tiene el scoreboard de staff activo
     *
     * @param player El jugador a verificar
     * @return true si tiene el scoreboard activo, false si no
     */
    public boolean hasActiveStaffScoreboard(Player player) {
        return activeStaffScoreboards.containsKey(player.getUniqueId());
    }

    /**
     * Alterna el estado del scoreboard de staff para un jugador
     *
     * @param player El jugador para alternar el scoreboard
     * @return true si se activó, false si se desactivó
     */
    public boolean toggleStaffScoreboard(Player player) {
        if (hasActiveStaffScoreboard(player)) {
            hideStaffScoreboard(player);
            return false;
        } else {
            return showStaffScoreboard(player);
        }
    }

    /**
     * Actualiza el scoreboard de staff para todos los jugadores activos
     * Útil cuando cambia la configuración
     */
    public void updateAllStaffScoreboards() {
        for (UUID playerId : new HashMap<>(activeStaffScoreboards).keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                hideStaffScoreboard(player);
                showStaffScoreboard(player);
            }
        }
    }

    /**
     * Limpia todos los scoreboards activos
     * Útil al reiniciar o deshabilitar el plugin
     */
    public void cleanup() {
        for (ExyliaScoreboardManager.ExyliaScoreboard scoreboard : activeStaffScoreboards.values()) {
            scoreboard.destroy();
        }
        activeStaffScoreboards.clear();
    }

    /**
     * Verifica si el scoreboard de staff está habilitado en la configuración
     *
     * @return true si está habilitado, false si no
     */
    private boolean isStaffScoreboardEnabled() {
        return plugin.getConfigManager().getConfig("config").getBoolean("staff-scoreboard.enabled", true);
    }
}