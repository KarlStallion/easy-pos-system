package ee.ut.math.tvt.salessystem.dao;

import ee.ut.math.tvt.salessystem.dataobjects.Sale;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

public class InMemorySalesSystemDAO implements SalesSystemDAO {
    private static final Logger log = LogManager.getLogger(InMemorySalesSystemDAO.class);
    private final List<StockItem> stockItemList;
    private final List<SoldItem> soldItemList;
    private final List<Sale> salesList;

    public InMemorySalesSystemDAO() {
        List<StockItem> items = new ArrayList<StockItem>();
        items.add(new StockItem(1L, "Lays chips", "Potato chips", 11.0, 5));
        items.add(new StockItem(2L, "Chupa-chups", "Sweets", 8.0, 8));
        items.add(new StockItem(3L, "Frankfurters", "Beer sauseges", 15.0, 12));
        items.add(new StockItem(4L, "Free Beer", "Student's delight", 0.0, 100));
        this.stockItemList = items;

        List<SoldItem> firstSaleItems = new ArrayList<SoldItem>();
        StockItem item1 = new StockItem(1L, "Lays chips", "Potato chips", 11.0, 5);
        StockItem item2 = new StockItem(2L, "Chupa-chups", "Sweets", 8.0, 8);
        firstSaleItems.add(new SoldItem(item1, 4, 4 * item1.getPrice()));
        firstSaleItems.add(new SoldItem(item2, 5, 5 * item2.getPrice()));

        this.soldItemList = firstSaleItems;

        // Calculating sum of firstSaleItems
        double sumFirstSaleItems = 0;
        for (SoldItem soldItem:firstSaleItems) {
            sumFirstSaleItems += soldItem.calculateTotal();
        }

        List<Sale> salesList = new ArrayList<Sale>();

        // Creating a new sale
        String[] currentDateAndTime = getCurrentDateAndTime();

        Sale sale1 = new Sale(currentDateAndTime[0], currentDateAndTime[1], sumFirstSaleItems, firstSaleItems);

        // Create a new list of sold items for each sale
        List<SoldItem> sale1Items = new ArrayList<>(firstSaleItems);

        sale1.setSoldItems(sale1Items);

        salesList.add(sale1);

        this.salesList = salesList;

    }

    @Override
    public List<StockItem> findStockItems() {
        return stockItemList;
    }

    @Override
    public StockItem findStockItem(long id) {
        for (StockItem item : stockItemList) {
            if (item.getId() == id)
                return item;
        }
        return null;
    }
    @Override
    public void saveStockItem(StockItem stockItem) {
        try {
            stockItemList.add(stockItem);
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
    }
    @Override
    public void saveSoldItem(SoldItem item) {
        soldItemList.add(item);
    }


    @Override
    public List<SoldItem> findSoldItems() {
        return soldItemList;
    }

    @Override
    public List<Sale> findSales(){
        return salesList;
    }

    @Override
    public void updateSales(String[] currentDateAndTime, double sum, List<SoldItem> saleItems){
        salesList.add(new Sale(currentDateAndTime[0], currentDateAndTime[1], sum, saleItems));
    }

    @Override
    public void beginTransaction() {
        log.debug("Beginning transaction.");
    }

    @Override
    public void rollbackTransaction() {
    }

    @Override
    public void commitTransaction() {
        log.debug("Committing transaction.");
    }

    @Override
    public void removeStockItem(StockItem oldItem) {

        stockItemList.remove(oldItem);
    }

    @Override
    public void addStockItem(StockItem item){
        long itemBarcode = item.getID();
        int itemQuantity = item.getQuantity();
        String itemName = item.getName();
        double itemPrice = item.getPrice();
        if(itemQuantity < 0){
            log.error("Item quantity cannot be negative.");
            throw new IllegalArgumentException("Item quantity cannot be negative.");
        }
        beginTransaction();
        if (findStockItem(itemBarcode) != null) {
            updateExistingItem(itemBarcode, itemQuantity, itemName, itemPrice);
        } else {
            createNewStockItem(itemBarcode, itemQuantity, itemName, itemPrice);
        }
        commitTransaction();
    }
    @Override
    public void updateExistingItem(long itemBarcode, int itemQuantity, String itemName, double itemPrice) {
        log.debug("Changing the value of an old item with barcode: " + itemBarcode);
        StockItem oldItem = findStockItem(itemBarcode);
        int itemQuantityOld = oldItem.getQuantity();
        oldItem.setQuantity(itemQuantityOld + itemQuantity);
        oldItem.setName(itemName);
        oldItem.setPrice(itemPrice);
        log.debug("Id: " + itemBarcode +
                "\nNew quantity: " + itemQuantityOld + " ---> " + (itemQuantityOld + itemQuantity) +
                "\nName: " + itemName +
                "\nPrice: " + itemPrice);
    }

    @Override
    public void createNewStockItem(long itemBarcode, int itemQuantity, String itemName, double itemPrice) {
        log.debug("Creating a new StockItem from user input.");
        StockItem newItem = new StockItem(itemBarcode, itemName, "Filler info", itemPrice, itemQuantity);
        saveStockItem(newItem);
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
}
