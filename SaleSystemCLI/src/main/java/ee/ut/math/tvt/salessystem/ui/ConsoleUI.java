package ee.ut.math.tvt.salessystem.ui;

import ee.ut.math.tvt.salessystem.SalesSystemException;
import ee.ut.math.tvt.salessystem.dao.InMemorySalesSystemDAO;
import ee.ut.math.tvt.salessystem.dao.HibernateSalesSystemDAO;
import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dataobjects.Sale;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import ee.ut.math.tvt.salessystem.logic.ShoppingCart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * A simple CLI (limited functionality).
 */
public class ConsoleUI {
    private static final Logger log = LogManager.getLogger(ConsoleUI.class);

    private final SalesSystemDAO dao;
    private final ShoppingCart cart;

    public ConsoleUI(SalesSystemDAO dao) {
        this.dao = dao;
        cart = new ShoppingCart(dao);
    }

    public static void main(String[] args) throws Exception {
        SalesSystemDAO dao = new InMemorySalesSystemDAO();
        ConsoleUI console = new ConsoleUI(dao);
        console.run();
    }

    /**
     * Run the sales system CLI.
     */
    public void run() throws IOException {
        System.out.println("===========================");
        System.out.println("=       Sales System      =");
        System.out.println("===========================");
        printUsage();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            processCommand(in.readLine().trim().toLowerCase());
            System.out.println("Done. ");
        }
    }

    private void showStock() {
        List<StockItem> stockItems = dao.findStockItems();
        System.out.println("-------------------------");
        for (StockItem si : stockItems) {
            System.out.println(si.getId() + " " + si.getName() + " " + si.getPrice() + "euros (" + si.getQuantity() + " items)");
        }
        if (stockItems.size() == 0) {
            System.out.println("\tNothing");
        }
        System.out.println("-------------------------");
    }

    private void showCart() {
        System.out.println("-------------------------");
        for (SoldItem si : cart.getAll()) {
            System.out.println(si.getName() + " " + si.getPrice() + "euros (" + si.getQuantity() + " items)");
        }
        if (cart.getAll().size() == 0) {
            System.out.println("\tNothing");
        }
        System.out.println("-------------------------");
    }


    private void showTeam() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("No application.properties file");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            log.info("Team info has loaded");

            System.out.println("-------------------------");
            System.out.println("Team name: " + prop.getProperty("team.name"));
            System.out.println("Team lead: " + prop.getProperty("team.lead"));
            System.out.println("Team lead email: " + prop.getProperty("team.lead.email"));
            System.out.println("Team members: " + prop.getProperty("team.members"));
            System.out.println("-------------------------");

        } catch (IOException ex) {
            ex.printStackTrace();
            log.info("There has been an IOExeption while loading team info");
        }

    }

    private void showHistory() {
        System.out.println("-------------------------");
        for (int i = 0; i < dao.findSales().size(); i++) {
            Sale sale = dao.findSales().get(i);

            // Print Sale information
            System.out.println("Sale: " + sale.getDate() + ", " + sale.getTime() + ", " + sale.getTotal() + "euros");

            // Print SoldItems information
            System.out.println("Sold Items:");
            for (int j = 0; j < sale.getSoldItems().size(); j++) {
                SoldItem item = sale.getSoldItems().get(j);
                System.out.println("--" + item.getName() + " " + item.getPrice() + "euros (" + item.getQuantity() + " items)");
            }
            System.out.println("-------------------------");
        }
    }
    private void addToWarehouse(int nr, String name, double price) {
        long id = dao.findStockItems().size() + 1;
        if(nr < 0) {
            log.error("Error: Quantity must be positive");
            return;
        }else if (name.equals("")){
            log.error("Error: Please enter a name");
            return;
        } else if (price < 0){
            log.error("Error: Price must be positive");
            return;
        }
        StockItem item = new StockItem(id, name, "", price, nr);
        dao.addStockItem(item);
        System.out.println("Added " + name + " for " + price + " Euros (" + nr + " items)");
    }
    private void removeFromWarehouse(String[] c) {
        int nr = Integer.parseInt(c[1]);
        long idx = Long.parseLong(c[2]);
        //remove number on stockitem with id
        StockItem item = dao.findStockItem(idx);
        if (item != null) {
            if (item.getQuantity() > nr) {
                item.setQuantity(item.getQuantity() - nr);
                dao.updateExistingItem(item.getId(), item.getQuantity() - nr, item.getName(), item.getPrice());
                System.out.println("Removed " + nr + " of " + item.getName() + " from warehouse");
            } else if (item.getQuantity() == nr) {
                dao.removeStockItem(item);
                System.out.println("Removed " + item.getName() + " from warehouse");
            } else {
                System.out.println("Not enough items in warehouse");
            }
        } else {
            System.out.println("no stock item with id " + idx);
        }
    }
    private void updateExistingItem(String[] c) {
        int nr = Integer.parseInt(c[2]);
        long idx = Long.parseLong(c[1]);
        StockItem item = dao.findStockItem(idx);
        if (item != null) {
            dao.updateExistingItem(item.getId(), item.getQuantity(), item.getName(), item.getPrice());
            System.out.println("Incresed " + item.getName() + " by "  + nr + " in warehouse");

        } else {
            System.out.println("no stock item with id " + idx);
        }
    }

    private void addToCart(String[] c) {
        try {
            try {
                long idx = Long.parseLong(c[1]);
            }catch (NumberFormatException e){
                log.error("ID provided is not a valid ID");
                return;
            }
            long idx = Long.parseLong(c[1]);
            int amount = 0;
            try {
                amount = Integer.parseInt(c[2]);
            } catch (NumberFormatException e) {
                log.error("Error: Quantity provided is not a valid integer");
            }
            StockItem item = dao.findStockItem(idx);
            if (amount > 0) {
                if (item != null) {
                    double total = item.getPrice() * amount;
                    cart.addItem(new SoldItem(item, Math.min(amount, item.getQuantity()), total));
                } else {
                    log.error("Error: No stock item with ID: " + idx);
                }
            } else {
                log.error("Error: Incorrect quantity");
            }
        } catch (SalesSystemException | NoSuchElementException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void showLast10() {
        System.out.println("-------------------------");
        if (dao.findSales().size() > 20) {
            for (int i = dao.findSales().size() - 10; i < dao.findSales().size(); i++) {
                Sale sale = dao.findSales().get(i);
                System.out.println("Sale: " + sale.getDate() + ", " + sale.getTime() + ", " + sale.getTotal() + "euros");
                System.out.println("Sold Items:");
                for (int j = 0; j < sale.getSoldItems().size(); j++) {
                    SoldItem item = sale.getSoldItems().get(j);
                    System.out.println("--" + item.getName() + " " + item.getPrice() + "euros (" + item.getQuantity() + " items)");
                }
                System.out.println("-------------------------");
            }
        } else {
            showHistory();
        }
    }

    private void showBetweenDates(String[] c) {
        System.out.println("-------------------------");
        String[] date1 = c[1].split("-");
        String[] date2 = c[2].split("-");
        int[] date1Int = new int[3];
        int[] date2Int = new int[3];
        for (int i = 0; i < 3; i++) {
            date1Int[i] = Integer.parseInt(date1[i]);
            date2Int[i] = Integer.parseInt(date2[i]);
        }
        for (int i = 0; i < dao.findSales().size(); i++) {
            Sale sale = dao.findSales().get(i);
            String[] date = sale.getDate().split("-");
            int[] dateInt = new int[3];
            for (int j = 0; j < 3; j++) {
                dateInt[j] = Integer.parseInt(date[j]);
            }
            if (Arrays.equals(date1Int, dateInt) || Arrays.equals(date2Int, dateInt)) {
                System.out.println("Sale: " + sale.getDate() + ", " + sale.getTime() + ", " + sale.getTotal() + "euros");
                System.out.println("Sold Items:");
                for (int j = 0; j < sale.getSoldItems().size(); j++) {
                    SoldItem item = sale.getSoldItems().get(j);
                    System.out.println("--" + item.getName() + " " + item.getPrice() + "euros (" + item.getQuantity() + " items)");
                }
                System.out.println("-------------------------");
            } else if (dateInt[0] > date1Int[0] && dateInt[0] < date2Int[0]) {
                System.out.println("Sale: " + sale.getDate() + ", " + sale.getTime() + ", " + sale.getTotal() + "euros");
                System.out.println("Sold Items:");
                for (int j = 0; j < sale.getSoldItems().size(); j++) {
                    SoldItem item = sale.getSoldItems().get(j);
                    System.out.println("--" + item.getName() + " " + item.getPrice() + "euros (" + item.getQuantity() + " items)");
                }
                System.out.println("-------------------------");
            } else if (dateInt[0] == date1Int[0] && dateInt[0] == date2Int[0]) {
                if (dateInt[1] > date1Int[1] && dateInt[1] < date2Int[1]) {
                    System.out.println("Sale: " + sale.getDate() + ", " + sale.getTime() + ", " + sale.getTotal() + "euros");
                    System.out.println("Sold Items:");
                    for (int j = 0; j < sale.getSoldItems().size(); j++) {
                        SoldItem item = sale.getSoldItems().get(j);
                        System.out.println("--" + item.getName() + " " + item.getPrice() + "euros (" + item.getQuantity() + " items)");
                    }
                    System.out.println("-------------------------");
                } else if (dateInt[1] == date1Int[1] && dateInt[1] == date2Int[1]) {
                    if (dateInt[2] > date1Int[2] && dateInt[2] < date2Int[2]) {
                        System.out.println("Sale: " + sale.getDate() + ", " + sale.getTime() + ", " + sale.getTotal() + "euros");
                        System.out.println("Sold Items:");
                        for (int j = 0; j < sale.getSoldItems().size(); j++) {
                            SoldItem item = sale.getSoldItems().get(j);
                            System.out.println("--" + item.getName() + " " + item.getPrice() + "euros (" + item.getQuantity() + " items)");
                        }
                        System.out.println("-------------------------");
                    }
                }
    }}}

    private void printUsage() {
        System.out.println("-------------------------");
        System.out.println("Usage:");
        System.out.println("Structure: <command> <parameter1> <parameter2> ...");
        System.out.println("Example: l 5 Beer 2.5");
        System.out.println("Explanation: Add(l command) 5(QUANTITY) Beers(NAME) that cost 2.5(PRICE) Euros to the warehouse");
        System.out.println("Note: parameters are defined in the brackets under the command name");
        System.out.println("-------------------------");
        System.out.println("Commands:");
        System.out.println("h\t\tShow this help");
        System.out.println("w\t\tShow warehouse contents");
        System.out.println("l\t\tAdd contents with QUANTITY, NAME and PRICE to warehouse (quantity, name, price)");
        System.out.println("rem\t\tRemove NR of contents with INDEX from warehouse (quantity, index)");
        System.out.println("upt\t\tIncrease product with INDEX by NR of contents in the warehouse (index, quantity))");
        System.out.println("c\t\tShow cart contents");
        System.out.println("a\t\tAdd NR of stock item with index IDX to the cart (index, quantity)");
        System.out.println("g\t\tShow history of sales");
        System.out.println("g10\t\tShow last 10 sales");
        System.out.println("gdt\t\tShow sales between dates (ex. 2023-10-10 2023-10-11)");
        System.out.println("p\t\tPurchase the shopping cart");
        System.out.println("r\t\tReset the shopping cart");
        System.out.println("t\t\tShow Team info");
        System.out.println("-------------------------");
    }

    private void processCommand(String command) {
        String[] c = command.split(" ");

        if (c[0].equals("h"))
            printUsage();
        else if (c[0].equals("q"))
            System.exit(0);
        else if (c[0].equals("w"))
            showStock();
        else if (c[0].equals("l") && c.length == 4) {
            int nr = Integer.parseInt(c[1]);
            String name = c[2];
            double price = Double.parseDouble(c[3]);
            addToWarehouse(nr, name, price);
        }
        else if (c[0].equals("rem") && c.length == 3) {
            removeFromWarehouse(c);
        }
        else if (c[0].equals("upt") && c.length == 3) {
            updateExistingItem(c);
        }
        else if (c[0].equals("c"))
            showCart();
        else if (c[0].equals("p"))
            cart.submitCurrentPurchase();
        else if (c[0].equals("r"))
            cart.cancelCurrentPurchase();
        else if (c[0].equals("t"))
            showTeam();
        else if (c[0].equals("g"))
            showHistory();
        else if (c[0].equals("g10"))
            showLast10();
        else if (c[0].equals("gdt") && c.length == 3) {
            showBetweenDates(c);
        }
        else if (c[0].equals("a") && c.length == 3) {
            addToCart(c);
        } else {
            System.out.println("unknown command");
        }
    }


}
