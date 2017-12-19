package albert.stock.app;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        new EpsDataCollector().execute();;
        new IntrinsicValueAnalyzer().execute();;
    }
}
