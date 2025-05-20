package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.scoreboard.*;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor del scoreboard para miembros del staff
 * Adaptado al nuevo sistema de scoreboards optimizado
 */
public class ScoreboardManager {

    private final ExyliaStaff plugin;
    private final ExyliaScoreboardManager scoreboardManager;
    private final Map<UUID, PlayerScoreboard> activeStaffScoreboards;
    private static final String STAFF_TEMPLATE_ID = "staff_scoreboard";

    public ScoreboardManager(ExyliaStaff plugin, ExyliaScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.activeStaffScoreboards = new HashMap<>();

        registerStaffScoreboardTemplate();
    }

    /**
     * Registra el template del scoreboard de staff
     * Solo necesitamos crearlo una vez, luego lo podemos usar para múltiples jugadores
     */
    private void registerStaffScoreboardTemplate() {
        if (!isStaffScoreboardEnabled()) {
            return;
        }

        DebugUtils.logDebug(plugin.isDebugMode(), "Registrando template del scoreboard de staff");

        String titleString = plugin.getConfigManager().getConfig("config").getString("staff-scoreboard.title",
                "<gradient:#00D6FF:#0080FF><bold>MODO STAFF</bold></gradient>");
        List<String> lines = plugin.getConfigManager().getConfig("config").getStringList("staff-scoreboard.lines");
        int updateInterval = plugin.getConfigManager().getConfig("config").getInt("staff-scoreboard.update-interval", 20);

        ScoreboardTemplateBuilder builder = scoreboardManager.createTemplate(STAFF_TEMPLATE_ID)
                .title(titleString)
                .updateEvery(updateInterval);

        int size = lines.size();
        for (int i = 0; i < size; i++) {
            builder.line(i, ContentProvider.placeholder(lines.get(i)), size - i - 1);
        }

        builder.build();

        DebugUtils.logDebug(plugin.isDebugMode(), "Template del scoreboard de staff registrado correctamente");
    }

    /**
     * Actualiza el template del scoreboard de staff
     * Útil cuando cambia la configuración
     */
    public void updateStaffScoreboardTemplate() {
        DebugUtils.logDebug(plugin.isDebugMode(), "Actualizando template del scoreboard de staff");

        registerStaffScoreboardTemplate();

        updateAllStaffScoreboards();
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

        if (scoreboardManager.getTemplate(STAFF_TEMPLATE_ID) == null) {
            registerStaffScoreboardTemplate();
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerScoreboard playerScoreboard = scoreboardManager.showScoreboard(player, STAFF_TEMPLATE_ID);
            activeStaffScoreboards.put(player.getUniqueId(), playerScoreboard);
            DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard activado para " + player.getName());
        }, 2L);

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

        PlayerScoreboard scoreboard = activeStaffScoreboards.remove(player.getUniqueId());

        scoreboardManager.hideScoreboard(player);

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
        DebugUtils.logDebug(plugin.isDebugMode(), "Actualizando todos los scoreboards de staff activos");

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
        DebugUtils.logDebug(plugin.isDebugMode(), "Limpiando todos los scoreboards de staff");

        for (UUID playerId : new HashMap<>(activeStaffScoreboards).keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                hideStaffScoreboard(player);
            }
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