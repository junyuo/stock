package albert.stock.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import albert.stock.app.EpsDataCollector.EPS;
import albert.stock.app.EpsDataCollector.EpsHistory;
import lombok.Builder;
import lombok.Cleanup;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntrinsicValueAnalyzer {

    private static String dir = "tmp";
    private static DecimalFormat numberFormat = new DecimalFormat("0.00");
    private static File summaryXlsFile = new File(dir + File.separator + "summary.xls");

    public void execute(List<EpsHistory> data) throws IOException {
        log.info("Analyzing data...");
        List<Stock> stocks = prepareAnalysisData(data);
        computeGrowth(stocks);
        computeGrowthRate(stocks);
        compuateAvgGrowthRate(stocks);
        computeIntrinsicValue(stocks);
        writeAnalysisResult(stocks);

        log.info("analyze successful!");
        log.info("Please check " + dir + File.separator + "summary.xls");
    }

    private void writeAnalysisResult(List<Stock> stocks) throws IOException {
        @Cleanup
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("stock list");
        CellStyle style = createCellStyle(workbook);

        int rowCount = 0;
        Row headerRow = sheet.createRow(rowCount);
        writeHeader(headerRow, style);
        for (Stock stock : stocks) {
            Row row = sheet.createRow(++rowCount);
            writeDataForEachRow(stock, row, style);
        }

        @Cleanup
        FileOutputStream outputStream = new FileOutputStream(summaryXlsFile);
        workbook.write(outputStream);
    }

    private CellStyle createCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setWrapText(true);

        return cellStyle;
    }

    private void writeHeader(Row headerRow, CellStyle style) {
        Cell cell = headerRow.createCell(0);
        cell.setCellValue("股票代號");
        cell.setCellStyle(style);

        cell = headerRow.createCell(1);
        cell.setCellValue("名稱");
        cell.setCellStyle(style);

        cell = headerRow.createCell(2);
        cell.setCellValue("真實價值");
        cell.setCellStyle(style);

        cell = headerRow.createCell(3);
        cell.setCellValue("股價");
        cell.setCellStyle(style);
    }

    private void writeDataForEachRow(Stock stock, Row row, CellStyle style) {
        Cell cell = row.createCell(0);
        cell.setCellValue(stock.getSymbol());
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue(stock.getName());
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue(Double.valueOf(numberFormat.format(stock.getIntrinsicValue())));
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue(stock.getCurrentPrice());
        cell.setCellStyle(style);
    }

    private void computeIntrinsicValue(List<Stock> stocks) {
        for (Stock stock : stocks) {
            Double latestEps = stock.getHistories().get(0).getEps();
            stock.setIntrinsicValue(latestEps * (8.5 + 2 * stock.getAvgGrowthRate()));
        }
    }

    private void compuateAvgGrowthRate(List<Stock> stocks) {
        for (Stock stock : stocks) {
            List<History> histories = stock.getHistories();
            Double sum = 0d;
            for (int i = 0; i < histories.size() - 1; i++) {
                sum += histories.get(i).getGrowthRate();
            }
            stock.setAvgGrowthRate(sum / (histories.size() - 1));
        }
    }

    private void computeGrowthRate(List<Stock> stocks) {
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

    private void computeGrowth(List<Stock> stocks) {
        for (Stock stock : stocks) {
            List<History> histories = stock.getHistories();
            List<History> result = new ArrayList<>();
            for (int i = 0; i < histories.size(); i++) {
                if (i < histories.size() - 1) {
                    History current = histories.get(i);
                    History previous = histories.get(i + 1);
                    Double growth = current.getEps() - previous.getEps();
                    current.setGrowth(growth);
                    result.add(current);
                } else {
                    result.add(histories.get(i));
                }
            }
            stock.setHistories(result);
        }
    }

    private List<Stock> prepareAnalysisData(List<EpsHistory> data) throws IOException {
        List<Stock> stocks = new ArrayList<>();

        for (EpsHistory row : data) {
            String symbolArr[] = row.getSymbol().split("-");
            Stock stock = Stock.builder().symbol(symbolArr[0]).name(symbolArr[1]).currentPrice(row.getCurrentPrice())
                    .build();

            List<History> histories = new ArrayList<>();
            for (EPS eps : row.getHistories()) {
                History history = History.builder().year(eps.getYear()).eps(Double.valueOf(eps.getValue())).build();
                histories.add(history);
            }
            stock.setHistories(histories);
            stocks.add(stock);
        }

        return stocks;
    }

    @Data
    @ToString
    @Builder
    private static class Stock {
        private String symbol;
        private String name;
        private Double currentPrice;
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
