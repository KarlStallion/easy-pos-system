package ee.ut.math.tvt.salessystem.dao;

import ee.ut.math.tvt.salessystem.dataobjects.Sale;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HibernateSalesSystemDAO implements SalesSystemDAO {

    private static final Logger log = LogManager.getLogger(HibernateSalesSystemDAO.class);
    private final EntityManagerFactory emf;
    private final EntityManager em;

    public HibernateSalesSystemDAO() {
        emf = Persistence.createEntityManagerFactory("pos");
        em = emf.createEntityManager();
    }


    public void close() {
        em.close();
        emf.close();
    }

    public void removeStockItem(StockItem stockItem) {
        // Begin transaction
        beginTransaction();

        try {
            // Find the managed entity by ID
            StockItem managedStockItem = em.find(StockItem.class, stockItem.getId());

            if (managedStockItem != null) {
                // Remove the managed entity from the EntityManager
                em.remove(managedStockItem);
            } else {
                // Handle the case where the StockItem is not found
                log.error("StockItem with ID {} not found in the database.", stockItem.getId());
            }
            // Commit the transaction
            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            throw new RuntimeException("Error removing StockItem", e);
        }
    }

    @Override
    public void addStockItem(StockItem stockItem) {

    }
    @Override
    public void updateExistingItem(long itemBarcode, int itemQuantity, String itemName, double itemPrice) {

    }

    @Override
    public void createNewStockItem(long itemBarcode, int itemQuantity, String itemName, double itemPrice){

    }


    public String[] getCurrentDateAndTime() {
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


    public StockItem createStockItem(StockItem stockItem) {
        // Begin transaction
        beginTransaction();

        try {
            // Save or update the StockItem
            if (stockItem.getId() == null) {
                // New StockItem
                em.persist(stockItem);
            } else {
                // Existing StockItem
                em.merge(stockItem);
            }

            // Commit the transaction
            commitTransaction();
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error creating/updating StockItem", e);
        }

        return stockItem;
    }

    @Override
    public List<StockItem> findStockItems() {
        // Begin transaction
        beginTransaction();

        try {
            // Fetch a list of managed StockItem objects
            List<StockItem> stockItems = em.createQuery("SELECT s FROM StockItem s", StockItem.class).getResultList();

            // Commit the transaction
            commitTransaction();

            return stockItems;
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error finding StockItems", e);
        }
    }

    @Override
    public StockItem findStockItem(long id) {
        // Begin transaction
        beginTransaction();

        try {
            // Fetch a managed StockItem by ID
            StockItem stockItem = em.find(StockItem.class, id);

            // Commit the transaction
            commitTransaction();

            return stockItem;
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error finding StockItem by ID", e);
        }
    }

    @Override
    public void saveStockItem(StockItem stockItem) {
        // Begin transaction
        beginTransaction();

        try {
            // Save or update the StockItem
            if (stockItem.getId() == null) {
                // New StockItem
                em.persist(stockItem);
            } else {
                // Existing StockItem
                em.merge(stockItem);
            }

            // Commit the transaction
            commitTransaction();
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error saving StockItem", e);
        } finally {
            close();
        }
    }

    @Override
    public void saveSoldItem(SoldItem item) {
        try {
            beginTransaction();
            // Adding item to em.
            item = em.merge(item);
            // Find the StockItem from the DB
            Long stockItemId = item.getStockItemId();
            StockItem stockItem = em.find(StockItem.class, stockItemId);
            if(stockItem == null){
            }
            item.setStockItemId(stockItem.getId());

            // Saving the SoldItem to the DB
            em.persist(item);

            // Commit the transaction
            commitTransaction();
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error saving SoldItem", e);
        }
    }

    public void decreaseWarehouseStock(List<SoldItem> items){
        try {
            beginTransaction();
            for (SoldItem item : items) {
                log.debug(item);
                StockItem stockItem = em.find(StockItem.class, item.getStockItemId());
                log.debug(stockItem.getQuantity());

                stockItem.setQuantity(stockItem.getQuantity() - item.getQuantity());

                log.debug(stockItem.getQuantity());
                // If all items in the warehouse were sold, remove stock from warehouse
                if (stockItem.getQuantity() == 0) {
                    List<StockItem> stockItemList = findStockItems();
                    stockItemList.remove(stockItem);
                }
            }
            commitTransaction();

        }catch (Exception e){
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            log.error("Error removing StockItem");
            throw new RuntimeException("Error removing StockItem", e);
        }
    }

    @Override
    public void beginTransaction() {
        em.getTransaction().begin();
    }

    @Override
    public void rollbackTransaction() {
        em.getTransaction().rollback();
    }

    @Override
    public void commitTransaction() {
        EntityTransaction transaction = em.getTransaction();
        try {
            if (transaction.isActive()) {
                transaction.commit();
                log.debug("Transaction committed successfully");
            } else {
                log.warn("No active transaction to commit");
            }
        } catch (Exception e) {
            log.error("Error occurred during transaction commit", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    @Override
    public List<SoldItem> findSoldItems() {
        // Begin transaction
        beginTransaction();

        try {
            // Fetch a list of managed SoldItem objects
            List<SoldItem> soldItems = em.createQuery("SELECT si FROM SoldItem si", SoldItem.class).getResultList();

            // Commit the transaction
            commitTransaction();

            return soldItems;
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error finding SoldItems", e);
        }
    }

    @Override
    public List<Sale> findSales() {

        try {
            // Fetch a list of managed Sale objects
            List<Sale> sales = em.createQuery("SELECT s FROM Sale s", Sale.class).getResultList();

            return sales;
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error finding Sales", e);
        }
    }

    @Override
    public void updateSales(String[] currentDateAndTime, double sum, List<SoldItem> saleItems) {

        try {
            // Implement the updateSales method as needed
            Sale newSale = new Sale(currentDateAndTime[0], currentDateAndTime[1], sum, saleItems);
            log.error("Siin1");
            //System.out.println(newSale);

            log.error("katse 1.0");
            // Add all soldItems to the db and set the sale reference for each SoldItem
            for (SoldItem soldItem : saleItems) {
                em.merge(soldItem);
                em.persist(soldItem);
                soldItem.setSale(newSale);
            }
            log.error(saleItems);
            log.error("siin1.2");
            // Adding newSale to DB.
            em.persist(newSale);
            log.error("katse2.0");
            newSale.setSoldItems(saleItems);
            log.error("siin 1.3");
            // Adding newSale to em.
            em.merge(newSale);
            log.error("siin1.5");


            log.error("Siin2");

            // Commit the transaction
            commitTransaction();
        } catch (Exception e) {
            // Handle exceptions, maybe log or rollback the transaction
            rollbackTransaction();
            throw new RuntimeException("Error updating sales", e);
        }
    }

}

