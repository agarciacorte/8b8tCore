package me.txmc.core.patch.listeners;

import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Listener for handling the NBT (Named Binary Tag) data of items in the player's inventory.
 *
 * <p>This class is part of the 8b8tCore plugin and is responsible for ensuring that items containing
 * excessive amounts of data are cleared from the player's inventory upon join.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Detecting items in the player's inventory upon join</li>
 *     <li>Calculating the size of each item's metadata</li>
 *     <li>Clearing items that exceed a specific size threshold</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/03 14:49
 */
public class NbtBanPatch implements Listener {
    private final JavaPlugin plugin;
    private final int MAX_ITEM_SIZE_BYTES;

    public NbtBanPatch(JavaPlugin plugin) {
        this.plugin = plugin;
        this.MAX_ITEM_SIZE_BYTES = plugin.getConfig().getInt("NbtBanItemChecker.maxItemSizeAllowed", 50000);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Iterate through the player's inventory
        for (ItemStack item : player.getInventory().getContents()) {

            if (item != null) {
                // Check if the item is a shulker box
                if (item.getType().toString().endsWith("SHULKER_BOX")) {
                    // Process the shulker box
                    processShulkerBox(player, item);
                } else {
                    // Calculate the size of the item's metadata
                    int itemSize = calculateStringSizeInBytes(item.toString());

                    // Check if the item exceeds the size threshold
                    if (itemSize > MAX_ITEM_SIZE_BYTES) {
                        // Clear the item
                        player.getInventory().remove(item);
                        getLogger().warn("Cleared item in " + player.getName() + "'s inventory with size " + itemSize + " bytes named '" + getItemName(item) + "'");
                        sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", getItemName(item));
                    }
                }
            }
        }
    }

    // Process shulker box and its contents
    private void processShulkerBox(Player player, ItemStack shulkerBoxItem) {

        BlockStateMeta blockStateMeta = (BlockStateMeta) shulkerBoxItem.getItemMeta();
        if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof ShulkerBox) {
            ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
            Inventory shulkerInventory = shulkerBox.getInventory();

        int totalSize = 0;

        // Iterate through the shulker box's inventory
        for (ItemStack item : shulkerInventory.getContents()) {
            if (item != null) {
                // Calculate the size of the item's metadata
                int itemSize = calculateStringSizeInBytes(item.toString());
                totalSize += itemSize;
            }
        }

        // Check if the shulker box exceeds the size threshold
        if (totalSize > MAX_ITEM_SIZE_BYTES) {
            // Clear the shulker box from the player's inventory
            player.getInventory().remove(shulkerBoxItem);
            getLogger().warn("Cleared shulker box in " + player.getName() + "'s inventory with size " + totalSize + " bytes");
            sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", getItemName(shulkerBoxItem));
        }
        }
    }

    // Method to calculate the size of the given string in bytes
    private int calculateStringSizeInBytes(String data)  {
        byte[] byteArray = data.getBytes(StandardCharsets.UTF_8);
        return byteArray.length;
    }

    private String getItemName(ItemStack itemStack) {
        if (itemStack == null) {
            return "";
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return String.valueOf(itemStack.getItemMeta().getDisplayName());
        } else {
            return itemStack.getType().toString().replace("_", " ").toLowerCase();
        }
    }
}
