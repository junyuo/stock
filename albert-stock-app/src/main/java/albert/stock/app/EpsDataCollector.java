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

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpsDataCollector {

    public List<EpsHistory> execute() {
        log.info("fetching data...");
        List<String> symbols = readSymbols();
        List<EpsHistory> historyData = new ArrayList<>();
        for (String symbol : symbols) {
            collectData(historyData, symbol);
        }
        for (EpsHistory history : historyData) {
            log.debug(history.toString());
        }
        log.info("fetched EPS data...");
        return historyData;
    }

    private void collectData(List<EpsHistory> historyData, String symbol) {
        EpsHistory history = EpsHistory.builder().symbol(symbol).build();
        List<EPS> epsData = new ArrayList<>();

        String basicInfoUrl = "https://goodinfo.tw/StockInfo/StockDetail.asp?STOCK_ID=" + symbol;
        try {
            Document basicInfo = Jsoup.connect(basicInfoUrl).get();
            String name = basicInfo.select(
                    "body > table:nth-child(3) > tbody > tr > td:nth-child(3) > table > tbody > tr:nth-child(1) > td > table:nth-child(1) > tbody > tr > td > table > tbody > tr:nth-child(1) > td > span:nth-child(1) > a")
                    .text();
            history.setSymbol(CharMatcher.anyOf(" ").replaceFrom(name, "-"));

            String currentPirce = basicInfo.select(
                    "body > table:nth-child(3) > tbody > tr > td:nth-child(3) > table > tbody > tr:nth-child(1) > td > table:nth-child(1) > tbody > tr > td > table > tbody > tr:nth-child(3) > td:nth-child(1)")
                    .text();
            history.setCurrentPrice((Strings.isNullOrEmpty(currentPirce)) ? 0d : Double.valueOf(currentPirce));

            history.setCurrentPrice(getDividen(symbol));

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
                log.error(new MessageFormat("您指定的股票代號 {0} 不存在 ").format(new String[] { symbol }));
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

        if (Strings.isNullOrEmpty(dividen)) {
            return 0d;
        } else {
            return Double.valueOf(dividen);
        }
    }

    private List<String> readSymbols() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        List<String> symbols = new ArrayList<>();
        try {
            symbols = IOUtils.readLines(classLoader.getResourceAsStream("symbols.txt"));
        } catch (IOException e) {
            throw new RuntimeException("找不到 symbols.txt", e);
        }
        return symbols;
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
