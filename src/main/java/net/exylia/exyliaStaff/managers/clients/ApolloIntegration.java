package net.exylia.exyliaStaff.managers.clients;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.staffmod.StaffModModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ApolloIntegration {
    public static void enableStaffMods(Player player) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(apolloPlayer -> getStaffModModule().enableAllStaffMods(apolloPlayer));
    }

    public static void disableStaffMods(Player player) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(apolloPlayer -> getStaffModModule().disableAllStaffMods(apolloPlayer));
    }

    private static StaffModModule getStaffModModule() {
        return Apollo.getModuleManager().getModule(StaffModModule.class);
    }
}
