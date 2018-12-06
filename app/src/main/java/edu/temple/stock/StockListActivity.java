package edu.temple.stock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.temple.stock.model.Symbol;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
//stacklist activity is the main activity
public class StockListActivity extends AppCompatActivity {
    private boolean myTwoPane;//use boolean to decide whether to use one pane or two pane mode
    private PortfolioWorkerThread workerThread;//use a background thread.get stock info at regular interval of time.
    SharedPreferences sharedpreferences;//use shared prefereces to store info
    private String PREF = "portfolios";
    private PortfolioFileObserver portfolioFileObserver;//member variable for file observer project
    private Timer timer;//time to repeat the thread, use to update stock info in certain time loop(60s my case)
    private static TextView empty;
    //use broadcast receiver to listen tot he event from file observer. So that each time file observer recieves update
    //in the file, it sends event to bradcast so that it can read and display to the updated stock info.
    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            boolean hasChanged = intent.getBooleanExtra("hasChanged", false);
            if(hasChanged){
                readUpdatedSymbols();
            }
        }
    };
    private SimpleItemRecyclerViewAdapter adapter;//adapter holds the list conent
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //if orientation is in landscape or tablet decive, it will load the two pane layout, otherwise stays to single pane mode
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_stock_list_land);
        }else {
            setContentView(R.layout.activity_stock_list);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        //set the floating button to add the symble.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();//it will prompts the confirmation
            }
        });
        sharedpreferences = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        //if veiw is present, then we are in two pain mode
        if (findViewById(R.id.item_detail_container) != null) {
            myTwoPane = true;
        }
        View recyclerView = findViewById(R.id.stock_list);
        assert recyclerView != null;
        List<Symbol> symbols = new ArrayList<>();
        adapter = new SimpleItemRecyclerViewAdapter(this, symbols, myTwoPane);
        setupRecyclerView((RecyclerView) recyclerView, adapter);
        timer = new Timer();//set the timer for thread repeat
        empty = (TextView) findViewById(R.id.empty);
        workerThread = new PortfolioWorkerThread(sharedpreferences);
        timer.scheduleAtFixedRate(workerThread, 1000, 60000);//set frequency of update to 60 seconds
        portfolioFileObserver = new PortfolioFileObserver(PortfolioFileHandler.getPortfolioPath(this), this);//set the observer to observe any change
        portfolioFileObserver.startWatching();
        empty.setVisibility(View.VISIBLE);//empty view will be shown if the current stock list is empty
        //registering broadcast listener
        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("file_change_listener"));
    }
    @Override
    protected void onResume() {//set onResume status
        super.onResume();
        if(portfolioFileObserver != null){
            portfolioFileObserver.startWatching();
        }
        readUpdatedSymbols();
    }
    @Override
    protected void onDestroy() {//set on Destroy status, stop watching file observer and unregister the broadcast
        super.onDestroy();
        portfolioFileObserver.stopWatching();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
    }
    @Override
    protected void onPause() {//onPause will just stop wathching the file observer
        super.onPause();
        portfolioFileObserver.stopWatching();
    }
    //the disalog ask user to enter the symble (stock ticker, like GOOG for goole)
    private void showInputDialog(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(StockListActivity.this);
        alertDialog.setTitle(R.string.dialog_title);
        alertDialog.setMessage(R.string.dialog_message);
        final EditText input = new EditText(StockListActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        //use onclick listener to listen to the add button, and add stock ticker tot he list
        alertDialog.setPositiveButton(R.string.add,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String symbol = input.getText().toString();
                        if(symbol == null || symbol.trim().isEmpty()){
                            Toast.makeText(StockListActivity.this, R.string.hint_message, Toast.LENGTH_SHORT);
                        }else {
                            //if we can find the match of the stock ticker (it is valid), then we can grab details about it using
                            //background task (StockDataLoader)
                            //To achieve this, we are using AsyncTask to avoid memory leak and make it not run on UI thread
                            addSymbol(symbol);
                        }
                    }
                });//then set onclick listener for clicking cancel button
        alertDialog.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }
    //execute Asynctask to fetch detail of stock ticker
    //Using an interface as an argument to the loader, the interface method will be called by loader when it finishes
    //fetching information of stock ticker
    private void addSymbol(final String symbol) {
        StockDataLoader taskRunner = new StockDataLoader(new SymbolResponseHandler() {
            @Override
            public void onResponseReceived(String response) {
                validateSymbol();
            }
        }, sharedpreferences );
        taskRunner.execute(symbol);
    }
    private void showSymbolNotFound(){//if symbol (stock ticker) not found, than it is invalid and user should type again)
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(StockListActivity.this, R.string.invalid_symbol, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void setupRecyclerView(@NonNull RecyclerView recyclerView, SimpleItemRecyclerViewAdapter adapter) {
        recyclerView.setAdapter(adapter);
    }
    //set up a recycler view list. It is a better version of list than the list view which can easily modified
    public static class SimpleItemRecyclerViewAdapter extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {
        private final StockListActivity myParentActivity;
        private final List<Symbol> myValues;
        private final boolean myTwoPane;
        private final View.OnClickListener myOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Symbol item = (Symbol) view.getTag();
                if (myTwoPane) {//if we are in 2 pane mode
                    Bundle arguments = new Bundle();
                    arguments.putString(StockDetailFragment.ARG_ITEM_ID, item.getSymbol());
                    arguments.putString(StockDetailFragment.ARG_COMPANY_NAME, item.getName());
                    arguments.putDouble(StockDetailFragment.ARG_CURR_STOCK_PRICE, item.getLastPrice());
                    arguments.putDouble(StockDetailFragment.ARG_OPENING_STOCK_PRICE, item.getOpen());
                    StockDetailFragment fragment = new StockDetailFragment();//use fragment display the stock details after click the certain stock ticker
                    fragment.setArguments(arguments);
                    myParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();
                } else {//if single pane, then click the certain stock ticker will trigger the stockdetail activity using the intent
                    Context context = view.getContext();
                    Intent intent = new Intent(context, StockDetailActivity.class);
                    intent.putExtra(StockDetailFragment.ARG_ITEM_ID, item.getSymbol());
                    intent.putExtra(StockDetailFragment.ARG_COMPANY_NAME, item.getName());
                    intent.putExtra(StockDetailFragment.ARG_CURR_STOCK_PRICE, item.getLastPrice());
                    intent.putExtra(StockDetailFragment.ARG_OPENING_STOCK_PRICE, item.getOpen());
                    context.startActivity(intent);
                }
            }
        };
        SimpleItemRecyclerViewAdapter(StockListActivity parent, List<Symbol> items, boolean twoPane) {//set the variables such as whether recycler view is in 2 pane mode
            myValues = items;
            myParentActivity = parent;
            myTwoPane = twoPane;
        }
        public void clear(){//method to clear all the data in myValues, and will notify data has been changed
            myValues.clear();
            notifyDataSetChanged();
        }
        public void addSymbol(Symbol symbol, boolean update){//method will add data from the certain stock ticker
            myValues.add(symbol);
            if(update) {
                notifyDataSetChanged();
            }
        }
        public void update(){
            notifyDataSetChanged();
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stock_list_content, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {//display data at specific position of stock list
            holder.myIdView.setText(myValues.get(position).getSymbol());
            holder.myContentView.setText(""+myValues.get(position).getLastPrice());
            if(myValues.get(position).getLastPrice() < myValues.get(position).getOpen()){//decide whether certain stock is increasing or decreasing compare to open price
                //we can compare the value of open price and current price
                holder.itemView.setBackgroundColor( Color.RED);
            }else {
                holder.itemView.setBackgroundColor(Color.GREEN);
            }
            holder.itemView.setTag(myValues.get(position));
            holder.itemView.setOnClickListener(myOnClickListener);
        }
        @Override
        public int getItemCount() {//get items in the data set held by adapter
            if(myValues.size() == 0){
                empty.setVisibility(View.VISIBLE);
            }else {
                empty.setVisibility(View.GONE);
            }
            return myValues.size();
        }
        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView myIdView;
            final TextView myContentView;
            ViewHolder(View view) {
                super(view);
                myIdView = (TextView) view.findViewById(R.id.id_text);
                myContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }
    //create a worker thread which can keep getting details of all saved stocks in background
    //thread will work in background and regularly fetches stock info
    private class PortfolioWorkerThread extends TimerTask {
        private SharedPreferences sharedPreferences;
        private PortfolioWorkerThread(SharedPreferences sharedPreferences){
            this.sharedPreferences = sharedPreferences;
        }
        @Override
        public void run() {
            try {
                fetchSymbolInformation(this.sharedPreferences);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    //read the flag from the shared preference to know if last loaded stock ticker is valid or not
    //if valid, then read updataed info.
    private void validateSymbol(){
        if(sharedpreferences != null){
            boolean hasChanged = sharedpreferences.getBoolean("changed", false);
            boolean isValid = sharedpreferences.getBoolean("valid_symbol", false);
            if(hasChanged){
                if(isValid) {
                    readUpdatedSymbols();
                }else {
                    showSymbolNotFound();
                }
                SharedPreferences.Editor edit = sharedpreferences.edit();
                edit.putBoolean("changed", false);
                edit.commit();
            }
        }
    }
    //reads all saved stock and one by one fetches details about the stock from the API.
    private void fetchSymbolInformation(SharedPreferences sharedPreferences){
        Set<String> symbols = sharedPreferences.getStringSet("symbols", null);
        final List<String> lstData = new ArrayList<>();
        if (symbols != null) {
            final Gson gson = new Gson();//use GSON method to convert Java objects into JSON and back
            String[] symArray = new String[symbols.size()];
            symbols.toArray(symArray);
            for (String symbol : symArray) {
                Log.d("Saved Symbol: ", symbol);
                String response = getPortfolioInformation(symbol);
                if (response != null) {
                    try {
                        Symbol obj = gson.fromJson(response, Symbol.class);
                        if (obj != null) {
                            lstData.add(response);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (lstData != null && lstData.size() > 0) {
                writeSymbols(lstData);
            }
        }
    }
    //save stock info in the portfolio file, use getPortfolioInformation method to read stock info from API to buffer then write to portfolio file
    private void writeSymbols(List<String> symbols) {
        // get the application context
        Context context = getApplicationContext();
        PortfolioFileHandler symbolHandler = new PortfolioFileHandler();
        symbolHandler.writeSymbols(symbols, context);//write info to the file
    }
    //read stock detail info from the internal storaged file. Then converts JSON string to java java class using GSON
    //So the list of all stock tickers will be passed to the list adapter. So that it will display the updated info
    private void readUpdatedSymbols(){
        Context context = getApplicationContext();
        PortfolioFileHandler symbolReader = new PortfolioFileHandler();
        List<String> symbols = symbolReader.readSymbols(context);
        adapter.clear();
        Gson gson = new Gson();
        for(String symbol: symbols){
            try {
                Symbol symbol1 = gson.fromJson(symbol, Symbol.class);//use fromJson() method to convert JSON elements to Java objects
                if(symbol1 != null && symbol1.getSymbol() != null && !symbol1.getSymbol().trim().isEmpty()) {
                    adapter.addSymbol(symbol1, true);
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
    //make the API function and get the result.Basically will create a read buffer function read info from the url of API
    private String getPortfolioInformation(String symbol){
        String REQUEST_METHOD = "GET";
        int TIMEOUT = 15000;
        String BASE_URL = "http://dev.markitondemand.com/MODApis/Api/v2/Quote/json/?symbol="+symbol;
        String result = null;//set result from empty stutus then fill it up
        try {
            URL myUrl = new URL(BASE_URL);
            HttpURLConnection connection =(HttpURLConnection) myUrl.openConnection();
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(TIMEOUT);
            connection.setConnectTimeout(TIMEOUT);
            connection.connect();
            InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String inputLine = null;
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
}
