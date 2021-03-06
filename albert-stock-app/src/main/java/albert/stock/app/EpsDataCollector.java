package albert.stock.app;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Strings;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpsDataCollector {

    public static void main(String[] args) {
        new EpsDataCollector().execute();
    }

    public List<EpsHistory> execute() {
        log.info("fetching data...");
        List<Security> securities = readSecurities();
        List<EpsHistory> historyData = new ArrayList<>();
        for (Security security : securities) {
            collectData(historyData, security);
        }
        for (EpsHistory history : historyData) {
            log.debug(history.toString());
        }
        log.info("fetched EPS data...");
        return historyData;
    }

    private void collectData(List<EpsHistory> historyData, Security security) {
        EpsHistory history = EpsHistory.builder().symbol(security.getSymbol() + "-" + security.getName()).build();
        List<EPS> epsData = new ArrayList<>();

        String basicInfoUrl = "https://goodinfo.tw/StockInfo/StockDetail.asp?STOCK_ID=" + security.getSymbol();
        try {
            Document basicInfo = Jsoup.connect(basicInfoUrl).get();

            String currentPirce = basicInfo.select(
                    "body > table:nth-child(5) > tbody > tr > td:nth-child(3) > table > tbody > tr:nth-child(1) > td > table > tbody > tr:nth-child(1) > td:nth-child(1) > table > tbody > tr:nth-child(3) > td:nth-child(1)")
                    .text();
            history.setCurrentPrice((Strings.isNullOrEmpty(currentPirce)) ? 0d : Double.valueOf(currentPirce));

            history.setDividen(getDividen(security.getSymbol()));

            Element financeIncomElement = basicInfo.getElementById("FINANCE_INCOME");
            if (financeIncomElement != null) {
                Elements trElements = financeIncomElement.select("tr");
                for (int i = 0; i < trElements.size(); i++) {
                    if (i >= 4) {
                        String year = trElements.get(i).children().get(0).text();
                        String eps = trElements.get(i).children().get(7).text();
                        EPS data = EPS.builder().year(year).value(eps).build();
                        epsData.add(data);
                    }
                }
                history.setHistories(epsData);

                historyData.add(history);
            } else {
                String errorMs = "資料取得發生錯誤，或您指定的股票代號 {0} 不存在 ";
                log.error(new MessageFormat(errorMs).format(new String[] { security.getSymbol() }));
            }
        } catch (IOException e) {
            throw new RuntimeException("無法取得資料, 錯誤原因: " + e.getMessage(), e);
        }

    }

    private Double getDividen(String symbol) {
        String url = "https://goodinfo.tw/StockInfo/StockDividendPolicy.asp?STOCK_ID=" + symbol;
        String dividen = "";
        try {
            Document doc = Jsoup.connect(url).get();
            dividen = doc.select("#divDetail > table > tbody:nth-child(2) > tr:nth-child(1) > td:nth-child(8)").text();
        } catch (IOException e) {
            throw new RuntimeException("無法取得資料, 錯誤原因: " + e.getMessage(), e);
        }

        if (Strings.isNullOrEmpty(dividen) || "-".equals(dividen)) {
            return 0d;
        } else {
            return Double.valueOf(dividen);
        }
    }

    private List<Security> readSecurities() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        List<Security> securities = new ArrayList<>();
        try {
            List<String> data = IOUtils.readLines(classLoader.getResourceAsStream("symbols.txt"));
            if (data != null && data.size() > 0) {
                for (String item : data) {
                    String itemArr[] = item.split(",");
                    Security security = Security.builder().symbol(itemArr[0]).name(itemArr[1]).build();
                    securities.add(security);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("找不到 symbols.txt", e);
        }
        return securities;
    }

    @Data
    @Builder
    public static class Security {
        private String symbol;
        private String name;
    }

    @Data
    @Builder
    public static class EpsHistory {
        private String symbol;
        private Double currentPrice;
        private Double dividen;
        private List<EPS> histories;
    }

    @Data
    @Builder
    public static class EPS {
        private String year;
        private String value;
    }
}
