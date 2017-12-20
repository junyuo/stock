package albert.stock.app;

import java.io.IOException;
import java.util.List;

import albert.stock.app.EpsDataCollector.EpsHistory;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        List<EpsHistory> data = new EpsDataCollector().execute();
        new IntrinsicValueAnalyzer().execute(data);
    }
}
