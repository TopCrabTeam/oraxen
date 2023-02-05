package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.*;
import java.util.stream.Collectors;

public class RecipesEventsManager implements Listener {

    private static RecipesEventsManager instance;
    private Map<CustomRecipe, String> permissionsPerRecipe = new HashMap<>();
    private Set<CustomRecipe> whitelistedCraftRecipes = new HashSet<>();
    private ArrayList<CustomRecipe> whitelistedCraftRecipesOrdered = new ArrayList<>();

    public static RecipesEventsManager get() {
        if (instance == null) {
            instance = new RecipesEventsManager();
        }
        return instance;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(instance, OraxenPlugin.get());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCrafted(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        Player player = (Player) event.getView().getPlayer();
        if (!hasPermission(player, CustomRecipe.fromRecipe(recipe))) event.getInventory().setResult(null);

        ItemStack result = event.getInventory().getResult();
        if (result == null) return;

        boolean containsOraxenItem = Arrays.stream(event.getInventory().getMatrix()).anyMatch(ingredient -> OraxenItems.exists(OraxenItems.getIdByItem(ingredient)));
        if (!containsOraxenItem || recipe == null) return;

        CustomRecipe current = new CustomRecipe(null, recipe.getResult(), Arrays.asList(event.getInventory().getMatrix()));
        for (CustomRecipe whitelistedRecipe : whitelistedCraftRecipes) {
            if (whitelistedRecipe.equals(current)) return;
        }

        event.getInventory().setResult(null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.discoverRecipes(getPermittedRecipes(player).stream().map(r -> NamespacedKey.fromString(r.getName(), OraxenPlugin.get())).collect(Collectors.toSet()));
    }

    public void resetRecipes() {
        permissionsPerRecipe = new HashMap<>();
        whitelistedCraftRecipes = new HashSet<>();
        whitelistedCraftRecipesOrdered = new ArrayList<>();
    }

    public void addPermissionRecipe(CustomRecipe recipe, String permission) {
        permissionsPerRecipe.put(recipe, permission);
    }

    public void whitelistRecipe(CustomRecipe recipe) {
        whitelistedCraftRecipes.add(recipe);
        whitelistedCraftRecipesOrdered.add(recipe);
    }

    public List<CustomRecipe> getPermittedRecipes(CommandSender sender) {
        return whitelistedCraftRecipesOrdered
                .stream()
                .filter(customRecipe -> !permissionsPerRecipe.containsKey(customRecipe) || hasPermission(sender, customRecipe))
                .toList();
    }

    public String[] getPermittedRecipesName(CommandSender sender) {
        return getPermittedRecipes(sender)
                .stream()
                .map(CustomRecipe::getName)
                .toArray(String[]::new);
    }


    public boolean hasPermission(CommandSender sender, CustomRecipe recipe) {
        return !permissionsPerRecipe.containsKey(recipe) || sender.hasPermission(permissionsPerRecipe.get(recipe));
    }

}
