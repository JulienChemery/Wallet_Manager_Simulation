package hal.amorce_projet_gd;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.scene.Node;

import java.util.Collections;
import java.util.stream.Collectors;

public class PortefeuilleController {

    @FXML
    public void initialize() {
        initializeYearSelector();
        yearSelector.getSelectionModel().select(Integer.valueOf(LocalDate.now().getYear()));
    }

    @FXML
    private LineChart<String, Number> lineChart;
    private final CoinGeckoApiClient client = new CoinGeckoApiClientImpl();

    private final Color[] colors = {
            Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW, Color.ORANGE, Color.PINK,
            Color.PURPLE, Color.AQUA, Color.BROWN, Color.CHOCOLATE, Color.CORAL, Color.DARKBLUE
    };
    @FXML
    private ComboBox<Integer> yearSelector;

    private String currentCryptocurrency = "bitcoin";



    private void initializeYearSelector() {
        yearSelector.setItems(FXCollections.observableArrayList(2022, 2023, 2024));
        yearSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (currentCryptocurrency.equals("bitcoin")) {
                    generateHistoricalDataForYear(newValue);
                } else if (currentCryptocurrency.equals("ethereum")) {
                    generateHistoricalDataForEthereum(newValue);
                }
            }
        });
    }

    public void setCurrentCryptocurrency(String cryptocurrency) {
        this.currentCryptocurrency = cryptocurrency;
    }
    private void generateHistoricalDataForYear(int year) {
        lineChart.getData().clear();

        Map<String, Double> historicalData = fetchHistoricalBitcoinData(year);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bitcoin " + year);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = startOfYear.withDayOfYear(startOfYear.lengthOfYear());
        if (year == LocalDate.now().getYear()) {
            endOfYear = LocalDate.now();
        }

        LocalDate date = startOfYear;
        while (!date.isAfter(endOfYear)) {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Double value = historicalData.getOrDefault(formattedDate, null);
            if (value != null) {
                String label = date.format(DateTimeFormatter.ofPattern("dd MMM"));
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, value);
                Node node = new Circle(0);
                node.setVisible(false);
                dataPoint.setNode(node);
                series.getData().add(dataPoint);
            }
            if ((year == 2022 || year == 2023) && !date.isEqual(endOfYear)) {
                date = date.plusDays(3);
            } else {
                date = date.plusDays(1);
            }
        }

        lineChart.getData().add(series);

        if (year == LocalDate.now().getYear()) {
            addCurrentBitcoinValue(series);
        }

        Platform.runLater(this::applySeriesStyles);
    }

    private void addCurrentBitcoinValue(XYChart.Series<String, Number> series) {
        LocalDate today = LocalDate.now();
        String todayFormatted = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        boolean currentDayAlreadyAdded = series.getData().stream()
                .anyMatch(data -> data.getXValue().equals(todayFormatted));

        if (!currentDayAlreadyAdded) {
            double currentPrice = getRealBitcoinValueForDate(today);
            if (currentPrice != 0.0) {
                String label = today.format(DateTimeFormatter.ofPattern("dd MMM"));
                XYChart.Data<String, Number> currentDayData = new XYChart.Data<>(label, currentPrice);
                Circle node = new Circle(5, Color.GOLD);
                node.setVisible(true);
                currentDayData.setNode(node);
                series.getData().add(currentDayData);
            }
        }
    }

    private Map<String, Double> fetchHistoricalBitcoinData(int year) {
        Map<String, Double> historicalData = new HashMap<>();

        String dataFile = "BTC_" + year + ".txt";
        LocalDate endDate = (year == LocalDate.now().getYear()) ? LocalDate.now().minusDays(1) : LocalDate.of(year, 1, 1).withDayOfYear(LocalDate.of(year, 1, 1).lengthOfYear());

        readDataFromFile(year, dataFile, historicalData, endDate);

        if (year == LocalDate.now().getYear()) {
            LocalDate today = LocalDate.now();
            double valueForToday = getRealBitcoinValueForDate(today);
            historicalData.put(today.toString(), valueForToday);
        }

        return historicalData;
    }

    private void readDataFromFile(int year, String filePath, Map<String, Double> historicalData, LocalDate endDate) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line != null) {
                String[] values = line.split(", ");
                LocalDate date = LocalDate.of(year, 1, 1);
                List<Double> valuesList = Arrays.stream(values)
                        .map(String::trim)
                        .map(s -> s.replaceAll("[^\\d.]", ""))
                        .map(Double::parseDouble)
                        .collect(Collectors.toList());

                Collections.reverse(valuesList);

                for (Double value : valuesList) {
                    if (date.isAfter(endDate)) {
                        break;
                    }
                    historicalData.put(date.toString(), value);
                    date = date.plusDays(1);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("The file was not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }
    }


    private double getRealBitcoinValueForDate(LocalDate date) {
        try {
            Map<String, Map<String, Double>> prices = client.getPrice("bitcoin", "usd");
            Double currentPrice = prices.get("bitcoin").get("usd");
            return currentPrice != null ? currentPrice : 0.0;
        } catch (Exception e) {
            System.out.println("Error fetching real-time Bitcoin value: " + e.getMessage());
            return 0.0;
        }
    }


    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void applySeriesStyles() {
        for (int i = 0; i < lineChart.getData().size(); i++) {
            XYChart.Series<String, Number> series = lineChart.getData().get(i);
            Color color = colors[i % colors.length];
            String rgb = toRGBCode(color);
            series.getNode().setStyle("-fx-stroke: " + rgb + "; -fx-stroke-width: 2px;");
        }
    }


    public void generateHistoricalDataForEthereum(int year) {
        lineChart.getData().clear();
        Map<String, Double> historicalData = fetchHistoricalEthereumData(year);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ethereum " + year);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = startOfYear.withDayOfYear(startOfYear.lengthOfYear());
        if (year == LocalDate.now().getYear()) {
            endOfYear = LocalDate.now();
        }

        LocalDate date = startOfYear;
        while (!date.isAfter(endOfYear)) {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Double value = historicalData.getOrDefault(formattedDate, null);
            if (value != null) {
                String label = date.format(DateTimeFormatter.ofPattern("dd MMM"));
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, value);
                Node node = new Circle(0);
                node.setVisible(false);
                dataPoint.setNode(node);
                series.getData().add(dataPoint);
            }
            if ((year == 2022 || year == 2023) && !date.isEqual(endOfYear)) {
                date = date.plusDays(3);
            } else {
                date = date.plusDays(1);
            }
        }

        lineChart.getData().add(series);
        if (year == LocalDate.now().getYear()) {
            addCurrentEthereumValue(series);
        }
        Platform.runLater(this::applySeriesStyles);
    }
    private Map<String, Double> fetchHistoricalEthereumData(int year) {
        Map<String, Double> historicalData = new HashMap<>();
        String dataFile = "ETH_" + year + ".txt";
        LocalDate endDate = (year == LocalDate.now().getYear()) ? LocalDate.now().minusDays(1) : LocalDate.of(year, 1, 1).withDayOfYear(LocalDate.of(year, 1, 1).lengthOfYear());
        readDataFromFile(year, dataFile, historicalData, endDate);

        if (year == LocalDate.now().getYear()) {
            LocalDate today = LocalDate.now();
            double valueForToday = getRealEthereumValueForDate(today);
            historicalData.put(today.toString(), valueForToday);
        }
        return historicalData;
    }


    private void addCurrentEthereumValue(XYChart.Series<String, Number> series) {
        LocalDate today = LocalDate.now();
        String todayFormatted = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        boolean currentDayAlreadyAdded = series.getData().stream()
                .anyMatch(data -> data.getXValue().equals(todayFormatted));

        if (!currentDayAlreadyAdded) {
            double currentPrice = getRealEthereumValueForDate(today);
            if (currentPrice != 0.0) {
                String label = today.format(DateTimeFormatter.ofPattern("dd MMM"));
                XYChart.Data<String, Number> currentDayData = new XYChart.Data<>(label, currentPrice);
                Circle node = new Circle(5, Color.GOLD);
                node.setVisible(true);
                currentDayData.setNode(node);
                series.getData().add(currentDayData);
            }
        }
    }

    private double getRealEthereumValueForDate(LocalDate date) {
        try {
            Map<String, Map<String, Double>> prices = client.getPrice("ethereum", "usd");
            Double currentPrice = prices.get("ethereum").get("usd");
            return currentPrice != null ? currentPrice : 0.0;
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération de la valeur réelle d'Ethereum: " + e.getMessage());
            return 0.0;
        }
    }



}
