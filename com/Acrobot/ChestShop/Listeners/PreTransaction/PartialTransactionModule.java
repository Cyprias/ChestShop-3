package com.Acrobot.ChestShop.Listeners.PreTransaction;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.ChestShop.Economy.Economy;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;

import static com.Acrobot.ChestShop.Events.PreTransactionEvent.TransactionOutcome.*;
import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.BUY;
import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.SELL;

/**
 * @author Acrobot
 */
public class PartialTransactionModule implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public static void onPreBuyTransaction(PreTransactionEvent event) {
        if (event.isCancelled() || event.getTransactionType() != BUY) {
            return;
        }

        Player client = event.getClient();
        String clientName = client.getName();
        ItemStack[] stock = event.getStock();

        double price = event.getPrice();
        double pricePerItem = event.getPrice() / getItemCount(stock);
        double walletMoney = Economy.getBalance(clientName);

        if (!Economy.hasEnough(clientName, price)) {
            int amountAffordable = getAmountOfAffordableItems(walletMoney, pricePerItem);

            if (amountAffordable < 1) {
                event.setCancelled(CLIENT_DOES_NOT_HAVE_ENOUGH_MONEY);
                return;
            }

            event.setPrice(amountAffordable * pricePerItem);
            event.setStock(getCountedItemStack(stock, amountAffordable));
        }

        stock = event.getStock();

        if (!InventoryUtil.hasItems(stock, event.getOwnerInventory())) {
            ItemStack[] itemsHad = getItems(stock, event.getOwnerInventory());
            int posessedItemCount = getItemCount(itemsHad);

            if (posessedItemCount <= 0) {
                event.setCancelled(NOT_ENOUGH_STOCK_IN_CHEST);
                return;
            }

            event.setPrice(pricePerItem * posessedItemCount);
            event.setStock(itemsHad);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public static void onPreSellTransaction(PreTransactionEvent event) {
        if (event.isCancelled() || event.getTransactionType() != SELL) {
            return;
        }

        String ownerName = event.getOwner().getName();
        ItemStack[] stock = event.getStock();

        double price = event.getPrice();
        double pricePerItem = event.getPrice() / getItemCount(stock);
        double walletMoney = Economy.getBalance(ownerName);

        if (Economy.isOwnerEconomicallyActive(event.getOwnerInventory()) && !Economy.hasEnough(ownerName, price)) {
            int amountAffordable = getAmountOfAffordableItems(walletMoney, pricePerItem);

            if (amountAffordable < 1) {
                event.setCancelled(SHOP_DOES_NOT_HAVE_ENOUGH_MONEY);
                return;
            }

            event.setPrice(amountAffordable * pricePerItem);
            event.setStock(getCountedItemStack(stock, amountAffordable));
        }

        stock = event.getStock();

        if (!InventoryUtil.hasItems(stock, event.getClientInventory())) {
            ItemStack[] itemsHad = getItems(stock, event.getClientInventory());
            int posessedItemCount = getItemCount(itemsHad);

            if (posessedItemCount <= 0) {
                event.setCancelled(NOT_ENOUGH_STOCK_IN_INVENTORY);
                return;
            }

            event.setPrice(pricePerItem * posessedItemCount);
            event.setStock(itemsHad);
        }
    }

    private static int getAmountOfAffordableItems(double walletMoney, double pricePerItem) {
        return (int) Math.floor(walletMoney / pricePerItem);
    }

    private static int getItemCount(ItemStack[] items) {
        int count = 0;

        for (ItemStack item : items) {
            count += item.getAmount();
        }

        return count;
    }

    private static ItemStack[] getItems(ItemStack[] stock, Inventory inventory) {
        List<ItemStack> neededItems = new LinkedList<ItemStack>();

        boolean itemAdded = false;

        for (ItemStack item : stock) {
            for (ItemStack needed : neededItems) {
                if (MaterialUtil.equals(item, needed)) {
                    needed.setAmount(needed.getAmount() + item.getAmount());
                    itemAdded = true;
                    break;
                }
            }

            if (!itemAdded) {
                neededItems.add(item);
                itemAdded = false;
            }
        }

        List<ItemStack> toReturn = new LinkedList<ItemStack>();

        for (ItemStack item : neededItems) {
            int amount = InventoryUtil.getAmount(item, inventory);

            ItemStack clone = item.clone();
            clone.setAmount(amount > item.getAmount() ? item.getAmount() : amount);

            toReturn.add(clone);
        }

        return toReturn.toArray(stock);
    }

    private static ItemStack[] getCountedItemStack(ItemStack[] stock, int numberOfItems) {
        int left = numberOfItems;
        LinkedList<ItemStack> stacks = new LinkedList<ItemStack>();

        for (ItemStack stack : stock) {
            int count = stack.getAmount();
            ItemStack toAdd;

            if (left > count) {
                toAdd = stack;
                left -= count;
            } else {
                ItemStack clone = stack.clone();

                clone.setAmount(left);
                toAdd = clone;
                left = 0;
            }

            boolean added = false;

            for (ItemStack iStack : stacks) {
                if (MaterialUtil.equals(toAdd, iStack)) {
                    iStack.setAmount(iStack.getAmount() + toAdd.getAmount());
                    added = true;
                    break;
                }
            }

            if (!added) {
                stacks.add(toAdd);
            }

            if (left <= 0) {
                break;
            }
        }

        return stacks.toArray(stock);
    }
}
