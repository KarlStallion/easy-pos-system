package ee.ut.math.tvt.salessystem.ui;

import ee.ut.math.tvt.salessystem.dao.HibernateSalesSystemDAO;
import ee.ut.math.tvt.salessystem.dao.SalesSystemDAO;
import ee.ut.math.tvt.salessystem.dao.InMemorySalesSystemDAO;
import ee.ut.math.tvt.salessystem.ui.controllers.HistoryController;
import ee.ut.math.tvt.salessystem.ui.controllers.PurchaseController;
import ee.ut.math.tvt.salessystem.ui.controllers.StockController;
import ee.ut.math.tvt.salessystem.logic.ShoppingCart;
import ee.ut.math.tvt.salessystem.ui.controllers.TeamController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.net.URL;

/**
 * Graphical user interface of the sales system.
 */
public class SalesSystemUI extends Application {

    private static final Logger log = LogManager.getLogger(SalesSystemUI.class);

    private final InMemorySalesSystemDAO dao;
    private final ShoppingCart shoppingCart;
    private Tab purchaseTab;
    private PurchaseController purchaseController;

    public SalesSystemUI() {
        dao = new InMemorySalesSystemDAO();
        shoppingCart = new ShoppingCart(dao);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        initializeLog4j();
        log.info("Setting logger to src/main/resources/log4j2.xml");
        log.info("javafx version: " + System.getProperty("javafx.runtime.version"));

        purchaseTab = new Tab();
        purchaseTab.setText("Point-of-sale");
        purchaseTab.setClosable(false);
        purchaseController = new PurchaseController(dao, shoppingCart);
        purchaseTab.setContent(loadControls("PurchaseTab.fxml", purchaseController));

        purchaseTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                purchaseController.updateNameField();
            }
        });

        Tab stockTab = new Tab();
        stockTab.setText("Warehouse");
        stockTab.setClosable(false);
        stockTab.setContent(loadControls("StockTab.fxml", new StockController(dao)));


        Tab historyTab = new Tab();
        historyTab.setText("History");
        historyTab.setClosable(false);
        historyTab.setContent(loadControls("HistoryTab.fxml", new HistoryController(dao)));

        Tab teamTab = new Tab();
        teamTab.setText("Team");
        teamTab.setClosable(false);
        teamTab.setContent(loadControls("TeamTab.fxml", new TeamController()));

        Group root = new Group();
        Scene scene = new Scene(root, 600, 500, Color.WHITE);
        scene.getStylesheets().add(getClass().getResource("DefaultTheme.css").toExternalForm());

        BorderPane borderPane = new BorderPane();
        borderPane.prefHeightProperty().bind(scene.heightProperty());
        borderPane.prefWidthProperty().bind(scene.widthProperty());
        borderPane.setCenter(new TabPane(purchaseTab, stockTab, historyTab, teamTab));
        root.getChildren().add(borderPane);

        primaryStage.setTitle("Sales system");
        primaryStage.setScene(scene);
        primaryStage.show();

        log.info("Salesystem GUI started");
    }

    private Node loadControls(String fxml, Initializable controller) throws IOException {
        try {
            URL resource = getClass().getResource(fxml);
            if (resource == null) {
                log.error(fxml + " not found!");
                return createErrorNode("FXML file not found.");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(resource);
            fxmlLoader.setController(controller);
            return fxmlLoader.load();
        } catch (IOException e) {
            log.error("Error loading FXML: " + e.getMessage(), e);
            return createErrorNode("Error loading FXML");
        }
    }
    private Node createErrorNode(String errorMessage){
        // Creating a simple error node with the error message
        Text errorText = new Text(errorMessage);
        errorText.setFill(Color.RED);
        errorText.setFont(Font.font(16));
        return new StackPane(errorText);
    }

    private static void initializeLog4j() {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        // Reconfigure Log4j
        Configurator.initialize(null, "log4j2.xml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}


