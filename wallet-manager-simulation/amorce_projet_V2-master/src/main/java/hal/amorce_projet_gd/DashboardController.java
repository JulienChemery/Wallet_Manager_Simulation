
package hal.amorce_projet_gd;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.exception.CoinGeckoApiException;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.shape.Rectangle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class DashboardController {
    @FXML
    private TableView<Transaction> historyTable;
    @FXML
    private StackPane contentArea;
    @FXML
    private TableView<CryptoAsset> portfolioTableView;

    @FXML
    private Label balanceLabel;

    private Portfolio portfolio;
    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
    private String username;
    private double balance = 1000.0;
    private User currentUser;

    @FXML
    private StackPane chartContainer;
    @FXML
    private Label selectedPortfolioLabel;
    @FXML
    private Button portfolioButton;

    private Set<String> portfoliosCreatedThisSession = new HashSet<>();

    private String tempSelectedPortfolio = null;
    private String firstCreatedPortfolioName = null;
    private List<String> portfolioCreationOrder = new ArrayList<>();


    private final String ALPHA_VANTAGE_API_KEY = "6TG1JXDF3CEMHBEY";
    private static final String API_KEY = "VZ3OAJEZ6IQ8G2BV";
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    @FXML
    protected void handleResearch() {
        List<String> choices = Arrays.asList("Crypto", "Action");
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Crypto", choices);
        typeDialog.setTitle("Type de Recherche");
        typeDialog.setHeaderText("Choisissez le type de recherche");
        typeDialog.setContentText("Sélectionnez :");

        Optional<String> typeResult = typeDialog.showAndWait();
        typeResult.ifPresent(type -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Recherche");

            dialog.setHeaderText("Choisissez le nom d'une " + type);
            dialog.setContentText("Entrez le nom (minuscule pour crypto, quelconque pour actions.):");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(chosenItem -> {
                if (type.equals("Crypto")) {
                    fetchCryptoValue(chosenItem);
                } else if (type.equals("Action")) {
                    bestMatches(chosenItem);
                }
            });
        });
    }

    public void bestMatches(String chosenItem){
        String url = BASE_URL + "?function=SYMBOL_SEARCH&keywords=" + chosenItem + "&apikey=" + API_KEY;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JSONObject jsonObject = new JSONObject(responseBody);

            if (jsonObject.has("bestMatches")) {
                JSONArray bestMatchesArray = jsonObject.getJSONArray("bestMatches");

                if (bestMatchesArray.length() > 0) {
                    JSONObject match = bestMatchesArray.getJSONObject(0);
                    String symbol = match.getString("1. symbol");
                    System.out.println(symbol);
                    fetchStockValue(symbol);
                } else {
                    showAlert("Aucune correspondance trouvée", "Aucun symbole trouvé pour : " + chosenItem);
                }
            } else {
                showAlert("Erreur de réponse", "Clé API épuisée.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur s'est produite lors de la recherche du symbole.");
        } catch (JSONException e) {
            e.printStackTrace();
            showAlert("Erreur de traitement JSON", "La clé 'bestMatches' n'a pas été trouvée dans la réponse JSON.");
        }
    }

    private void fetchStockValue(String query) {
        String url = BASE_URL + "?function=TIME_SERIES_DAILY&symbol=" + query + "&apikey=" + API_KEY;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JSONObject jsonObject = new JSONObject(responseBody);
            JSONObject timeSeriesDaily = jsonObject.getJSONObject("Time Series (Daily)");
            String latestDate = timeSeriesDaily.names().getString(0);
            JSONObject latestData = timeSeriesDaily.getJSONObject(latestDate);
            String prices = latestData.getString("4. close");


            showAlert("Valeur actuelle", query + " valeur : $" + prices);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void fetchCryptoValue(String query) {
        try {
            Map<String, Map<String, Double>> prices = client.getPrice(query.toLowerCase(), "usd");
            if (prices.containsKey(query)) {
                Double price = prices.get(query).get("usd");
                showAlert("Valeur actuelle", query + " valeur : $" + price);
            } else {
                showAlert("Introuvable", "Pas de donnée trouvée pour " + query);
            }
        } catch (CoinGeckoApiException e) {
            showAlert("Erreur", "Impossible de récupérer des données pour " + query + ". Assurez vous que le nom est correcte.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    protected void handleDeletePortfolio() {
        if (this.portfolio == null) {
            showAlert("Erreur", "Aucun portefeuille sélectionné");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Supprimer un Portefeuille");
        confirmationAlert.setHeaderText("Confirmez la suppression");
        confirmationAlert.setContentText("Êtes-vous sûr de vouloir supprimer le portefeuille '" + this.portfolio.getName() + "' ?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String deletedPortfolioName = this.portfolio.getName();
            String username = currentUser.getUsername();

            DatabaseManager.deletePortfolio(username, deletedPortfolioName);

            currentUser.removePortfolio(deletedPortfolioName);

            this.portfolio = null;

            updatePortfolioDisplay();
            displayPortfolioPieChart();
            selectedPortfolioLabel.setText("Aucun portefeuille sélectionné");

            showAlert("Succès", "Portefeuille supprimé, Veuillez sélectionner un nouveau portefeuille.");
        }
    }


    @FXML
    protected void duplicatePortfolio(String existingPortfolioName, String newPortfolioName) {
        UserManager.savePortfolioData(username, portfolio);

        UserManager.updateUserPortfolio(username, portfolio.getAssets());
        loadUserPortfolios(username);
        UserManager.savePortfolioData(username, portfolio);

        Portfolio existingPortfolio = currentUser.getPortfolio(existingPortfolioName);
        if (existingPortfolio != null) {
            Map<String, Double> duplicatedAssets = new HashMap<>();
            for (String cryptoId : existingPortfolio.getAssets().keySet()) {
                duplicatedAssets.put(cryptoId, 0.0);
            }

            Portfolio newPortfolio = new Portfolio(newPortfolioName, duplicatedAssets, client);
            currentUser.addPortfolio(newPortfolioName, newPortfolio);
            UserManager.saveNewPortfolio(username, newPortfolio);
            this.portfolio = newPortfolio;
            updatePortfolioDisplay();
            displayPortfolioPieChart();

            UserManager.saveNewPortfolio(username, newPortfolio);

            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);


            selectedPortfolioLabel.setText("Portefeuille sélectionné: " + newPortfolioName);
            updatePortfolioDisplay();
        }
    }
    public void setUser(User user) {
        this.currentUser = user;
        this.username = user.getUsername();
        this.balance = user.getBalance();

        updateBalanceLabel();

        UserManager.loadUserPortfolios(currentUser);

        if (tempSelectedPortfolio != null) {
            selectPortfolio(tempSelectedPortfolio);
            tempSelectedPortfolio = null;
        }

        updateButtonAccess();


    }


    private void updateButtonAccess() {
        boolean hasPortfolio = currentUser != null && !currentUser.getPortfolios().isEmpty();
        portfolioButton.setDisable(!hasPortfolio);
    }

    @FXML
    private void initialize() {
        setupHistoryTable();
        setupPortfolioTable();
        updateBalanceLabel();
        selectedPortfolioLabel.setText("Portefeuille sélectionné: Aucun");
        portfolio = null;
    }


    private void displayPortfolioPieChart() {
        if (this.portfolio != null) {
            PieChart pieChart = new PieChart();
            pieChart.setAnimated(true);
            pieChart.setStartAngle(90);
            pieChart.setLabelsVisible(false);
            double totalQuantity = portfolio.getTotalQuantity();

            if (totalQuantity > 0) {
                for (Map.Entry<String, Double> entry : portfolio.getAssets().entrySet()) {
                    String cryptoId = entry.getKey();
                    double quantity = entry.getValue();
                    pieChart.getData().add(new PieChart.Data(cryptoId, quantity));
                }
            } else {
                pieChart.getData().add(new PieChart.Data("No Data", 1));
            }

            String[] colors = {"#FFB6C1", "#ADD8E6", "#90EE90", "#FFFACD", "#E6E6FA", "#FFDAB9", "#D8BFD8", "#FFEFD5"};
            int colorIndex = 0;

            for (PieChart.Data data : pieChart.getData()) {
                data.getNode().setStyle("-fx-pie-color: " + colors[colorIndex++ % colors.length] + ";");
            }

            chartContainer.getChildren().clear();
            chartContainer.getChildren().add(pieChart);

            displayLegend(chartContainer, pieChart, colors, totalQuantity);
        }
    }

    private void displayLegend(StackPane chartContainer, PieChart pieChart, String[] colors, double totalQuantity) {
        VBox legendBox = new VBox(10);
        legendBox.setAlignment(Pos.CENTER);

        int colorIndex = 0;
        for (PieChart.Data data : pieChart.getData()) {
            HBox legendItem = new HBox(5);
            legendItem.setAlignment(Pos.CENTER_LEFT);

            Rectangle colorRect = new Rectangle(10, 10, Color.web(colors[colorIndex++ % colors.length]));

            double percentage = (data.getPieValue() / totalQuantity) * 100;
            String legendText = data.getName() + " - " + String.format("%.2f%%", percentage);
            Label label = new Label(legendText);
            label.setTextFill(Color.WHITE);

            legendItem.getChildren().addAll(colorRect, label);
            legendBox.getChildren().add(legendItem);
        }

        VBox container = new VBox(pieChart, legendBox);
        container.setAlignment(Pos.CENTER);

        chartContainer.getChildren().add(container);
    }



    @FXML
    protected void addNewPortfolio(String portfolioName) {

        if(this.portfolio==null){
            Portfolio newPortfolio = new Portfolio(portfolioName, new HashMap<>(), client);
            currentUser.addPortfolio(portfolioName, newPortfolio);
            UserManager.saveNewPortfolio(username, newPortfolio);
            if (firstCreatedPortfolioName == null) {
                firstCreatedPortfolioName = portfolioName;
            }
            this.portfolio = newPortfolio;
            updatePortfolioDisplay();
            displayPortfolioPieChart();
            updateButtonAccess();
            selectedPortfolioLabel.setText("Portefeuille sélectionné: " + portfolioName);


            updatePortfolioDisplay();


            portfoliosCreatedThisSession.add(portfolioName);
            portfolioCreationOrder.add(portfolioName);
        }else{
            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);

            updatePortfolioDisplay();
            Portfolio newPortfolio = new Portfolio(portfolioName, new HashMap<>(), client);
            currentUser.addPortfolio(portfolioName, newPortfolio);
            UserManager.saveNewPortfolio(username, newPortfolio);
            if (firstCreatedPortfolioName == null) {
                firstCreatedPortfolioName = portfolioName;
            }
            this.portfolio = newPortfolio;
            updatePortfolioDisplay();
            displayPortfolioPieChart();
            updateButtonAccess();
            selectedPortfolioLabel.setText("Portefeuille sélectionné: " + portfolioName);

            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);

            updatePortfolioDisplay();


            portfoliosCreatedThisSession.add(portfolioName);
            portfolioCreationOrder.add(portfolioName);
        }


    }


    private void setupPortfolioTable() {
        TableColumn<CryptoAsset, String> cryptoColumn = new TableColumn<>("Crypto/Action");
        cryptoColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<CryptoAsset, Double> quantityColumn = new TableColumn<>("Quantité");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        portfolioTableView.getColumns().addAll(cryptoColumn, quantityColumn);
    }


    @FXML
    protected void handleBTC() throws Exception {
        contentArea.getChildren().clear();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("btc-view.fxml"));
        Node btcView = loader.load();
        contentArea.getChildren().setAll(btcView);
        portfolioTableView.setVisible(false);
        PortefeuilleController btcController = loader.getController();
        btcController.setCurrentCryptocurrency("bitcoin");
    }

    @FXML
    protected void handleETH() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("eth-view.fxml"));
        try {
            contentArea.getChildren().clear();
            Node ethView = loader.load();
            PortefeuilleController ethController = loader.getController();
            ethController.generateHistoricalDataForEthereum(LocalDate.now().getYear());
            contentArea.getChildren().setAll(ethView);
            portfolioTableView.setVisible(false);
            ethController.setCurrentCryptocurrency("ethereum");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    protected void handlePortfolio() {
        updateBalanceLabel();
        List<String> choices = Arrays.asList("Recharger", "Acheter", "Vendre");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Recharger", choices);
        dialog.setTitle("Mon Portefeuille");
        dialog.setHeaderText("Choisissez une action pour votre portefeuille");
        dialog.setContentText("Sélectionnez :");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::executePortfolioAction);

        portfolioTableView.setVisible(true);

    }

    @FXML
    protected void handlePortefeuille() {
        User updatedUser = UserManager.getUser(username);
        if (updatedUser != null) {
            this.portfolio = new Portfolio("NomGenerique", updatedUser.getPortfolio(), client);
            updatePortfolioDisplay();
        }
        portfolioTableView.setVisible(true);

        contentArea.getChildren().clear();
        contentArea.getChildren().add(portfolioTableView);
    }


    private void handleRecharge() {
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Recharger le Portefeuille");
        dialog.setHeaderText("Rechargement du portefeuille");
        dialog.setContentText("Entrez le montant à recharger:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                balance += amount;
                updateBalanceLabel();
                UserManager.updateUserBalance(username, balance);
                logTransaction("Recharge", "USD", amount, 1.0, portfolio.getName());
            } catch (NumberFormatException e) {
            }
        });
        portfolioTableView.setVisible(true);
    }


    private void fetchAndBuyCrypto(String cryptoId) {
        try {
            Map<String, Map<String, Double>> prices = client.getPrice(cryptoId, "usd");
            Double currentPrice = prices.get(cryptoId).get("usd");
            promptForPurchase(cryptoId, currentPrice);
            UserManager.updateUserPortfolio(username, portfolio.getAssets());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void promptForPurchase(String crypto, double price) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Acheter des cryptomonnaies");
        dialog.setHeaderText("Le prix actuel de " + crypto + " est $" + price);
        dialog.setContentText("Entrer le montant à acheter");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                double unitsToBuy = Double.parseDouble(amountStr);
                double cost = unitsToBuy * price;
                if (balance >= cost) {
                    balance -= cost;
                    portfolio.addToPortfolio(crypto, unitsToBuy);
                    updateBalanceLabel();
                    updatePortfolioDisplay();
                    UserManager.updateUserBalance(username, balance);

                    logTransaction("Acheter", crypto, unitsToBuy, price, portfolio.getName());
                    displayPortfolioPieChart();
                } else {
                    showInsufficientFundsMessage();
                }
            } catch (NumberFormatException e) {
                showInvalidInputMessage();
            }
        });
    }


    private void showInsufficientFundsMessage() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Erreur de Transaction");
        alert.setHeaderText("Solde insuffisant !");
        alert.setContentText("Vous n'avez pas assez de fonds pour réaliser cet achat !");
        alert.showAndWait();
    }

    private void showInvalidInputMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Entrée invalide");
        alert.setHeaderText("Montant d'achat invalide");
        alert.setContentText("Entrez un montant correcte.");
        alert.showAndWait();
    }


    private void logTransaction(String type, String crypto, double amount, double pricePerUnit, String portfolioName) {
        Transaction transaction = new Transaction(
                currentUser.getUsername(),
                type,
                crypto,
                amount,
                pricePerUnit,
                LocalDateTime.now(),
                portfolioName
        );
        UserManager.logTransaction(transaction);

    }


    @FXML
    private void handleBuy() {
        List<String> choices = Arrays.asList("Crypto", "Action");
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Crypto", choices);
        typeDialog.setTitle("Type d'Achat");
        typeDialog.setHeaderText("Choisissez le type d'achat");
        typeDialog.setContentText("Sélectionnez :");

        Optional<String> typeResult = typeDialog.showAndWait();
        typeResult.ifPresent(type -> {
            if (type.equals("Crypto")) {
                buyCrypto();
            } else if (type.equals("Action")) {
                buyStock();
            }
        });
    }

    private void buyCrypto() {

        TextInputDialog cryptoInputDialog = new TextInputDialog();
        cryptoInputDialog.setTitle("Acheter des Cryptomonnaies");
        cryptoInputDialog.setHeaderText("Entrez le nom de la cryptomonnaie que vous souhaitez acheter");
        cryptoInputDialog.getDialogPane().setContentText("Nom de la Cryptomonnaie :");

        cryptoInputDialog.getDialogPane().getButtonTypes().clear();

        TextField researchField = new TextField();
        researchField.setPromptText("Rechercher (en minuscule)");
        cryptoInputDialog.getDialogPane().setContent(researchField);

        ButtonType researchButtonType = new ButtonType("Rechercher", ButtonBar.ButtonData.OK_DONE);
        cryptoInputDialog.getDialogPane().getButtonTypes().add(researchButtonType);

        cryptoInputDialog.setResultConverter(dialogButton -> {
            if (dialogButton == researchButtonType) {
                return researchField.getText();
            }
            return null;
        });

        Optional<String> cryptoResult = cryptoInputDialog.showAndWait();

        if (cryptoResult.isPresent() && !cryptoResult.get().trim().isEmpty()) {
            String chosenCrypto = cryptoResult.get().trim();
            fetchAndBuyCrypto(chosenCrypto);
            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);
        } else {
        }
    }



    private double getCurrentPrice(String stockSymbol) throws Exception {

        String urlS = BASE_URL + "?function=SYMBOL_SEARCH&keywords=" + stockSymbol + "&apikey=" + API_KEY;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlS))
                .build();
        String symbol = "";
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JSONObject jsonObject = new JSONObject(responseBody);

            if (jsonObject.has("bestMatches")) {
                JSONArray bestMatchesArray = jsonObject.getJSONArray("bestMatches");


                JSONObject match = bestMatchesArray.getJSONObject(0);
                symbol = match.getString("1. symbol");
            } else {
                showAlert("Erreur de réponse", "Clé API épuisée.");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        String urlString = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + ALPHA_VANTAGE_API_KEY;
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONObject jsonObject = new JSONObject(new JSONTokener(content.toString()));
        if (!jsonObject.has("Global Quote")) {
            throw new Exception("Global Quote not found for " + symbol);
        }

        JSONObject globalQuote = jsonObject.getJSONObject("Global Quote");
        if (!globalQuote.has("05. price")) {
            throw new Exception("Price not found in Global Quote for " + symbol);
        }

        double currentPrice = globalQuote.getDouble("05. price");

        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Acheter " + symbol);
        amountDialog.setHeaderText("Le prix actuel de " + symbol + " est $" + currentPrice + "\nEntrez la quantité d'actions à acheter");
        amountDialog.setContentText("Quantité:");
        Optional<String> amountResult = amountDialog.showAndWait();
        String finalSymbol = symbol;
        amountResult.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                double transactionAmount = amount * currentPrice;

                if (balance >= transactionAmount) {
                    balance -= transactionAmount;

                    portfolio.addToPortfolio(finalSymbol, amount);

                    updateBalanceLabel();
                    updatePortfolioDisplay();

                    logTransaction("Acheter", finalSymbol, amount, currentPrice, portfolio.getName());
                    displayPortfolioPieChart();

                    UserManager.updateUserPortfolio(username, portfolio.getAssets());
                    UserManager.updateUserBalance(username, balance);
                    UserManager.savePortfolioData(username, portfolio);

                    showAlert("Achat réussi", "Vous avez acheté " + amount + " actions de " + finalSymbol);
                } else {
                    showInsufficientFundsMessage();
                }
            } catch (NumberFormatException e) {
                showInvalidInputMessage();
            }
        });
        return currentPrice;
    }


    private void buyStock() {
        TextInputDialog stockInputDialog = new TextInputDialog();
        stockInputDialog.setTitle("Acheter des Actions");
        stockInputDialog.setHeaderText("Entrez le nom de l'action que vous souhaitez acheter");
        stockInputDialog.setContentText("Nom de l'Action:");

        Optional<String> stockResult = stockInputDialog.showAndWait();
        stockResult.ifPresent(stockSymbol -> {
            try {
                getCurrentPrice(stockSymbol);
            } catch (Exception e) {
                showAlert("Erreur", e.getMessage());
            }
        });
    }

    private void updatePortfolioDisplay() {
        if (this.portfolio != null) {
            portfolioTableView.setItems(FXCollections.observableArrayList(portfolio.getAssetsList()));
            displayPortfolioPieChart();
        }
    }


    @FXML
    private void handleSell() {
        List<String> choices = Arrays.asList("Crypto", "Action");
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Crypto", choices);
        typeDialog.setTitle("Type de Vente");
        typeDialog.setHeaderText("Choisissez le type de vente");
        typeDialog.setContentText("Sélectionnez :");

        Optional<String> typeResult = typeDialog.showAndWait();
        typeResult.ifPresent(type -> {
            if (type.equals("Crypto")) {
                VendreCryypto();
                UserManager.savePortfolioData(username, portfolio);
            } else if (type.equals("Action")) {
                VendreAction();
                UserManager.savePortfolioData(username, portfolio);
            }
        });
    }
    private void VendreCryypto() {
        TextInputDialog cryptoDialog = new TextInputDialog();
        cryptoDialog.setTitle("Vendre des Cryptomonnaies");
        cryptoDialog.setHeaderText("Entrez le nom de la cryptomonnaie à vendre");
        cryptoDialog.setContentText("Nom de la Cryptomonnaie:");
        Optional<String> cryptoResult = cryptoDialog.showAndWait();
        cryptoResult.ifPresent(cryptoId -> {
            try {
                if (!portfolio.getAssets().containsKey(cryptoId)) {
                    showAlert("Erreur", "Vous ne possédez pas cette cryptomonnaie.");
                    return;
                }
                Map<String, Map<String, Double>> prices = client.getPrice(cryptoId, "usd");
                Double currentPrice = prices.get(cryptoId).get("usd");
                promptForSale(cryptoId, currentPrice);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de récupérer le prix pour " + cryptoId);
            }
        });
    }

    private void VendreAction() {
        TextInputDialog stockInputDialog = new TextInputDialog();
        stockInputDialog.setTitle("Vendre des Actions");
        stockInputDialog.setHeaderText("Entrez le nom de l'action que vous souhaitez vendre");
        stockInputDialog.setContentText("Nom de l'Action:");
        Optional<String> stockResult = stockInputDialog.showAndWait();
        stockResult.ifPresent(stockSymbol -> {
            try {
                getCurrentPriceSell(stockSymbol);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de récupérer le prix pour " + stockSymbol);
            }
        });
    }

    private double getCurrentPriceSell(String stockSymbol) throws Exception {

        String urlS = BASE_URL + "?function=SYMBOL_SEARCH&keywords=" + stockSymbol + "&apikey=" + API_KEY;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlS))
                .build();
        String symbol = "";
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();


            JSONObject jsonObject = new JSONObject(responseBody);
            if (jsonObject.has("bestMatches")) {
                JSONArray bestMatchesArray = jsonObject.getJSONArray("bestMatches");


                JSONObject match = bestMatchesArray.getJSONObject(0);
                symbol = match.getString("1. symbol");
            } else {
                showAlert("Erreur de réponse", "Clé API épuisée.");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        String urlString = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + ALPHA_VANTAGE_API_KEY;
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONObject jsonObject = new JSONObject(new JSONTokener(content.toString()));
        if (!jsonObject.has("Global Quote")) {
            throw new Exception("Global Quote not found for " + symbol);
        }

        JSONObject globalQuote = jsonObject.getJSONObject("Global Quote");
        if (!globalQuote.has("05. price")) {
            throw new Exception("Price not found in Global Quote for " + symbol);
        }

        double currentPrice = globalQuote.getDouble("05. price");
        promptForStockSale(symbol, currentPrice);
        return currentPrice;
    }


    private void promptForSale(String cryptoId, double currentPrice) {
        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Vendre des Cryptomonnaies");
        amountDialog.setHeaderText("Le prix actuel de " + cryptoId + " est $" + currentPrice + "\nEntrez la quantité de " + cryptoId + " à vendre");
        amountDialog.setContentText("Quantité:");
        Optional<String> amountResult = amountDialog.showAndWait();
        amountResult.ifPresent(amountStr -> {
            try {
                double amountToSell = Double.parseDouble(amountStr);
                double totalSale = amountToSell * currentPrice;
                CryptoAsset asset = portfolio.getAsset(cryptoId);

                if (asset != null && amountToSell <= asset.getQuantity()) {
                    balance += totalSale;
                    portfolio.removeQuantity(cryptoId, amountToSell);
                    updateBalanceLabel();
                    updatePortfolioDisplay();
                    UserManager.updateUserBalance(username, balance);
                    UserManager.updateUserPortfolio(username, portfolio.getAssets());
                    logTransaction("Vendre", cryptoId, amountToSell, currentPrice, portfolio.getName());
                    displayPortfolioPieChart();
                } else {
                    showInsufficientAssetMessage(cryptoId);
                }
            } catch (NumberFormatException e) {
                showInvalidInputMessage();
            }
        });
    }
    private void promptForStockSale(String stockSymbol, double currentPrice) {
        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Vendre des Actions");
        amountDialog.setHeaderText("Le prix actuel de " + stockSymbol + " est $" + currentPrice + "\nEntrez la quantité d'actions de " + stockSymbol + " à vendre");
        amountDialog.setContentText("Quantité:");
        Optional<String> amountResult = amountDialog.showAndWait();
        amountResult.ifPresent(amountStr -> {
            try {
                double amountToSell = Double.parseDouble(amountStr);
                double totalSale = amountToSell * currentPrice;
                CryptoAsset asset = portfolio.getAsset(stockSymbol);

                if (asset != null && amountToSell <= asset.getQuantity()) {
                    balance += totalSale;
                    portfolio.removeQuantity(stockSymbol, amountToSell);
                    updateBalanceLabel();
                    updatePortfolioDisplay();
                    UserManager.updateUserBalance(username, balance);
                    UserManager.updateUserPortfolio(username, portfolio.getAssets());
                    logTransaction("Vendre", stockSymbol, amountToSell, currentPrice, portfolio.getName());
                    displayPortfolioPieChart();
                } else {
                    showInsufficientAssetMessage(stockSymbol);
                }
            } catch (NumberFormatException e) {
                showInvalidInputMessage();
            }
        });
    }


    private void showInsufficientAssetMessage(String cryptoName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Erreur de la vente de la crypto");
        alert.setHeaderText(cryptoName + "insuffisant !");
        alert.setContentText("Vous n'avez pas assez de " + cryptoName + " pour pouvoir vendre.");
        alert.showAndWait();
    }


    private void selectPortfolio(String portfolioName) {
        UserManager.updateUserPortfolio(username, portfolio.getAssets());
        loadUserPortfolios(username);
        UserManager.savePortfolioData(username, portfolio);
        this.portfolio = currentUser.getPortfolio(portfolioName);
        if (this.portfolio == null) {
            this.portfolio = new Portfolio(portfolioName, new HashMap<>(), client);
            currentUser.addPortfolio(portfolioName, this.portfolio);
        }
        UserManager.loadPortfolioData(this.portfolio);
        updatePortfolioDisplay();
        displayPortfolioPieChart();
        selectedPortfolioLabel.setText("Portefeuille sélectionné: " + portfolioName);
        UserManager.updateUserPortfolio(username, portfolio.getAssets());
        loadUserPortfolios(username);
        UserManager.savePortfolioData(username, portfolio);
    }

    private void updateBalanceLabel() {
        balanceLabel.setText(String.format("Solde du compte : %.2f $", balance));
    }

    @FXML
    protected void handleSelectPortfolio() {
        List<String> portfolioNames = new ArrayList<>(currentUser.getPortfolios().keySet());
        ChoiceDialog<String> dialog = new ChoiceDialog<>("", portfolioNames);
        dialog.setTitle("Sélectionner un Portefeuille");
        dialog.setHeaderText("Choisissez un portefeuille à gérer");
        dialog.setContentText("Portefeuilles disponibles:");
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String selectedPortfolioName = result.get();
            this.portfolio = currentUser.getPortfolio(selectedPortfolioName);
            updatePortfolioDisplay();
            displayPortfolioPieChart();
            selectedPortfolioLabel.setText("Portefeuille sélectionné : " + selectedPortfolioName);
            UserManager.loadUserPortfolios(currentUser);
            portfolioTableView.setVisible(true);
        }
    }




    @FXML
    protected void handleDuplicatePortfolio() {

        UserManager.updateUserPortfolio(username, portfolio.getAssets());
        loadUserPortfolios(username);
        UserManager.savePortfolioData(username, portfolio);

        updatePortfolioDisplay();
        List<String> portfolioNames = new ArrayList<>(currentUser.getPortfolios().keySet());
        ChoiceDialog<String> dialog = new ChoiceDialog<>("", portfolioNames);
        dialog.setTitle("Dupliquer un Portefeuille");
        dialog.setHeaderText("Choisissez un portefeuille à dupliquer");
        dialog.setContentText("Portefeuilles disponibles:");
        updatePortfolioDisplay();
        UserManager.savePortfolioData(username, portfolio);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(existingPortfolioName -> {
            String newPortfolioName = existingPortfolioName + "_copy";
            duplicatePortfolio(existingPortfolioName, newPortfolioName);


            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);

            UserManager.savePortfolioData(username, portfolio);
            updatePortfolioDisplay();
        });
    }



    private void executePortfolioAction(String action) {
        if (portfolio == null) {
            showAlert("Action impossible", "Veuillez créer ou sélectionner un portefeuille.");
            return;
        }
        switch (action) {
            case "Recharger":
                handleRecharge();
                break;
            case "Acheter":
                handleBuy();
                break;
            case "Vendre":
                handleSell();
                break;
            default:
                showAlert("Action non reconnue", "L'action sélectionnée n'est pas reconnue.");
        }
    }

    public void loadUserPortfolios(String username) {
        String sql = "SELECT * FROM portfolios WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String portfolioName = rs.getString("name");
                int portfolioId = rs.getInt("id");
                Portfolio portfolio = new Portfolio(portfolioName, new HashMap<>(), client);
                loadPortfolioAssets(portfolio, portfolioId);
                currentUser.addPortfolio(portfolioName, portfolio);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadPortfolioAssets(Portfolio portfolio, int portfolioId) {
        String sql = "SELECT * FROM portfolio_assets WHERE portfolio_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, portfolioId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String cryptoId = rs.getString("crypto_id");
                double quantity = rs.getDouble("quantity");
                portfolio.addToPortfolio(cryptoId, quantity);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }


    @FXML
    protected void handleCreateNewPortfolio() {
        if(this.portfolio==null) {
            TextInputDialog dialog = new TextInputDialog("Mon Nouveau Portefeuille");
            dialog.setTitle("Créer un Nouveau Portefeuille");
            dialog.setHeaderText("Entrez le nom pour le nouveau portefeuille");
            dialog.setContentText("Nom du portefeuille:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(this::addNewPortfolio);
        }else{
            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);
            TextInputDialog dialog = new TextInputDialog("Mon Nouveau Portefeuille");
            dialog.setTitle("Créer un Nouveau Portefeuille");
            dialog.setHeaderText("Entrez le nom pour le nouveau portefeuille");
            dialog.setContentText("Nom du portefeuille:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(this::addNewPortfolio);
            UserManager.updateUserPortfolio(username, portfolio.getAssets());
            loadUserPortfolios(username);
            UserManager.savePortfolioData(username, portfolio);
            updatePortfolioDisplay();
        }
    }

    @FXML
    protected void handleHistory() {
        contentArea.getChildren().clear();
        portfolioTableView.setVisible(false);

        List<Transaction> transactions = UserManager.getTransactions(currentUser.getUsername());
        ObservableList<Transaction> data = FXCollections.observableArrayList(transactions);
        historyTable.setItems(data);
        historyTable.setVisible(true);

        if (!contentArea.getChildren().contains(historyTable)) {
            contentArea.getChildren().add(historyTable);
        }}
    private void setupHistoryTable() {
        TableColumn<Transaction, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Transaction, String> cryptoColumn = new TableColumn<>("Crypto/Actions");
        cryptoColumn.setCellValueFactory(new PropertyValueFactory<>("crypto"));

        TableColumn<Transaction, Number> amountColumn = new TableColumn<>("Montant");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Transaction, Number> priceColumn = new TableColumn<>("Prix/Unité");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("pricePerUnit"));

        TableColumn<Transaction, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));

        TableColumn<Transaction, String> portfolioColumn = new TableColumn<>("Portefeuille");
        portfolioColumn.setCellValueFactory(new PropertyValueFactory<>("portfolioName"));

        historyTable.getColumns().addAll(typeColumn, cryptoColumn, amountColumn, priceColumn, dateColumn, portfolioColumn);
    }



}