package ee.ut.math.tvt.salessystem.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

import java.io.IOException;
import java.io.InputStream;

public class TeamController implements Initializable {
    private static final Logger log = LogManager.getLogger(TeamController.class);
    @FXML
    private Text teamName, teamLeadName, teamLeadMail, teamMembersName;



    private void loadTeamInfo() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("No application.properties file");
                log.error("There has been an error finding properties file");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            log.debug("Team info has loaded");

            String imagePath = prop.getProperty("team.logo.path");
            Image teamLogo = new Image(imagePath);
            teamName.setText(prop.getProperty("team.name"));
            teamLeadName.setText(prop.getProperty("team.lead"));
            teamLeadMail.setText(prop.getProperty("team.lead.email"));
            teamMembersName.setText(prop.getProperty("team.members"));
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("There has been an error loading team info");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("TeamController initialized");
        loadTeamInfo();
    }
}
