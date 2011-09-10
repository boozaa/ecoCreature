package se.crafted.chrisb.ecoCreature.managers;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import se.crafted.chrisb.ecoCreature.ecoCreature;
import se.crafted.chrisb.ecoCreature.models.ecoReward;

public class ecoRewardManager
{
    private final ecoCreature plugin;

    private final static String AMOUNT_TOKEN = "<amt>";
    private final static String ITEM_TOKEN = "<itm>";
    private final static String CREATURE_TOKEN = "<crt>";

    public static Boolean isIntegerCurrency;

    public static Boolean canCampSpawner;
    public static Boolean shouldOverrideDrops;
    public static Boolean isFixedDrops;
    public static Boolean shouldClearCampDrops;
    public static int campRadius;
    public static Boolean hasBowRewards;
    public static Boolean hasDeathPenalty;
    public static Boolean isPercentPenalty;
    public static Double penaltyAmount;
    public static Boolean canHuntUnderSeaLevel;
    public static Boolean isWolverineMode;

    public static Boolean shouldOutputMessages;
    public static Boolean shouldOutputNoRewardMessage;
    public static Boolean shouldOutputSpawnerMessage;
    public static String noBowRewardMessage;
    public static String noCampMessage;
    public static String deathPenaltyMessage;

    public static HashMap<String, Double> groupMultiplier = new HashMap<String, Double>();
    public static HashMap<CreatureType, ecoReward> rewards;
    public static ecoReward spawnerReward;

    public ecoRewardManager(ecoCreature plugin)
    {
        this.plugin = plugin;
    }

    public void registerDeathPenalty(Player player)
    {
        if (player == null || !hasDeathPenalty) {
            return;
        }

        Double amount = isPercentPenalty ? plugin.method.getAccount(player.getName()).balance() * (penaltyAmount / 100.0D) : penaltyAmount;
        plugin.method.getAccount(player.getName()).subtract(amount);

        if (ecoRewardManager.shouldOutputMessages) {
            player.sendMessage(deathPenaltyMessage.replaceAll(AMOUNT_TOKEN, plugin.method.format(amount).replaceAll("\\$", "\\\\\\$")));
        }
    }

    public void registerCreatureReward(Player player, CreatureType tamedCreature, CreatureType killedCreature)
    {
        if (player == null || killedCreature == null) {
            return;
        }

        if (!ecoCreature.permissionsHandler.has(player, "ecoCreature.Creature.Craft" + killedCreature.getName())) {
            return;
        }

        ecoReward reward = rewards.get(killedCreature);
        String weaponName = tamedCreature != null ? tamedCreature.getName() : Material.getMaterial(player.getItemInHand().getTypeId()).name();

        registerReward(player, reward, weaponName);
    }

    public void registerSpawnerReward(Player player, Block block)
    {
        if (player == null || block == null) {
            return;
        }

        if (!block.getType().equals(Material.MOB_SPAWNER)) {
            return;
        }

        if (ecoCreature.permissionsHandler.has(player, "ecoCreature.Creature.Spawner")) {

            registerReward(player, spawnerReward, Material.getMaterial(player.getItemInHand().getTypeId()).name());

            for (ItemStack itemStack : spawnerReward.computeDrops()) {
                block.getWorld().dropItemNaturally(block.getLocation(), itemStack);
            }
        }
    }

    private void registerReward(Player player, ecoReward reward, String weaponName)
    {
        Double amount = computeReward(player, reward);

        if (amount > 0.0D) {
            plugin.method.getAccount(player.getName()).add(amount);
            if (ecoRewardManager.shouldOutputMessages) {
                player.sendMessage(reward.getRewardMessage().replaceAll(AMOUNT_TOKEN, plugin.method.format(amount).replaceAll("\\$", "\\\\\\$")).replaceAll(ITEM_TOKEN, toCamelCase(weaponName)).replaceAll(CREATURE_TOKEN, reward.getRewardName()));
            }
        }
        else if (amount < 0.0D) {
            plugin.method.getAccount(player.getName()).subtract(amount);
            if (ecoRewardManager.shouldOutputMessages) {
                player.sendMessage(reward.getPenaltyMessage().replaceAll(AMOUNT_TOKEN, plugin.method.format(amount).replaceAll("\\$", "\\\\\\$")).replaceAll(ITEM_TOKEN, toCamelCase(weaponName)).replaceAll(CREATURE_TOKEN, reward.getRewardName()));
            }
        }
        else {
            if ((ecoRewardManager.shouldOutputMessages) && (ecoRewardManager.shouldOutputNoRewardMessage)) {
                player.sendMessage(reward.getNoRewardMessage().replaceAll(CREATURE_TOKEN, reward.getRewardName()).replaceAll(ITEM_TOKEN, toCamelCase(weaponName)));
            }
        }
    }

    private static Double computeReward(Player player, ecoReward reward)
    {
        Double amount = reward.getRewardAmount();

        if (isIntegerCurrency) {
            amount = (double) Math.round(amount);
        }

        if (groupMultiplier.containsKey(ecoCreature.permissionsHandler.getGroup(player.getWorld().getName(), player.getName()))) {
            amount *= groupMultiplier.get(ecoCreature.permissionsHandler.getGroup(player.getWorld().getName(), player.getName()));
        }

        return amount;
    }

    private static String toCamelCase(String rawItemName)
    {
        String[] rawItemNameParts = rawItemName.split("_");
        String itemName = "";

        for (String itemNamePart : rawItemNameParts) {
            itemName = itemName + " " + toProperCase(itemNamePart);
        }

        if (itemName.trim().equals("Air")) {
            itemName = "Fists";
        }

        if (itemName.trim().equals("Bow")) {
            itemName = "Bow & Arrow";
        }

        return itemName.trim();
    }

    private static String toProperCase(String str)
    {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}