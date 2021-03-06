package com.Acrobot.ChestShop.Listeners.PostTransaction;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Config.Config;
import com.Acrobot.ChestShop.DB.Queue;
import com.Acrobot.ChestShop.DB.Transaction;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import static com.Acrobot.Breeze.Utils.MaterialUtil.getSignName;
import static com.Acrobot.ChestShop.Config.Property.GENERATE_STATISTICS_PAGE;
import static com.Acrobot.ChestShop.Config.Property.LOG_TO_DATABASE;
import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.BUY;

/**
 * @author Acrobot
 */
public class TransactionLogger implements Listener {
    private static final String BUY_MESSAGE = "%1$s bought %2$s for %3$.2f from %4$s at %5$s";
    private static final String SELL_MESSAGE = "%1$s sold %2$s for %3$.2f to %4$s at %5$s";

    @EventHandler
    public static void onTransaction(TransactionEvent event) {
        String template = (event.getTransactionType() == BUY ? BUY_MESSAGE : SELL_MESSAGE);

        StringBuilder items = new StringBuilder(50);

        for (ItemStack item : event.getStock()) {
            items.append(item.getAmount()).append(' ').append(getSignName(item));
        }

        String message = String.format(template,
                event.getClient().getName(),
                items.toString(),
                event.getPrice(),
                event.getOwner().getName(),
                locationToString(event.getSign().getLocation()));

        ChestShop.getBukkitLogger().info(message);
    }

    @EventHandler
    public static void onTransactionLogToDB(TransactionEvent event) {
        if (!Config.getBoolean(LOG_TO_DATABASE) && !Config.getBoolean(GENERATE_STATISTICS_PAGE)) {
            return;
        }

        double pricePerStack = event.getPrice() / event.getStock().length;

        for (ItemStack item : event.getStock()) {
            Transaction transaction = new Transaction();

            transaction.setAmount(item.getAmount());

            transaction.setItemID(item.getTypeId());
            transaction.setItemDurability(item.getDurability());

            transaction.setPrice((float) pricePerStack);

            transaction.setShopOwner(event.getOwner().getName());
            transaction.setShopUser(event.getClient().getName());

            transaction.setSec(System.currentTimeMillis() / 1000);
            transaction.setBuy(event.getTransactionType() == BUY);

            Queue.addToQueue(transaction);
        }
    }

    private static String locationToString(Location loc) {
        return '[' + loc.getWorld().getName() + "] " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
