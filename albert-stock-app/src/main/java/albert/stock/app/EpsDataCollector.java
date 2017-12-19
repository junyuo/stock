package albert.stock.app;

import java.io.File;
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
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpsDataCollector {
    private static String dir = "tmp\\";

    public void execute() throws IOException {
        List<String> symbols = readSymbols();
        List<EpsHistory> historyData = new ArrayList<>();
        for (String symbol : symbols) {
            collectData(historyData, symbol);
        }

        for (EpsHistory history : historyData) {
            CharSink charsink = Files.asCharSink(new File(dir + history.getSymbol() + ".csv"), Charsets.UTF_8);
            List<String> lines = new ArrayList<>();
            lines.add("year,eps");
            for (EPS eps : history.getHistories()) {
                lines.add(eps.getYear() + "," + eps.getValue());
            }
            charsink.writeLines(lines);
        }
        if (historyData.size() > 0) {
            List<String> successList = new ArrayList<>();
            historyData.forEach(data -> successList.add(data.getSymbol()));
            log.debug("collector data successfully, symbol list = " + Joiner.on(", ").join(successList));
        }
    }

    private void collectData(List<EpsHistory> historyData, String symbol) throws IOException {
        EpsHistory history = EpsHistory.builder().symbol(symbol).build();
        List<EPS> epsData = new ArrayList<>();

        String url = "https://goodinfo.tw/StockInfo/StockDetail.asp?STOCK_ID=" + symbol;
        Document doc = Jsoup.connect(url).get();
        String name = doc.select(
                "body > table:nth-child(3) > tbody > tr > td:nth-child(3) > table > tbody > tr:nth-child(1) > td > table:nth-child(1) > tbody > tr > td > table > tbody > tr:nth-child(1) > td > span:nth-child(1) > a")
                .text();
        history.setSymbol(CharMatcher.anyOf(" ").replaceFrom(name, "-"));

        Element financeIncomElement = doc.getElementById("FINANCE_INCOME");
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
    }

    private List<String> readSymbols() throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        List<String> symbols = IOUtils.readLines(classLoader.getResourceAsStream("symbols.txt"));
        return symbols;
    }

    @Data
    @Builder
    private static class EpsHistory {
        private String symbol;
        private List<EPS> histories;
    }

    @Data
    @Builder
    private static class EPS {
        private String year;
        private String value;
    }
}
