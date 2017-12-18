package albert.stock.app;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntrinsicValueAnalyzer {

    private static String dir = "tmp";
    private static DecimalFormat numberFormat = new DecimalFormat("0.00");
    private static File summaryFile = new File(dir + "\\summary.csv");

    public static void main(String[] args) throws IOException {
        List<Stock> stocks = readCSV();
        computeGrowth(stocks);
        computeGrowthRate(stocks);
        compuateAvgGrowthRate(stocks);
        computeIntrinsicValue(stocks);
        writeAnalysisResult(stocks);

        log.info("analyze successful!");
        log.info("Please check " + dir + "\\summary.csv");
    }

    private static void writeAnalysisResult(List<Stock> stocks) throws IOException {
        byte bom[] = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        String header = new String(bom) + "symbol,name,intrinsic value";
        List<String> csvString = new ArrayList<>();
        csvString.add(header);
        for (Stock stock : stocks) {
            String symbol = stock.getSymbol();
            String name = stock.getName();
            String intrinsicValue = numberFormat.format(stock.getIntrinsicValue());
            String row = symbol + "," + name + "," + intrinsicValue;
            csvString.add(row);
        }
        CharSink charsink = Files.asCharSink(summaryFile, Charsets.UTF_8);
        charsink.writeLines(csvString);
    }

    private static void computeIntrinsicValue(List<Stock> stocks) {
        for (Stock stock : stocks) {
            Double latesEps = stock.getHistories().get(0).getEps();
            stock.setIntrinsicValue(latesEps * (8.5 + 2 * stock.getAvgGrowthRate()));
        }
    }

    private static void compuateAvgGrowthRate(List<Stock> stocks) {
        for (Stock stock : stocks) {
            List<History> histories = stock.getHistories();
            Double sum = 0d;
            for (int i = 0; i < histories.size() - 1; i++) {
                sum += histories.get(i).getGrowthRate();
            }
            stock.setAvgGrowthRate(sum / (histories.size() - 1));
        }
    }

    private static void computeGrowthRate(List<Stock> stocks) {
        for (Stock stock : stocks) {
            List<History> histories = stock.getHistories();
            List<History> result = new ArrayList<>();
            for (int i = 0; i < histories.size(); i++) {
                if (i < histories.size() - 1) {
                    History current = histories.get(i);
                    History last = histories.get(i + 1);
                    Double growthRate = current.getGrowth() / last.getEps();
                    current.setGrowthRate(growthRate);
                    result.add(current);
                } else {
                    result.add(histories.get(i));
                }
            }
            stock.setHistories(result);
        }
    }

    private static void computeGrowth(List<Stock> stocks) {
        for (Stock stock : stocks) {
            List<History> histories = stock.getHistories();
            List<History> result = new ArrayList<>();
            for (int i = 0; i < histories.size(); i++) {
                if (i < histories.size() - 1) {
                    History current = histories.get(i);
                    History last = histories.get(i + 1);
                    Double growth = current.getEps() - last.getEps();
                    current.setGrowth(growth);
                    result.add(current);
                } else {
                    result.add(histories.get(i));
                }

            }
            stock.setHistories(result);
        }
    }

    private static void printData(List<Stock> stocks) {
        for (Stock stock : stocks) {
            log.debug("symbol = " + stock.getSymbol() + ", name = " + stock.getName() + ", intrinsic value = "
                    + stock.getIntrinsicValue());
            List<History> histories = stock.getHistories();
            for (History history : histories) {
                log.debug(history.toString());
            }
            log.debug("-------------------------------------------------------------------------");
        }
    }

    private static List<Stock> readCSV() throws IOException {
        List<Stock> stocks = new ArrayList<>();

        Collection<File> files = FileUtils.listFiles(new File(dir), new String[] { "csv" }, false);
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.contains("-")) {
                String fileNameArr[] = fileName.substring(0, fileName.lastIndexOf(".")).split("-");
                Stock stock = Stock.builder().symbol(fileNameArr[0]).name(fileNameArr[1]).build();
                List<History> histories = new ArrayList<>();

                List<String> lines = FileUtils.readLines(new File(dir + "\\" + fileName));
                for (int i = 0; i < lines.size(); i++) {
                    if (i > 0) {
                        String[] lineArr = lines.get(i).split(",");
                        String year = lineArr[0];
                        Double eps = Double.parseDouble(lineArr[1]);
                        History history = History.builder().year(year).eps(eps).build();
                        histories.add(history);
                    }
                }
                stock.setHistories(histories);
                stocks.add(stock);
            }
        }
        return stocks;
    }

    @Data
    @ToString
    @Builder
    private static class Stock {
        private String symbol;
        private String name;
        private Double avgGrowthRate;
        private Double intrinsicValue;
        private List<History> histories;
    }

    @Data
    @ToString
    @Builder
    private static class History {
        private String year;
        private Double eps;
        private Double growth;
        private Double growthRate;
    }

}