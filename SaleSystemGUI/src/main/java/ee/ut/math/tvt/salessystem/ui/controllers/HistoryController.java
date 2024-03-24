package ee.ut.math.tvt.salessystem.ui.controllers;

import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dataobjects.Sale;
import ee.ut.math.tvt.salessystem.dataobjects.SoldItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * Encapsulates everything that has to do with the purchase tab (the tab
 * labelled "History" in the menu).
 */
public class HistoryController implements Initializable {
    private static final Logger log = LogManager.getLogger(HistoryController.class);
    private final SalesSystemDAO dao;
    @FXML
    private Button ShowBetweenDates, ShowLast10, ShowAll;
    @FXML
    private DatePicker startDatePicker, endDatePicker;
    @FXML
    private TableView<Sale> historySalesTableView;
    @FXML
    private TableView<SoldItem> historySpecificSaleTableView;
    @FXML
    private TableColumn<SoldItem, Long> idColumn;

    public HistoryController(SalesSystemDAO dao) {
        this.dao = dao;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("HistoryController initialized");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("stockItemId"));
    }

    @FXML
    public void showSaleClicked(MouseEvent event) {
        log.debug("Sale clicked");

        if (event.getClickCount() == 1 && !event.isConsumed()) {
            event.consume();
            Sale sale = getSelectedSale(event);
            if (sale != null) {
                displaySoldItems(sale.getSoldItems());
            }
        }
    }

    private Sale getSelectedSale(MouseEvent event) {
        Node node = ((Node) event.getTarget()).getParent();
        if (node instanceof TableRow) {
            return ((TableRow<Sale>) node).getItem();
        } else if (node.getParent() instanceof TableRow) {
            return ((TableRow<Sale>) node.getParent()).getItem();
        }
        return null;
    }

    private void displaySoldItems(List<SoldItem> soldItems) {
        historySpecificSaleTableView.setItems(FXCollections.observableList(soldItems));
        historySpecificSaleTableView.refresh();
        log.info("Clicked on " + soldItems);
    }

    @FXML
    protected void ShowBetweenDatesButtonClicked() {
        log.debug("Show Between Dates button clicked");
        historySpecificSaleTableView.setItems(FXCollections.emptyObservableList());

        LocalDate startDateValue = startDatePicker.getValue();
        LocalDate endDateValue = endDatePicker.getValue();

        if (startDateValue != null && endDateValue != null) {
            List<Sale> salesWithinRange = findSalesBetweenDates(startDateValue, endDateValue);
            displaySales(salesWithinRange);
            log.info("Showing sales between dates: " + startDateValue + " - " + endDateValue);
        } else {
            handleDateSelectionError("Please fill both: Start date and End date using the datepicker.");
        }
    }

    @FXML
    protected void ShowLast10ButtonClicked() {
        log.debug("Show Last 10 button clicked");
        historySpecificSaleTableView.setItems(FXCollections.emptyObservableList());

        List<Sale> latestSales = getLatestSales(10);
        displaySales(latestSales);
        log.info("Showing last 10 sales.");
    }

    @FXML
    protected void ShowAllButtonClicked() {
        log.info("Showing all sales.");
        historySpecificSaleTableView.setItems(FXCollections.emptyObservableList());
        List<Sale> sales = dao.findSales();
        displaySales(sales);
    }

    private void displaySales(List<Sale> sales) {
        historySalesTableView.setItems(FXCollections.observableList(sales));
        historySalesTableView.refresh();
    }

    private void handleDateSelectionError(String errorMessage) {
        log.error("Error: " + errorMessage);
        createErrorMessage("Error", errorMessage);
    }

    private void createErrorMessage(String headerText, String errorMessage) {
        log.error("Error: " + errorMessage);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.getDialogPane().setContentText(errorMessage);
        alert.getDialogPane().setHeaderText(headerText);
        alert.showAndWait();
    }

    public List<Sale> findSalesBetweenDates(LocalDate startDate, LocalDate endDate) {
        log.debug("Finding sales between dates");
        List<Sale> sales = dao.findSales();
        return sales.stream()
                .filter(sale -> isDateWithinRange(LocalDate.parse(sale.getDate()), startDate, endDate))
                .toList();
    }

    public boolean isDateWithinRange(LocalDate saleDate, LocalDate startDate, LocalDate endDate) {
        log.debug("Checking if date is within range");
        return !saleDate.isBefore(startDate) && !saleDate.isAfter(endDate);
    }

    private List<Sale> getLatestSales(int count) {
        List<Sale> sales = dao.findSales();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return sales.stream()
                .sorted(Comparator.comparing(sale -> parseSaleDate(sdf, sale), Collections.reverseOrder()))
                .limit(count)
                .toList();
    }

    private Date parseSaleDate(SimpleDateFormat sdf, Sale sale) {
        try {
            return sdf.parse(sale.getDate() + " " + sale.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}


