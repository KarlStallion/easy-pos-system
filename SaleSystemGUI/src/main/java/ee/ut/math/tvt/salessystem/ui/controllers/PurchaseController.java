package ee.ut.math.tvt.salessystem.ui.controllers;

import ee.ut.math.tvt.salessystem.SalesSystemException;
import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;
import ee.ut.math.tvt.salessystem.dataobjects.StockItem;
import ee.ut.math.tvt.salessystem.logic.ShoppingCart;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Modality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Encapsulates everything that has to do with the purchase tab (the tab
 * labelled "Point-of-sale" in the menu). Consists of the purchase menu,
 * current purchase dialog and shopping cart table.
 */
public class PurchaseController implements Initializable {

    private static final Logger log = LogManager.getLogger(PurchaseController.class);
    private final SalesSystemDAO dao;
    private final ShoppingCart shoppingCart;

    @FXML private Button newPurchase, submitPurchase, cancelPurchase, addItemButton;
    @FXML private TextField barCodeField, quantityField, priceField;
    @FXML private TableView<SoldItem> purchaseTableView;
    @FXML private ComboBox<String> nameField;


    public PurchaseController(SalesSystemDAO dao, ShoppingCart shoppingCart) {
        this.dao = dao;
        this.shoppingCart = shoppingCart;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("PurchaseController initialized");
        initializeUI();
        updateNameField();
    }

    private void initializeUI() {
        cancelPurchase.setDisable(true);
        submitPurchase.setDisable(true);
        purchaseTableView.setItems(FXCollections.observableList(shoppingCart.getAll()));
        disableProductField(true);

        barCodeField.focusedProperty().addListener((observable, oldPropertyValue, newPropertyValue) -> {
            if (!newPropertyValue) {
                fillInputsBySelectedStockItem();
            }
        });

        nameField.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                StockItem selectedStockItem = getStockItemByName(newValue);
                if (selectedStockItem != null) {
                    barCodeField.setText(String.valueOf(selectedStockItem.getId()));
                    priceField.setText(String.valueOf(selectedStockItem.getPrice()));
                }
            }
        });

    }
    public void updateNameField() {
        List<String> itemNames = retrieveItemNames();
        nameField.setItems(FXCollections.observableArrayList(itemNames));
    }

    private StockItem getStockItemByName(String itemName) {
        return dao.findStockItems()
                .stream()
                .filter(stockItem -> stockItem.getName().equals(itemName))
                .findFirst()
                .orElse(null);
    }

    private List<String> retrieveItemNames() {
        return dao.findStockItems()
                .stream()
                .map(StockItem::getName)
                .collect(Collectors.toList());
    }

    @FXML
    protected void newPurchaseButtonClicked() {
        log.info("New sale process started");
        try {
            enableInputs();
        } catch (SalesSystemException e) {
            log.error(e.getMessage(), e);
        }
    }

    @FXML
    protected void cancelPurchaseButtonClicked() {
        log.info("Sale cancelled");
        try {
            shoppingCart.cancelCurrentPurchase();
            disableInputs();
            purchaseTableView.refresh();
        } catch (SalesSystemException e) {
            log.error(e.getMessage(), e);
        }
    }

    @FXML
    protected void submitPurchaseButtonClicked() {
        log.info("Sale complete");
        try {
            log.debug("Contents of the current basket:\n" + shoppingCart.getAll());

            double transactionSum = shoppingCart.getAll().stream()
                    .mapToDouble(SoldItem::getTotal)
                    .sum();

            createCompletionMessage(transactionSum);
            shoppingCart.submitCurrentPurchase();
            disableInputs();
            purchaseTableView.refresh();

        } catch (SalesSystemException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void enableInputs() {
        resetProductField();
        disableProductField(false);
        cancelPurchase.setDisable(false);
        submitPurchase.setDisable(false);
        newPurchase.setDisable(true);
        nameField.setDisable(false);
    }

    private void disableInputs() {
        resetProductField();
        cancelPurchase.setDisable(true);
        submitPurchase.setDisable(true);
        newPurchase.setDisable(false);
        disableProductField(true);
        nameField.setDisable(true);
    }

    private void fillInputsBySelectedStockItem() {
        StockItem stockItem = getStockItemByBarcode();
        if (stockItem != null) {
            nameField.setValue(stockItem.getName());
            priceField.setText(String.valueOf(stockItem.getPrice()));
        } else {
            resetProductField();
        }
    }

    private StockItem getStockItemByBarcode() {
        try {
            long code = Long.parseLong(barCodeField.getText());
            return dao.findStockItem(code);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FXML
    public void addItemEventHandler() {
        StockItem stockItem = getSelectedStockItem();

        if(addItemToShoppingCart(stockItem) == null){
            log.debug("Error adding item to shopping cart.");
            return;
        }
        log.debug("Added " + stockItem.getName() + " quantity of " + quantityField.getText() + " to the shopping cart.");

    }

    private String addItemToShoppingCart(StockItem stockItem){
        if (stockItem != null) {
            try {
                String idString = barCodeField.getText();
                String quantityString = quantityField.getText();
                String selectedName = nameField.getValue();
                String priceString = priceField.getText();

                String possibleError = checkingInputs(idString, quantityString, selectedName, priceString);

                if (possibleError != null) {
                    log.error(possibleError);
                    createErrorMessage(possibleError);
                    return null;
                }

                int requestedQuantity = Integer.parseInt(quantityString);

                if(checkWarehouseQuantity(stockItem,requestedQuantity) == null){
                    return null;
                }

                SoldItem soldItem = new SoldItem(stockItem, requestedQuantity, requestedQuantity * stockItem.getPrice());
                shoppingCart.addItem(soldItem);
                purchaseTableView.refresh();
                return "no error";
            } catch (Exception e) {
                throw new RuntimeException(e + ": Error adding item to shopping cart.");
            }
        }
        return null;
    }
    private String checkWarehouseQuantity(StockItem stockItem, int requestedQuantity){
        int itemQuantityLeft = stockItem.getQuantity();

        int totalQuantityInCart = shoppingCart.getTotalQuantityInCart(stockItem);

        if (requestedQuantity > itemQuantityLeft || (requestedQuantity + totalQuantityInCart) > itemQuantityLeft) {
            log.error("There is not enough items in the warehouse!");
            createErrorMessage("There is not enough stock of " + stockItem.getName() + " in the warehouse! There is " + (itemQuantityLeft - totalQuantityInCart) + " left.");
            quantityField.setText(String.valueOf(itemQuantityLeft - totalQuantityInCart));
            return null;
        }
        return "no error";
    }

    private StockItem getSelectedStockItem() {
        String selectedName = nameField.getValue();
        if (selectedName != null && !selectedName.isEmpty()) {
            return getStockItemByName(selectedName);
        } else {
            return getStockItemByBarcode();
        }
    }

    private void disableProductField(boolean disable) {
        addItemButton.setDisable(disable);
        barCodeField.setDisable(disable);
        quantityField.setDisable(disable);
        nameField.setDisable(disable);
        priceField.setDisable(disable);
    }

    private void resetProductField() {
        barCodeField.setText("");
        quantityField.setText("1");
        nameField.setValue("");
        priceField.setText("");
    }

    protected void createCompletionMessage(double transactionSum) {
        Alert.AlertType error = Alert.AlertType.INFORMATION;
        Alert alert = new Alert(error, "");

        alert.initModality(Modality.APPLICATION_MODAL);

        alert.getDialogPane().setContentText("Transaction has been completed successfully. Total price: " + transactionSum);
        alert.getDialogPane().setHeaderText("Success");

        alert.showAndWait();
    }

    protected void createErrorMessage(String errorMessage) {
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
