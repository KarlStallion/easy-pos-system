package ee.ut.math.tvt.salessystem.logic;

import ee.ut.math.tvt.salessystem.dataobjects.Sale;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class ShoppingCart {
    private static final Logger log = LogManager.getLogger(ShoppingCart.class);
    private final SalesSystemDAO dao;
    private final List<SoldItem> items = new ArrayList<>();

    public ShoppingCart(SalesSystemDAO dao) {
        this.dao = dao;
    }

    /**
     * Add new SoldItem to table.
     */
    public void addItem(SoldItem item) {
        StockItem stockItem = dao.findStockItem(item.getStockItemId());
        int totalQuantityInCart = getTotalQuantityInCart(stockItem) + item.getQuantity();

        if (item.getQuantity() <= 0 || totalQuantityInCart > stockItem.getQuantity()) {
            throw new IllegalArgumentException("Quantity must be greater than zero and less than or equal to available stock");
        }

        Long idOfNewItem = item.getStockItemId();
        SoldItem existingItem = findSoldItem(idOfNewItem);

        if (existingItem != null) {
            updateExistingItem(existingItem, item);
        } else {
            addNewItem(item);
        }
    }

    private void updateExistingItem(SoldItem oldItem, SoldItem newItem) {
        log.debug("Item already exists in the shopping cart.");
        oldItem.setQuantity(oldItem.getQuantity() + newItem.getQuantity());
        oldItem.setTotal(oldItem.calculateTotal());
        log.debug("Added " + newItem.getName() + " quantity of " + newItem.getQuantity() + " to the shopping cart.");
    }

    public void addNewItem(SoldItem newItem) {
        log.debug("Adding new item to the shopping cart.");
        newItem.setTotal(newItem.calculateTotal());
        items.add(newItem);
        log.debug("Added " + newItem.getName() + " quantity of " + newItem.getQuantity() + " to the shopping cart.");
    }

    public SoldItem findSoldItem(Long id) {
        for (SoldItem soldItem : items) {
            if(soldItem.getStockItemId().equals(id)){
                return soldItem;
            }
        }
        return null;
    }

    public int getTotalQuantityInCart(StockItem stockItem) {
        SoldItem foundItem = findSoldItem(stockItem.getId());
        if(foundItem != null){
            return foundItem.getQuantity();
        }else{
            return 0;
        }
    }

    public List<SoldItem> getAll() {
        return items;
    }

    public void cancelCurrentPurchase() {
        items.clear();
        log.debug("Sale cancelled.");
    }

    public void submitCurrentPurchase() {
        // note the use of transactions. InMemorySalesSystemDAO ignores transactions
        // but when you start using hibernate in lab5, then it will become relevant.
        // what is a transaction? https://stackoverflow.com/q/974596
        //dao.beginTransaction();
        try {
            dao.beginTransaction();

            saveSoldItems();

            // Saving sale
            savePurchaseToHistory();

            dao.commitTransaction();

            // Clear the items list after saving the sale
            items.clear();
            log.info("Sale processed.");
        } catch (Exception e) {
            dao.rollbackTransaction();
            log.error("Submitting purchase failed.");
            throw e;
        }
    }

    private void saveSoldItems() {

        for (SoldItem item : items) {
            dao.saveSoldItem(item);
        }
        // Decreasing warehouse stock
        decreaseWarehouseStock(items);
    }

    public void savePurchaseToHistory(){
        List<Sale> salesList = dao.findSales();
        String[] currentDateAndTime = getCurrentDateAndTime();

        double sum = 0;

        // Create a new list for this sale
        List<SoldItem> saleItems = new ArrayList<>(items);

        // Calculating the full transaction sum.
        for (SoldItem item : items) {
            sum += item.getTotal();
        }

        log.debug("SUM: " + sum);
        dao.updateSales(currentDateAndTime, sum, saleItems);
        System.out.println(salesList);
        items.clear();
        log.debug("Sale saved to history.");
    }
    public String[] getCurrentDateAndTime(){
        String[] currentDateAndTime = new String[2];
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        String formattedDate = dateFormat.format(currentDate);
        String formattedTime = timeFormat.format(currentDate);

        currentDateAndTime[0] = formattedDate;
        currentDateAndTime[1] = formattedTime;

        return currentDateAndTime;
    }

    public void decreaseWarehouseStock(List<SoldItem> items){
        for (SoldItem item : items) {
            long idOfNewItem = item.getStockItemId();
            StockItem stockItem = dao.findStockItem(idOfNewItem);
            stockItem.setQuantity(stockItem.getQuantity() - item.getQuantity());
            // If all items in the warehouse were sold, remove stock from warehouse
            if (stockItem.getQuantity() <= 0) {
                List<StockItem> stockItemList = dao.findStockItems();
                stockItemList.remove(stockItem);
            }
        }
    }
}
