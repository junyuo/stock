# stock
程式執行點 [APP](https://github.com/junyuo/stock/blob/master/albert-stock-app/src/main/java/albert/stock/app/App.java)。
根據[如何計算 Intrinsic Value (股票內在價值)](https://albert-kuo.blogspot.tw/2015/03/intrinsic-value.html)此篇文章計算股票的真實價值: 
* 將 csv 檔案放在 tmp 目錄下 (命名規則為<股票代號>-<股票名稱>.csv，如1210-大成.csv))，[csv 格式](https://github.com/junyuo/stock/blob/master/albert-stock-app/tmp/1210-%E5%A4%A7%E6%88%90.csv)。
* 跑完程式，會將分析結果放在 tmp\summary.csv，如 https://github.com/junyuo/stock/blob/master/albert-stock-app/tmp/summary.csv