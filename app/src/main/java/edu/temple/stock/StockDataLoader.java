package edu.temple.stock;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import edu.temple.stock.model.Symbol;
//workds in background thread and does not block UI thread. So app can download the fetches info from the network
//asychronously
public class StockDataLoader extends AsyncTask<String, String, String> {
    private static final String REQUEST_METHOD = "GET";
    public static final int TIMEOUT = 15000;
    private static final String BASE_URL = "http://dev.markitondemand.com/MODApis/Api/v2/Quote/json/?symbol=";
    private SharedPreferences sharedPreferences;
    private SymbolResponseHandler responsehandler;
    public StockDataLoader(SymbolResponseHandler handler, SharedPreferences preferences){
        this.responsehandler = handler;
        this.sharedPreferences = preferences;
    }
    @Override
    protected String doInBackground(String... params) {
        String stringUrl = BASE_URL + params[0];
        String result;
        String inputLine;
        try {
            URL myUrl = new URL(stringUrl);
            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(TIMEOUT);
            connection.setConnectTimeout(TIMEOUT);
            connection.connect();
            InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }
            reader.close();
            streamReader.close();
            result = stringBuilder.toString();
        }
        catch(IOException e){
            e.printStackTrace();
            result = null;
        }
        return result;
    }
    @Override
    protected void onPostExecute(String result) {
        if(result != null){//check if result is not null
            SharedPreferences.Editor edit = this.sharedPreferences.edit();
            Gson gson = new Gson();
            try {
                Symbol obj = gson.fromJson(result, Symbol.class);//convert to symbol class using GSON method
                if(obj != null && obj.getSymbol() != null && !obj.getSymbol().trim().isEmpty()){
                    Set<String> symbols = this.sharedPreferences.getStringSet("symbols", null);
                    if(symbols == null){
                        symbols = new HashSet<>();
                    }
                    symbols.add(obj.getSymbol());//save added stock ticker (symbol)
                    edit.clear();
                    edit.putStringSet("symbols", symbols);
                    edit.commit();
                    edit.putBoolean("changed", true);
                    edit.commit();
                    edit.putBoolean("valid_symbol", true);//save validity info of the stock ticker (symbol)
                    edit.commit();
                }else {
                    edit.putBoolean("changed", true);
                    edit.commit();
                    edit.putBoolean("valid_symbol", false);
                    edit.commit();
                }
            }catch (Exception ex){
                edit.putBoolean("changed", true);
                edit.commit();
                edit.putBoolean("valid_symbol", false);
                edit.commit();
            }
        }
        if(responsehandler != null){
            responsehandler.onResponseReceived(result);
        }
    }
    @Override
    protected void onPreExecute() {
    }
    @Override
    protected void onProgressUpdate(String... text) {
    }
}
