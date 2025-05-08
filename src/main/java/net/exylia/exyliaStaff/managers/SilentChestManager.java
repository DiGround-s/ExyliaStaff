package net.exylia.exyliaStaff.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import net.exylia.exyliaStaff.ExyliaStaff;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SilentChestManager extends PacketListenerAbstract implements Listener {
    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    // Conjunto para rastrear qué cofres están siendo accedidos silenciosamente
    private final Set<ChestLocation> silentChests = new HashSet<>();

    public SilentChestManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;

        // Registrar este manejador como un listener de paquetes
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Solo nos interesan los paquetes de animación de bloques
        if (event.getPacketType() == PacketType.Play.Server.BLOCK_ACTION) {
            WrapperPlayServerBlockAction blockAction = new WrapperPlayServerBlockAction(event);

            // Obtenemos la posición del bloque
            int x = blockAction.getBlockPosition().getX();
            int y = blockAction.getBlockPosition().getY();
            int z = blockAction.getBlockPosition().getZ();

            // Verificamos si este bloque está en nuestra lista de cofres silenciosos
            ChestLocation chestLocation = new ChestLocation(x, y, z);
            if (silentChests.contains(chestLocation)) {
                // Cancelar la animación para todos los jugadores
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffModeManager.isInStaffMode(player)) return;

        // Funcionalidad para abrir cofres silenciosamente
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && isChest(clickedBlock.getType())) {
                // Cancelamos el evento original
                event.setCancelled(true);
                Bukkit.getLogger().info("Cofre abierto silenciosamente por staff");

                // Creamos un objeto para rastrear la ubicación del cofre
                ChestLocation chestLocation = new ChestLocation(
                        clickedBlock.getX(),
                        clickedBlock.getY(),
                        clickedBlock.getZ()
                );

                // Añadimos la posición a la lista de cofres silenciosos
                silentChests.add(chestLocation);

                // Abrimos el inventario del cofre
                if (clickedBlock.getState() instanceof Chest chest) {
                    player.openInventory(chest.getInventory());
                    Bukkit.getLogger().info("Inventario del cofre abierto silenciosamente");

                    // Mensaje opcional para el staff
                    if (plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.silent-chest-feedback", true)) {
                        player.sendMessage(plugin.getConfigManager().getMessage("staff-mode.silent-chest",
                                "%x%", String.valueOf(clickedBlock.getX()),
                                "%y%", String.valueOf(clickedBlock.getY()),
                                "%z%", String.valueOf(clickedBlock.getZ())));
                    }

                    // Programamos la eliminación del cofre de la lista tras un tiempo razonable
                    // como respaldo en caso de que el evento de cierre no se dispare
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        silentChests.remove(chestLocation);
                    }, 100L); // 5 segundos (100 ticks)
                }
                return;
            }
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        // Verificamos si es un ítem de staff y procesamos la acción según corresponda
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
//            staffModeManager.checkStaffItem(player, item);
            // Si es un ítem de staff, cancelamos el evento para evitar interacciones no deseadas
            if (staffModeManager.getStaffItems().isStaffItem(item)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!staffModeManager.isInStaffMode(player)) return;

        // Si el inventario cerrado es un cofre, limpiamos nuestro registro
        if (event.getInventory().getHolder() instanceof Chest chest) {
            ChestLocation chestLocation = new ChestLocation(
                    chest.getX(),
                    chest.getY(),
                    chest.getZ()
            );

            silentChests.remove(chestLocation);
        }
    }

    private boolean isChest(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.ENDER_CHEST;
    }

    // Clase auxiliar para rastrear las ubicaciones de los cofres
    private static class ChestLocation {
        private final int x;
        private final int y;
        private final int z;

        public ChestLocation(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChestLocation that = (ChestLocation) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    // Método para limpiar recursos cuando se descarga el plugin
    public void cleanup() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        silentChests.clear();
    }
}