package albert.stock.app;

import java.io.IOException;
import java.util.List;

import albert.stock.app.EpsDataCollector.EpsHistory;

/**
 * 程式執行進入點
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        List<EpsHistory> data = new EpsDataCollector().execute();
        if (data == null || data.size() == 0) {
            throw new RuntimeException("沒有可供分析的資料");
        } else {
            new IntrinsicValueAnalyzer().execute(data);
        }
    }
}
