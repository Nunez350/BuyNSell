package com.tradestation.webapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.*;

import com.ning.http.client.*;
import org.apache.commons.lang3.StringUtils;

//import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Observer;

/**
 * @author jjelinek@tradestation.com
 */
public class TradeStationWebApi {
    private final String APIKEY;
    private final String CALLBACK;
    private final String APISECRET;
    private final String BASEURL;
    static final AsyncHttpClient client =
            new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeoutInMs(-1).build());
    static final ObjectMapper mapper = new ObjectMapper();
    private com.tradestation.webapi.Token token;

    public TradeStationWebApi(String redirectUri) {
        APIKEY = "your api key";
        APISECRET = "your api secret";
        BASEURL = "https://sim.api.tradestation.com/v2/";
        CALLBACK = redirectUri;
    }

    public String getAuthorizationUrl() throws UnsupportedEncodingException {
        return BASEURL + String.format("authorize?client_id=%s&response_type=code&redirect_uri=%s", APIKEY,
                URLEncoder.encode(CALLBACK, "UTF-8"));
    }

    public ArrayList<com.tradestation.webapi.Quote> getQuotes(String[] symbols) throws IOException {
        // encode symbols (eg: replace " " with "%20")
        ArrayList<String> encodedSymbols = new ArrayList<String>();
        for (String symbol : symbols) {
            try {
                encodedSymbols.add(URLEncoder.encode(symbol, "UTF-8").replace("+", "%20"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        Request request = new RequestBuilder("GET")
                .setUrl(BASEURL + String.format("data/quote/%s", StringUtils.join(encodedSymbols, ",")))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", "Bearer " + this.token.getAccess_token())
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        ArrayList<com.tradestation.webapi.Quote> quotes = new ArrayList<com.tradestation.webapi.Quote>();

        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                quotes = mapper.readValue(json, new TypeReference<ArrayList<com.tradestation.webapi.Quote>>() {
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return quotes;
    }

    public Token getAccessToken() {
        return this.token;
    }

    public void setAccessToken(String authorizationCode) throws IOException {
        if (authorizationCode.isEmpty()) return;
        Request request = new RequestBuilder("POST")
                .setUrl(BASEURL + "security/authorize")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .setBody("grant_type=authorization_code&code=" + authorizationCode + "&client_id=" + APIKEY
                        + "&redirect_uri=" + CALLBACK + "&client_secret=" + APISECRET)
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        Token token = null;
        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                token = mapper.readValue(json, Token.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.token = token;
    }

    public void setAccessToken(Token token) throws IOException {
        if (token == null || token.getRefresh_token().isEmpty()) return;
        Request request = new RequestBuilder("POST")
                .setUrl(BASEURL + "security/authorize")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .setBody("grant_type=refresh_token&client_id=" + APIKEY + "&redirect_uri=" + CALLBACK
                        + "&client_secret=" + APISECRET + "&refresh_token=" + token.getRefresh_token())
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        Token newToken = null;
        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                newToken = mapper.readValue(json, Token.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.token.setAccess_token(newToken.getAccess_token());
        this.token.setToken_type(newToken.getToken_type());
        this.token.setUserid(newToken.getUserid());
        this.token.setExpires_in(newToken.getExpires_in());
    }

    public Thread getBarchartStream(Observer observer, String symbol, int interval, String intervalType, String startDate) {
        Request request = new RequestBuilder("GET")
                .setUrl(BASEURL + String.format("stream/barchart/%s/%s/%s/%s",
                        symbol, interval, intervalType, startDate))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", "Bearer " + this.token.getAccess_token())
                .build();

//        // create an event source
//        final StreamSource streamSource = new StreamSource<IntradayBar>(request, IntradayBar.class);

        // subscribe the observer to the event source
//        streamSource.addObserver(observer);

        // return the event thread
        return null;
//        return new Thread(streamSource);
    }

    public ArrayList<AccountInfo> getUserAccounts() throws IOException {
        Request request = new RequestBuilder("GET")
                .setUrl(BASEURL + String.format("users/%s/accounts", this.token.getUserid()))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", "Bearer " + this.token.getAccess_token())
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        ArrayList<AccountInfo> accounts = new ArrayList<AccountInfo>();

        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                accounts = mapper.readValue(json, new TypeReference<ArrayList<AccountInfo>>() {
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return accounts;
    }

    public ArrayList<OrderDetail> getOrders(String[] keys) throws IOException {
        Request request = new RequestBuilder("GET")
                .setUrl(BASEURL + String.format("accounts/%s/orders", StringUtils.join(keys, ",")))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", "Bearer " + this.token.getAccess_token())
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        ArrayList<OrderDetail> orders = new ArrayList<OrderDetail>();

        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                orders = mapper.readValue(json, new TypeReference<ArrayList<OrderDetail>>() {
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return orders;
    }

    public ArrayList<OrderResult> placeOrder(Order order) throws IOException {
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String orderjson = null;
        try {
            orderjson = objectWriter.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Request request = new RequestBuilder("POST")
                .setUrl(String.format("%sorders", BASEURL))
                .setBody(orderjson)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token.getAccess_token())
                .addHeader("Accept", "application/json")
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        ArrayList<OrderResult> result = null;
        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                try {
                    result = mapper.readValue(json, new TypeReference<ArrayList<OrderResult>>() {
                    });
                } catch (JsonMappingException e) {
                    // OrderResult Array failed, try single (most-likely an error)
                    result = new ArrayList<OrderResult>();
                    OrderResult res = mapper.readValue(json, new TypeReference<OrderResult>() {
                    });
                    result.add(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public ArrayList<Confirm> confirmOrder(Order order) throws IOException {
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String orderjson = null;
        try {
            orderjson = objectWriter.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Request request = new RequestBuilder("POST")
                .setUrl(String.format("%sorders/confirm", BASEURL))
                .setBody(orderjson)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token.getAccess_token())
                .addHeader("Accept", "application/json")
                .build();

        ListenableFuture<Response> response = null;
        response = client.executeRequest(request, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });

        ArrayList<Confirm> result = null;
        if (response != null) {
            try {
                String json = response.get().getResponseBody();
                try {
                    result = mapper.readValue(json, new TypeReference<ArrayList<Confirm>>() {
                    });
                } catch (JsonMappingException e) {
                    // Confirmation Array failed, try single (most-likely an error)
                    result = new ArrayList<Confirm>();
                    Confirm res = mapper.readValue(json, new TypeReference<Confirm>() {
                    });
                    result.add(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}