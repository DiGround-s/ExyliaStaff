package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.scoreboard.*;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final ExyliaStaff plugin;
    private final ExyliaScoreboardManager scoreboardManager;
    private final Map<UUID, PlayerScoreboard> activeStaffScoreboards;
    private static final String STAFF_TEMPLATE_ID = "staff_scoreboard";

    public ScoreboardManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.scoreboardManager = new ExyliaScoreboardManager(plugin);
        this.activeStaffScoreboards = new HashMap<>();

        registerStaffScoreboardTemplate();
    }

    private void registerStaffScoreboardTemplate() {
        if (isStaffScoreboardEnabled()) {
            return;
        }

        DebugUtils.logDebug(plugin.isDebugMode(), "Registrando template del scoreboard de staff");

        String titleString = plugin.getConfigManager().getConfig("modules/scoreboard").getString("title",
                "<gradient:#00D6FF:#0080FF><bold>MODO STAFF</bold></gradient>");
        List<String> lines = plugin.getConfigManager().getConfig("modules/scoreboard").getStringList("lines");
        int updateInterval = plugin.getConfigManager().getConfig("modules/scoreboard").getInt("update-interval", 20);

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

    public void updateStaffScoreboardTemplate() {
        DebugUtils.logDebug(plugin.isDebugMode(), "Actualizando template del scoreboard de staff");

        registerStaffScoreboardTemplate();

        updateAllStaffScoreboards();
    }

    public boolean showStaffScoreboard(Player player) {
        if (hasActiveStaffScoreboard(player)) {
            DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard ya está activo para " + player.getName() + ", no es necesario volver a mostrarlo");
            return true;
        }

        if (isStaffScoreboardEnabled()) {
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

    public void hideStaffScoreboard(Player player) {
        if (!hasActiveStaffScoreboard(player)) {
            DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard no está activo para " + player.getName() + ", no es necesario ocultarlo");
            return;
        }

        PlayerScoreboard scoreboard = activeStaffScoreboards.remove(player.getUniqueId());

        scoreboardManager.hideScoreboard(player);

        DebugUtils.logDebug(plugin.isDebugMode(), "Staff scoreboard desactivado para " + player.getName());

    }

    public boolean hasActiveStaffScoreboard(Player player) {
        return activeStaffScoreboards.containsKey(player.getUniqueId());
    }

    public boolean toggleStaffScoreboard(Player player) {
        if (hasActiveStaffScoreboard(player)) {
            hideStaffScoreboard(player);
            return false;
        } else {
            return showStaffScoreboard(player);
        }
    }

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

    private boolean isStaffScoreboardEnabled() {
        return !plugin.getConfigManager().getConfig("config").getBoolean("modules/scoreboard", true);
    }
}