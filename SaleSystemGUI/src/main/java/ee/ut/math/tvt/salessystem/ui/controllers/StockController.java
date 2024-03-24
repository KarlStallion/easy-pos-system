package ee.ut.math.tvt.salessystem.ui.controllers;

import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

/**
 * Encapsulates everything that has to do with the warehouse tab (the tab
 * labelled "Warehouse" in the menu).
 */
public class StockController implements Initializable {

    private final SalesSystemDAO dao;
    private static final Logger log = LogManager.getLogger(StockController.class);

    @FXML
    private Button addItem, removeItem;
    @FXML
    private TextField barCodeField, quantityField, nameField, priceField;
    @FXML
    private TableView<StockItem> warehouseTableView;

    public StockController(SalesSystemDAO dao) {
        this.dao = dao;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("StockController initialized");

        initializeBarcodeField();

        barCodeField.focusedProperty().addListener((observable, oldPropertyValue, newPropertyValue) -> {
            if (!newPropertyValue && !barCodeField.getText().isEmpty() && getStockItemByBarcode() != null) {
                fillInputsBySelectedStockItem();
            }
        });
    }

    private void initializeBarcodeField() {
        List<StockItem> stockItemList = dao.findStockItems();
        if (!stockItemList.isEmpty()) {
            StockItem lastItem = stockItemList.get(stockItemList.size() - 1);
            barCodeField.setText(String.valueOf(lastItem.getId() + 1));
        }
    }

    @FXML
    public void refreshButtonClicked() {
        log.debug("Refresh button pressed");
        refreshStockItems();
    }

    @FXML
    private void refreshStockItems() {
        warehouseTableView.setItems(FXCollections.observableList(dao.findStockItems()));
        warehouseTableView.refresh();
    }

    @FXML
    private StockItem getStockItemByBarcode() {
        try {
            long code = parseLong(barCodeField.getText());
            log.info("Filling inputs for barcode: " + code);
            return dao.findStockItem(code);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FXML
    private void fillInputsBySelectedStockItem() {
        StockItem stockItem = getStockItemByBarcode();
        if (stockItem != null) {
            quantityField.setText("0");
            nameField.setText(stockItem.getName());
            priceField.setText(String.valueOf(stockItem.getPrice()));
        } else {
            resetInputField();
        }
    }

    @FXML
    private void resetInputField() {
        barCodeField.setText("");
        quantityField.setText("0");
        nameField.setText("");
        priceField.setText("");
    }

    @FXML
    protected void addItemButtonClicked() {
        log.debug("Adding items to Warehouse");

        String itemBarcode = barCodeField.getText();
        String itemQuantity = quantityField.getText();
        String itemName = nameField.getText();
        String itemPrice = priceField.getText();

        try {
            String possibleError = checkingInputs(itemBarcode, itemQuantity, itemName, itemPrice);
            if (possibleError != null) {
                createErrorMessage(possibleError);
                resetInputField();
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("\nAdding item to the warehouse with item info: " +
                "\nBarcode: " + itemBarcode +
                "\nQuantity: " + itemQuantity +
                "\nName: " + itemName +
                "\nPrice: " + itemPrice);

        // Adding stockItem to the system.
        dao.addStockItem(new StockItem(parseLong(itemBarcode), itemName, "", parseDouble(itemPrice), parseInt(itemQuantity)));

        resetInputField();
    }


    @FXML
    protected void removeItemButtonClicked() {
        log.debug("Removing items from Warehouse");

        String itemBarcode = barCodeField.getText();
        String itemQuantity = quantityField.getText();

        try {
            long itemBarcodeLong = Long.parseLong(itemBarcode);
            StockItem oldItem = dao.findStockItem(itemBarcodeLong);

            if (oldItem == null) {
                handleItemNotFound();
            } else {
                updateItemQuantity(oldItem, itemQuantity);
            }

            resetInputField();
        } catch (NumberFormatException e) {
            log.error("Invalid barcode format: " + itemBarcode);
            createErrorMessage("Invalid barcode format");
        }
    }

    private void handleItemNotFound() {
        log.debug("\nThe item you are trying to remove does not exist in the warehouse.");
        createErrorMessage("The item you are trying to remove does not exist in the warehouse.");
    }

    private void updateItemQuantity(StockItem oldItem, String itemQuantity) {
        int itemQuantityOld = oldItem.getQuantity();
        int newQuantity = itemQuantityOld - Integer.parseInt(itemQuantity);

        if (newQuantity <= 0) {
            dao.removeStockItem(oldItem);
            log.info("\nRemoving " + oldItem + " from the system");
        } else {
            oldItem.setQuantity(newQuantity);
            log.info("\nChanging the quantity of an item with barcode: " + oldItem.getID() +
                    "\n Old quantity:" + itemQuantityOld +
                    "\n New quantity: " + newQuantity);
        }
    }

    private void createErrorMessage(String errorMessage) {
        Alert.AlertType error = Alert.AlertType.ERROR;
        Alert alert = new Alert(error, "");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.getDialogPane().setContentText(errorMessage);
        alert.getDialogPane().setHeaderText("Error");
        alert.showAndWait();
    }

    public String checkingInputs(String itemBarcode, String itemQuantity, String itemName, String itemPrice) {
        // Regex for checking barcode, quantity and price
        String regexInteger = "\\d+";
        String regexDecimal = "\\d+(\\.\\d+)?";
        Pattern patternInteger = Pattern.compile(regexInteger);
        Pattern patternDecimal = Pattern.compile(regexDecimal);

        Matcher matcherBarcode = patternInteger.matcher(itemBarcode);
        Matcher matcherQuantity = patternInteger.matcher(itemQuantity);
        Matcher matcherPrice = patternDecimal.matcher(itemPrice);

        try{
            // Checking if the barcode field is not empty
            if(Objects.equals(itemBarcode, "")){
                log.error("Please enter a barcode");
                return "Please enter a barcode!";
            }

            // Checking if the barcode is a positive integer
            if (!matcherBarcode.matches()) {
                log.error("Barcode is not right");
                return "Barcode is not correct!";
            }

            // Checking if the quantity field is not empty
            if(Objects.equals(itemQuantity, "")){
                log.error("Please enter a quantity");
                return "Please enter a quantity!";
            }

            // Checking if the quantity is a positive integer
            if (!matcherQuantity.matches()){
                log.error("Quantity is not right");
                return "Please enter a valid quantity!";
            }

            // Checking if the name field is not empty
            if(Objects.equals(itemName, "")){
                log.error("Please enter a name for the item");
                return "Please enter a name for the stock!";
            }

            // Checking that the price field is not empty
            if(Objects.equals(itemPrice, "")){
                log.error("Please enter a price for the item");
                return "Please enter a price for the item!";
            }

            // Checking that the price field is positive integer
            if(!matcherPrice.matches()){
                log.error("Price is not right");
                return "Please enter a valid price!";
            }

        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        return null;
    }
}
