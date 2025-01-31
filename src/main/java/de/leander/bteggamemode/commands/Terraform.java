package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.*;
import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Terraform implements CommandExecutor {

    private static int[] preHeight = new int[2];
    private static int height;
    static World world1;
    private static Plugin plugin;
    private static boolean runterterraformen;
    private static Polygonal2DRegion polyRegion;
    private static CuboidRegion cuboidRegion;
    static CuboidClipboard clipboard;
    static ClipboardHolder clipboardHolder;

    static Clipboard backup;
    static BlockVector koordinaten;

    public Terraform(JavaPlugin pPlugin) {
        plugin = pPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("terraform")) {
            if (player.hasPermission("bteg.terraform")) {
                if (args[0].equals("undo")) {
                    load(player);
                    return true;
                } else {
                    height = (Integer.parseInt(args[0]) - 1);
                }
                try {
                    terraform(player);
                } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                    e.printStackTrace();
                }

                world1 = player.getWorld();
                return true;
            }
        } return true;
    }

    void terraform(Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        Region plotRegion;
        // Get WorldEdit selection of player
        try {
            plotRegion = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection(WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            ex.printStackTrace();
            player.sendMessage("§b§lBTEG §7» §cPlease select a WorldEdit selection!");
            return;
        }
        try {
            // Check if WorldEdit selection is polygonal
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                polyRegion = (Polygonal2DRegion) plotRegion;
                if (polyRegion.getLength() > 300 || polyRegion.getWidth() > 300 || polyRegion.getHeight() > 60) {
                    player.sendMessage("§b§lBTEG §7» §cPlease adjust your selection size!");
                    return;
                }
                // Set minimum selection height under player location
                preHeight[0] = polyRegion.getMinimumY();
                preHeight[1] = polyRegion.getMaximumY();

                polyRegion.setMinimumY(height);
                polyRegion.setMaximumY(height + 35);

            } else {
                player.sendMessage("§b§lBTEG §7» §cPlease use poly selection to terraform!");
                return;
            }

        } catch (Exception ex) {
            player.sendMessage("§b§lBTEG §7» §cAn error occurred while select this area!");
            return;
        }

        replaceEmerald(polyRegion, player);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }

    private static void replaceEmerald(Region region, Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        world1 = player.getWorld();
        Vector centerBlock = region.getCenter();
        runterterraformen = height < player.getWorld().getHighestBlockAt(centerBlock.getBlockX(), centerBlock.getBlockZ()).getY() - 1;
        if ( region.getHeight() > 50){
            player.sendMessage("§b§lBTEG §7» §7You cannot terraform areas with height more than 50 blocks difference!");
            return;
        }else {
            player.sendMessage("§b§lBTEG §7» §7Terraforming started. Please wait a short moment!");
          //  player.performCommand("/copy"); // Am anfang benutzt aber speichern des clipboards funktioniert jetzt nur mit der worldedit api
            //WorldEdit CLipboard backup
            backup(polyRegion, player);
            koordinaten = BlockVector.toBlockPoint(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());
           // backup.setOrigin(koordinaten);
            //

            for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                        if (region.contains(new Vector(i, j, k))) {
                            Block block = world1.getBlockAt(i, j, k);
                            if (block.getType().equals(Material.GRASS_PATH)) {
                                block.setType(Material.LAPIS_BLOCK);
                            }


                            if (runterterraformen) {
                                for (int z = j; z >= height; z--) {

                                    if (block.getLocation().getBlockY() == height+1) {
                                        if(block.getType().equals(Material.LAPIS_BLOCK) || block.getType().equals(Material.CONCRETE) || block.getType().equals(Material.BRICK)){

                                        }else{
                                            world1.getBlockAt(i, height, k).setType(Material.EMERALD_BLOCK);
                                        }

                                    }
                                    if (block.getType().equals(Material.LAPIS_BLOCK)) {
                                        world1.getBlockAt(i, z, k).setType(Material.LAPIS_BLOCK);
                                    }
                                    if (block.getType().equals(Material.CONCRETE)) {
                                        world1.getBlockAt(i, z, k).setType(Material.CONCRETE);
                                        world1.getBlockAt(i, z, k).setData((byte) 7);
                                    }
                                    if (block.getType().equals(Material.BRICK)) {
                                        world1.getBlockAt(i, z, k).setTypeId(45, false);
                                    }
                                }
                                for (int z = j; z > height; z--) {
                                    if (block.getLocation().getBlockY() > height + 2) {
                                        world1.getBlockAt(i, z, k).setType(Material.AIR);
                                    }
                                }

                            }

                            if (!runterterraformen) {
                                for (int z = j; z >= region.getMinimumPoint().getBlockY(); z--) {
                                    if (block.getLocation().getBlockY() == (region.getMinimumPoint().getBlockY())) {
                                        if (block.getType().equals(Material.LAPIS_BLOCK) || block.getType().equals(Material.CONCRETE) || block.getType().equals(Material.BRICK)) {
                                        } else {
                                            world1.getBlockAt(i, z, k).setType(Material.EMERALD_BLOCK);
                                        }
                                    }
                                }
                                for (int z = j; z <= height; z++) {
                                    if (block.getLocation().getBlockY() < height) {
                                        if (block.getType().equals(Material.EMERALD_BLOCK)) {
                                            world1.getBlockAt(i, z, k).setType(Material.EMERALD_BLOCK);
                                        }
                                        if (block.getType().equals(Material.LAPIS_BLOCK)) {
                                            world1.getBlockAt(i, z, k).setType(Material.LAPIS_BLOCK);
                                        }
                                        if (block.getType().equals(Material.CONCRETE)) {
                                            world1.getBlockAt(i, z, k).setType(Material.CONCRETE);
                                            world1.getBlockAt(i, z, k).setData((byte) 7);
                                        }
                                        if (block.getType().equals(Material.BRICK)) {
                                            world1.getBlockAt(i, z, k).setTypeId(45, false);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            polyRegion.setMinimumY(preHeight[0]);
            polyRegion.setMaximumY(preHeight[1]);
            player.sendMessage("§b§lBTEG §7» §7Area succesfully terraformed to height §l"+(height+1)+"! Type </terraform undo> for undo.");
        }
    }

    private static void backup(Region pRegion,Player player){
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        WorldEdit we = worldEdit.getWorldEdit();

        WorldData data = polyRegion.getWorld().getWorldData();
        backup = new BlockArrayClipboard(pRegion);

        LocalPlayer localPlayer = worldEdit.wrapPlayer(player);
        LocalSession localSession = we.getSession(localPlayer);
        ClipboardHolder selection = new ClipboardHolder(backup, data); //localSession.getClipboard();
        EditSession editSession = localSession.createEditSession(localPlayer);

        Vector min = selection.getClipboard().getMinimumPoint();
        Vector max = selection.getClipboard().getMaximumPoint();

        editSession.enableQueue();
        clipboard = new CuboidClipboard(max.subtract(min).add(new Vector(1, 1, 1)), min);
        clipboard.copy(editSession);
        editSession.flushQueue();
    }

    private void load(Player player) {
        try {
            //
            EditSession editSession = new EditSession(new BukkitWorld(player.getWorld()), -1);
            editSession.enableQueue();

            clipboard.paste(editSession, koordinaten,false,true);
            editSession.flushQueue();

            player.sendMessage("§b§lBTEG §7» §7Undo succesful!");

            } catch (WorldEditException exception) {
                exception.printStackTrace();
            }

    }
}