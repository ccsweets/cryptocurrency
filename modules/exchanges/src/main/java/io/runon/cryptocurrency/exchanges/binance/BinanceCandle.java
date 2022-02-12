package io.runon.cryptocurrency.exchanges.binance;

import com.seomse.commons.utils.FileUtil;
import com.seomse.commons.utils.time.Times;
import com.seomse.crawling.core.http.HttpUrl;
import io.runon.trading.technical.analysis.candle.TradeCandle;
import org.json.JSONArray;

/**
 * 바이낸스 캔들 데이터
 * @author macle
 */
public class BinanceCandle {

//      parameter
//      symbol	STRING	YES
//      interval	ENUM	YES
//      startTime	LONG	NO
//      endTime	LONG	NO
//      limit	INT	NO	Default 500; max 1000.

    //intervals
    //1m
    //3m
    //5m
    //15m
    //30m
    //1h
    //2h
    //4h
    //6h
    //8h
    //12h
    //1d
    //3d
    //1w
    //1M

//response
// [
//  [
//    1499040000000,      // Open time
//    "0.01634790",       // Open
//    "0.80000000",       // High
//    "0.01575800",       // Low
//    "0.01577100",       // Close
//    "148976.11427815",  // Volume
//    1499644799999,      // Close time
//    "2434.19055334",    // Quote asset volume
//    308,                // Number of trades
//    "1756.87402397",    // Taker buy base asset volume
//    "28.46694368",      // Taker buy quote asset volume
//    "17928899.62484339" // Ignore.
//  ]
//]

    public static final String CANDLE = "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s";
    public static final String FUTURES_CANDLE = "https://fapi.binance.com/fapi/v1/klines?symbol=%s&interval=%s";

    /**
     *
     * 캔들 데이터를 지정한 파일경로로 추출함 (csv)
     * 파일은 덮어쓰지 않고 기존파일에 붙여서 내용이 생성됨
     * 캔들시작시간(밀리초 유닉스타임)[0],종가[1],시가[2],고가[3],저가[4],직전가[5],거래량[6],거래대금[7],거래횟수[8],매수거래량[9],매수거래대금[10]
     * @param url 바이낸스 현물, 혹은 선물
     * @param outPath 필수 파일 생성경로
     * @param symbol 필수 BTCUSDT, ETHUSDT ...
     * @param interval 필수 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M
     * @param startTime null 가능 설정하지 않으면 최신값 (unix time)
     * @param endTime null 가능 설정하지 않으면 최신값 (unix time)
     * @param limit null 가능 default 500 max 1000
     */
    public static void csv(String url, String outPath, String symbol, String interval, Long startTime, Long endTime, Integer limit){

        StringBuilder sb = new StringBuilder();

        JSONArray array = new JSONArray(jsonArray(url, symbol, interval, startTime,endTime, limit));
        int length = array.length();

        for (int i = 0; i < length ; i++) {
            JSONArray data = array.getJSONArray(i);
            sb.append("\n").append(data.getLong(0))
                    .append(",").append(data.getString(4))
                    .append(",").append(data.getString(1))
                    .append(",").append(data.getString(2))
                    .append(",").append(data.getString(3))
                    .append(",").append(data.getString(1))
                    .append(",").append(data.getString(5))
                    .append(",").append(data.getString(7))
                    .append(",").append(data.getInt(8))
                    .append(",").append(data.getString(9))
                    .append(",").append(data.getString(10))
            ;
        }

        if(FileUtil.isFile(outPath)){
            FileUtil.fileOutput(sb.toString(),outPath, false);
        }else{
            FileUtil.fileOutput(sb.substring(1),outPath, true);
        }
    }

    /**
     *
     * @param url 바이낸스 현물, 혹은 선물
     * @param symbol 필수 BTCUSDT, ETHUSDT ...
     * @param interval 필수 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M
     * @param startTime null 가능 설정하지 않으면 최신값 (unix time)
     * @param endTime null 가능 설정하지 않으면 최신값 (unix time)
     * @param limit null 가능 default 500 max 1000
     * @return api text json array
     */
    public static String jsonArray(String url, String symbol, String interval, Long startTime, Long endTime, Integer limit){
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(url.formatted(symbol,interval));
        if(startTime != null){
            queryBuilder.append("&startTime=").append(startTime);
        }

        if(endTime != null){
            queryBuilder.append("&endTime=").append(endTime);
        }

        if(limit != null){
            queryBuilder.append("&limit=").append(limit);
        }


        return HttpUrl.get(queryBuilder.toString());
    }

    /**
     *
     * @param url 바이낸스 현물, 혹은 선물
     * @param symbol 필수 BTCUSDT, ETHUSDT ...
     * @param interval 필수 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M
     * @param startTime null 가능 설정하지 않으면 최신값 (unix time)
     * @param endTime null 가능 설정하지 않으면 최신값 (unix time)
     * @param limit null 가능 default 500 max 1000
      * @return candles
     */
    public static TradeCandle [] candles(String url, String symbol, String interval, Long startTime, Long endTime, Integer limit){

        JSONArray array = new JSONArray(jsonArray(url, symbol, interval, startTime,endTime, limit));
        TradeCandle [] candles = new TradeCandle[array.length()];

        long candleTime ;

        char timeUnit = interval.charAt(interval.length()-1);
        String timeNumber = interval.substring(0, interval.length()-1);
        if(timeUnit == 'm'){
            candleTime = Times.MINUTE_1 * Long.parseLong(timeNumber);
        }else if(timeUnit == 'h'){
            candleTime = Times.HOUR_1 * Long.parseLong(timeNumber);
        }else if(timeUnit == 'd'){
            candleTime = Times.DAY_1 * Long.parseLong(timeNumber);
        }else if(timeUnit == 'w'){
            candleTime = Times.WEEK_1 * Long.parseLong(timeNumber);
        }else if(timeUnit == 'M'){
            //1달부터는 지원하지 않음
            throw new IllegalArgumentException("interval error: " + interval);
        }else{
            throw new IllegalArgumentException("interval error: " + interval);
        }


        for (int i = 0; i < candles.length ; i++) {
            JSONArray data = array.getJSONArray(i);

            long opeTime = data.getLong(0);

            TradeCandle tradeCandle = new TradeCandle();
            tradeCandle.setOpenTime(opeTime);
            tradeCandle.setCloseTime(opeTime + candleTime);
            tradeCandle.setClose(data.getBigDecimal(4));
            tradeCandle.setOpen(data.getBigDecimal(1));
            tradeCandle.setHigh(data.getBigDecimal(2));
            tradeCandle.setLow(data.getBigDecimal(3));
            tradeCandle.setPrevious(data.getBigDecimal(1));
            tradeCandle.setVolume(data.getBigDecimal(5));
            tradeCandle.setTradingPrice(data.getBigDecimal(7));
            tradeCandle.setTradeCount(data.getInt(8));
            tradeCandle.setBuyVolume(data.getBigDecimal(9));
            tradeCandle.setBuyTradingPrice(data.getBigDecimal(10));

            tradeCandle.setSellVolume();
            tradeCandle.setSellTradingPrice();
            tradeCandle.setChange();

            candles[i] = tradeCandle;
        }
        return candles;
    }

    public static void main(String[] args) {

    }
}