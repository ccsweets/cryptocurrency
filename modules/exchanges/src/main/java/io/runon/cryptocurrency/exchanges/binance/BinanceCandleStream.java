package io.runon.cryptocurrency.exchanges.binance;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.ExceptionUtil;
import com.seomse.crawling.core.http.HttpUrl;
import io.runon.cryptocurrency.trading.Cryptocurrency;
import io.runon.cryptocurrency.trading.DataStream;
import io.runon.cryptocurrency.trading.SymbolCurrency;

import io.runon.trading.technical.analysis.candle.TradeCandle;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;

/**
 * 실시간 캔들정보 얻기
 * https://github.com/binance/binance-spot-api-docs/blob/master/web-socket-streams.md#websocket-limits
 *
 * @author macle
 */
@Slf4j
public abstract class BinanceCandleStream<T extends Cryptocurrency> extends DataStream<T> {

    /**
     * @param streamId 아이디 (자유지정, 중복안됨)
     */
    public BinanceCandleStream(String streamId) {
        super(streamId);
    }

    @Override
    public SymbolCurrency getSymbolCurrency(String cryptocurrencyId) {
        return BinanceSymbolCurrency.getSymbolCurrency(cryptocurrencyId);
    }


    private String message = "{\"method\":\"SUBSCRIBE\",\"id\":1,\"params\":[\"btcusdt@kline_1d\"]}";

    /**
     * 기본값은 btc usdt 1d
     * {"method":"SUBSCRIBE","id":1,"params":["btcusdt@kline_1h"]}
     *         Gson gson = new Gson();
     *         JsonArray params = new JsonArray();
     *         params.add("btcusdt@kline_1d");
     *         JsonObject object = new JsonObject();
     *         object.addProperty("method", "SUBSCRIBE");
     *         object.addProperty("id", 1);
     *         object.add("params", params);
     *         System.out.println(gson.toJson(object));
     * @param message subscribe message
     */
    public void setMessage(String message) {
        this.message = message;
    }


    private String interval = "1d";

    /**
     * 설정하지 않으면 기본값 1d
     * //1m
     * //3m
     * //5m
     * //15m
     * //30m
     * //1h
     * //2h
     * //4h
     * //6h
     * //8h
     * //12h
     * //1d
     * //3d
     * //1w
     * //1M
     *
     * @param interval intervals 참조
     */
    public void setInterval(String interval) {
        this.interval = interval;
    }

    /**
     * message
     * @param symbols btc,eth... or BTC,ETH...
     * @param currencies usdt,usd,btc... or USDT,USD,BTC...
     */
    public void setMessage(String symbols, String currencies){
        String [] symbolArray = symbols.toLowerCase().split(",");
        String [] currencyArray = currencies.toLowerCase().split(",");

        JsonArray params = new JsonArray();
        for(String symbol : symbolArray){
            for(String currency : currencyArray){
                params.add(symbol + currency + "@kline_" + interval);
            }
        }
        JsonObject object = new JsonObject();
        object.addProperty("method", "SUBSCRIBE");
        object.addProperty("id", 1);
        object.add("params", params);
        Gson gson = new Gson();
        message = gson.toJson(object);
    }

    /**
     * example [{"symbol":"ETHBTC","price":"0.06529800"},{"symbol":"LTCBTC","price":"0.00287900"}]
     *
     * @return json array
     */
    public String getAllTicker() {
        return HttpUrl.get("https://api.binance.com/api/v1/ticker/allPrices");
    }

    private WebSocket webSocket = null;
    private OkHttpClient client = null;

    @Override
    public void connect() {
        if(webSocket != null){
            try{webSocket.close(1000, null);}catch (Exception e){log.error(ExceptionUtil.getStackTrace(e));}
            try{client.dispatcher().executorService().shutdown();}catch (Exception e){log.error(ExceptionUtil.getStackTrace(e));}
        }

        //noinspection NullableProblems
        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.debug("onOpen response:" + getStreamId());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject messageObj = new JSONObject(text);

                    if (messageObj.isNull("e")) {
                        log.debug(text);
                        return;
                    }

                    if(!messageObj.getString("e").equals("kline")){
                        log.debug(text);
                        return;
                    }

                    String id = messageObj.getString("s");

                    messageObj = messageObj.getJSONObject("k");

                    TradeCandle tradeCandle = new TradeCandle();
                    tradeCandle.setTradeRecord(false);
                    tradeCandle.setOpenTime(messageObj.getLong("t"));
                    tradeCandle.setCloseTime(messageObj.getLong("T") + 1);
                    tradeCandle.setOpen(messageObj.getBigDecimal("o"));
                    tradeCandle.setClose(messageObj.getBigDecimal("c"));
                    tradeCandle.setHigh(messageObj.getBigDecimal("h"));
                    tradeCandle.setLow(messageObj.getBigDecimal("l"));

                    tradeCandle.setTradeCount(messageObj.getInt("n"));
                    tradeCandle.setVolume(messageObj.getBigDecimal("v"));
                    tradeCandle.setTradingPrice(messageObj.getBigDecimal("q"));

                    tradeCandle.setBuyVolume(messageObj.getBigDecimal("V"));
                    tradeCandle.setBuyTradingPrice(messageObj.getBigDecimal("Q"));

                    tradeCandle.setSellVolume(tradeCandle.getVolume().subtract(tradeCandle.getBuyVolume()));
                    tradeCandle.setSellTradingPrice(tradeCandle.getTradingPrice().subtract(tradeCandle.getBuyTradingPrice()));

                    addCandle(id, tradeCandle);
                }catch(Exception e){
                    log.error(ExceptionUtil.getStackTrace(e));
                }
            }
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.debug("onClosing code:" + code +", reason:" + reason + ", " + getStreamId());
            }
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.error("onClosed code:" + code +", reason:" + reason  + ", " + getStreamId());
            }
        };

        client = new OkHttpClient();
        Request request = new Request.Builder().url("wss://stream.binance.com:9443/ws").build();
        webSocket = client.newWebSocket(request, webSocketListener);
        webSocket.send(message);
    }


    public static void main(String[] args) {


    }

}
