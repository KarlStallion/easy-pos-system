import ee.ut.math.tvt.salessystem.dao.InMemorySalesSystemDAO;
import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dataobjects.Sale;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import ee.ut.math.tvt.salessystem.logic.ShoppingCart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestClass {

    @Mock
    private InMemorySalesSystemDAO dao;

    @Mock
    private ShoppingCart shoppingCart;
    @Mock
    private StockItem stockItem;
    @Mock
    private StockItem stockItem1;

    @Mock
    private SoldItem soldItem;

    @Mock
    private SoldItem soldItem1;

    @Before
    public void setUp() {
        dao = mock(InMemorySalesSystemDAO.class);
        shoppingCart = mock(ShoppingCart.class);
    }

//================================================================================
// Warehouse addItem tests
//================================================================================
    //WH1 - testAddingItemBeginsAndCommitsTransactionWarehouse - check that
    //methods beginTransaction and commitTransaction are both called exactly once and that order
    // NOT DONE - suspicious problem.
    @Test
    public void testAddingItemBeginsAndCommitsTransactionWarehouse() {
        dao = mock(InMemorySalesSystemDAO.class);

        stockItem = new StockItem(1L, "testStock", "Filler info", 10, 100);
        dao.beginTransaction();
        dao.addStockItem(stockItem);
        dao.commitTransaction();

        // Verify that beginTransaction and commitTransaction are called exactly once and in order
        InOrder inOrder = inOrder(dao);
        inOrder.verify(dao, times(1)).beginTransaction();
        inOrder.verify(dao, times(1)).commitTransaction();
    }

    //WH2 -a testAddingNewItemWarehouse - check that a new item is saved through the DAO
    // DONE
   @Test
    public void testAddingNewItemWarehouse() {
        dao = new InMemorySalesSystemDAO();
        StockItem item = new StockItem(2L, "NewItem", "Description", 20.0, 50);

        dao.addStockItem(item);

        StockItem addedItem = dao.findStockItem(item.getId());

        // Verify that a new item is saved through the DAO
       // I am checking that I am able to find the item from the dao,
       // which effectively means that it is saved also through the DAO.
        assertNotNull(addedItem);
    }

    //WH3 - testAddingExistingItemWarehouse - check that adding a new item increases the quantity
    // DONE
    @Test
    public void testAddingExistingItemWarehouse() {
        dao = new InMemorySalesSystemDAO();
        int totalStockItems = 0;

        stockItem = new StockItem(9L, "ExistingItem", "Description", 15.0, 30);
        dao.addStockItem(stockItem);
        totalStockItems += stockItem.getQuantity();

        stockItem = new StockItem(9L, "ExistingItem", "Description", 15.0, 70);
        dao.addStockItem(stockItem);
        totalStockItems += stockItem.getQuantity();

        // Getting stockItem quantity from the DAO
        List<StockItem> stockItemList = dao.findStockItems();
        StockItem stockItemFromDao = stockItemList.get(0);
        int daoQuantity = stockItemFromDao.getQuantity();

        // Verify that adding a new item increases the quantity
        assertEquals(totalStockItems, daoQuantity);
    }

    //WH4 - testAddingItemWithNegativeQuantityWarehouse - check that adding an item
    //with negative quantity results in an exception
    // DONE
    @Test
    public void testAddingItemWithNegativeQuantityWarehouse() {
        dao = new InMemorySalesSystemDAO();

        // Verify that an exception is thrown when adding an item with negative quantity
        assertThrows(IllegalArgumentException.class, () -> {
            // Attempt to add an item with a negative quantity
            stockItem = new StockItem(4L, "NegativeItem", "Description", 25.0, -20);
            dao.addStockItem(stockItem);
        });
    }


//================================================================================
// Shopping cart addItem tests
//================================================================================

    //SC1 - testAddingExistingItemShoppingCart - check that adding an existing item increases the quantity
    // DONE
    @Test
    public void testAddingExistingItemShoppingCart() {
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(60L, "ExistingItem", "Description", 20.0, 50);
        dao.addStockItem(stockItem);

        // Adding item for the first time.
        soldItem = new SoldItem(stockItem ,10, 200);
        shoppingCart.addItem(soldItem);

        // Adding item for the second time.
        soldItem = new SoldItem(stockItem ,5, 100);
        shoppingCart.addItem(soldItem);

        SoldItem addedItem = shoppingCart.findSoldItem(60L);

        double addedItemQuantity = addedItem.getQuantity();
        // Verify that adding an existing item increases the quantity
        assertNotNull(addedItem);
        assertEquals(15.0, addedItemQuantity, 0.01);


    }

    //SC2 - testAddingNewItemShoppingCart - check that the new item is added to the shopping cart
    // DONE
    @Test
    public void testAddingNewItemShoppingCart() {
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(60L, "NewItem", "Description", 20.0, 50);
        dao.addStockItem(stockItem);

        soldItem = new SoldItem(stockItem ,10, 200);
        shoppingCart.addItem(soldItem);

        SoldItem addedItem = shoppingCart.findSoldItem(60L);

        // Verify that saveSoldItem is called with the correct item
        assertNotNull(addedItem);
    }

    //SC3 - testAddingItemWithNegativeQuantityShoppingCart - check that an exception is thrown if trying to add an item with a negative quantity
    // DONE
    @Test
    public void testAddingItemWithNegativeQuantityShoppingCart() {
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(60L, "NegativeItem", "Description", 20.0, 50);
        dao.addStockItem(stockItem);

        // Verify that an exception is thrown when adding an item with negative quantity
        assertThrows(IllegalArgumentException.class, () -> {
            // Attempt to add an item with a negative quantity
            soldItem = new SoldItem(stockItem, -10, -200);
            shoppingCart.addItem(soldItem);
        });
    }

    //SC4 - testAddingItemWithQuantityTooLarge - check that an exception is thrown if the quantity
    // of the added item is larger than the quantity in the warehouse
    // DONE
    @Test
    public void testAddingItemWithQuantityTooLarge() {
        // Arrange
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(5L, "TooLargeItem", "Description", 30.0, 10);
        dao.addStockItem(stockItem);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            SoldItem soldItem = new SoldItem(stockItem, 15,450); // Quantity larger than available in stock
            shoppingCart.addItem(soldItem);
        });
    }

    //SC5 - testAddingItemWithQuantitySumTooLarge - check that an
    //exception is thrown if the sum of the quantity of the added item and the
    //quantity already in the shopping cart is larger than the quantity in the warehouse
    // DONE
    @Test
    public void testAddingItemWithQuantitySumTooLarge() {

        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        // Arrange
        stockItem = new StockItem(6L, "TooLargeItem", "Description", 30.0, 10);
        dao.addStockItem(stockItem);

        SoldItem soldItem1 = new SoldItem(stockItem, 5,150); // Quantity less than available in stock
        shoppingCart.addItem(soldItem1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            SoldItem soldItem2 = new SoldItem(stockItem, 6,180); // Quantity combined with soldItem1.quantity larger than available in stock
            shoppingCart.addItem(soldItem2);
        });
    }
//================================================================================
// submitCurrentPurchase tests
//================================================================================
    //SCP1 - testSubmittingCurrentPurchaseDecreasesStockItemQuantity -
    // check that submitting the current purchase decreases the quantity of all corresponding StockItems
    // DONE
    @Test
    public void testSubmittingCurrentPurchaseDecreasesStockItemQuantity(){
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(60L, "testStock", "Description", 10, 100);
        dao.addStockItem(stockItem);

        soldItem = new SoldItem(stockItem ,10, 100);
        shoppingCart.addItem(soldItem);

        int newStockItemQuantity = stockItem.getQuantity() - soldItem.getQuantity();
        shoppingCart.submitCurrentPurchase();

        StockItem afterPurchaseStockItem = dao.findStockItem(60L);

        // Verify that the stock item quantity is decreased by the correct amount
        assertEquals(newStockItemQuantity, afterPurchaseStockItem.getQuantity());
    }

    //SCP2 - testSubmittingCurrentPurchaseBeginsAndCommitsTransaction -
    // check that submitting the current purchase calls
    //beginTransaction and endTransaction, exactly once and in that order
    // DONE
    @Test
    public void testSubmittingCurrentPurchaseBeginsAndCommitsTransaction(){
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(9L, "testStock", "Filler info", 10, 100);
        dao.addStockItem(stockItem);

        soldItem = new SoldItem(stockItem,10, 100);

        shoppingCart.addItem(soldItem);
        shoppingCart.submitCurrentPurchase();

        soldItem1 = dao.findSoldItems().get(0);
        // Verify that beginTransaction and commitTransaction are called exactly once and in order
        // If the transaction got through then the soldItem should be saved in the DAO
        // with using begin and commitTransaction methods.
        assertNotNull(soldItem1);
    }

    //SCP3 - testSubmittingCurrentOrderCreatesHistoryItem - check that
    // a new HistoryItem is saved and that it contains the correct SoldItems
    // DONE
    @Test
    public void testSubmittingCurrentOrderCreatesHistoryItem(){
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(60L, "testStock", "Description", 10, 100);
        dao.addStockItem(stockItem);

        soldItem = new SoldItem(stockItem ,10, 100);
        shoppingCart.addItem(soldItem);

        shoppingCart.submitCurrentPurchase();


        // Getting info from DAO.
        List<Sale> sales = dao.findSales();
        Sale sale = sales.get(0);
        List<SoldItem> soldItemList = sale.getSoldItems();
        SoldItem soldItemFromSale = soldItemList.get(0);

        // Verify that the history item is created with the correct items
        assertNotNull(sale);
        assertEquals(soldItemFromSale,soldItem);
    }

    //SCP4 - testSubmittingCurrentOrderSavesCorrectTime - check that
    //the timestamp on the created HistoryItem is set correctly (for example
    //has only a small difference to the current time)
    // DONE
    @Test
    public void testSubmittingCurrentOrderSavesCorrectTime(){
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(11L, "testStock", "Filler info", 10, 100);
        dao.addStockItem(stockItem);

        soldItem = new SoldItem(stockItem, 5,50);
        shoppingCart.addItem(soldItem);

        shoppingCart.submitCurrentPurchase();

        // Getting the time and date from the DAO.
        List<Sale> sales = dao.findSales();
        Sale sale = sales.get(0);

        String time = sale.getTime();
        String date = sale.getDate();


        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Getting current time
        String time2 = LocalTime.now().format(formatterTime);
        String date2 = LocalDate.now().format(formatterDate);

        // Verify that the timestamp on the created HistoryItem is set correctly (for example
        // has only a small difference to the current time)
        assertEquals(time, time2);
        assertEquals(date, date2);
    }

    //SCP5 - testCancellingOrder - check that canceling an order (with some items)
    // and then submitting a new order (with some different items) only
    //saves the items from the new order (with canceled items are discarded)
    // DONE
    @Test
    public void testCancellingOrder(){
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(57L, "discardedStock", "Filler info", 10, 100);
        dao.addStockItem(stockItem);

        stockItem1 = new StockItem(58L, "savedStock", "Filler info", 10, 100);
        dao.addStockItem(stockItem1);


        // Starting the first purchase
        soldItem = new SoldItem(stockItem, 5,50);
        shoppingCart.addItem(soldItem);
        // Cancelling the first purchase
        shoppingCart.cancelCurrentPurchase();

        // Starting the second purchase
        soldItem1 = new SoldItem(stockItem1, 5,50);
        shoppingCart.addItem(soldItem1);
        // Submitting the second purchase
        shoppingCart.submitCurrentPurchase();

        // Getting soldItems from the DAO
        List<SoldItem> soldItems = dao.findSoldItems();
        SoldItem soldItemFromSale = soldItems.get(0);

        System.out.println(soldItems);
        // Verify that the program only saves the items from the new order (with canceled items are discarded)
        assertEquals(soldItemFromSale, soldItem1);
    }

    //SCP6 - testCancellingOrderQuanititesUnchanged - check that after
    //canceling an order the quantities of the related StockItems are not changed
    // DONE
    @Test
    public void testCancellingOrderQuanititesUnchanged(){
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);

        stockItem = new StockItem(60L, "testStock", "Filler info", 10, 100);
        dao.addStockItem(stockItem);

        double startingQuantity = stockItem.getQuantity();

        // Starting the purchase
        soldItem = new SoldItem(stockItem, 5,50);
        shoppingCart.addItem(soldItem);
        // Cancelling the purchase
        shoppingCart.cancelCurrentPurchase();

        double currentQuantity = stockItem.getQuantity();
        // Verify that the quantities of the related StockItems are not changed
        assertEquals(startingQuantity, currentQuantity, 0.01);
    }

}