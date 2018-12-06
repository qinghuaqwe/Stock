package edu.temple.stock;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import edu.temple.stock.model.Symbol;

public class StockDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "symbol";
    public static final String ARG_COMPANY_NAME = "company";
    public static final String ARG_CURR_STOCK_PRICE = "ARG_CURR_STOCK_PRICE";
    public static final String ARG_OPENING_STOCK_PRICE = "ARG_OPENING_STOCK_PRICE";
    private String mySymbol;
    private String myCompany;
    private double myCurrPrice;
    private double myOpenPrice;
    public StockDetailFragment() {
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.two_Pane);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mySymbol  = getArguments().getString(ARG_ITEM_ID);
            myCompany = getArguments().getString(ARG_COMPANY_NAME);
            myCurrPrice = getArguments().getDouble(ARG_CURR_STOCK_PRICE);
            myOpenPrice = getArguments().getDouble(ARG_OPENING_STOCK_PRICE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.stock_detail, container, false);
        if (myCompany != null) {
            ((TextView) rootView.findViewById(R.id.company_name)).setText(myCompany);
        }
        WebView webView = (WebView) rootView.findViewById(R.id.web_view);
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        if(mySymbol != null) {
            webView.loadUrl(getChartURL(mySymbol));
        }
        ((TextView) rootView.findViewById(R.id.curr_price)).setText(""+myCurrPrice);
        ((TextView) rootView.findViewById(R.id.price_open
        )).setText(""+myOpenPrice);
        return rootView;
    }
    private static String getChartURL(String symbol){
        return "http://api.stockdio.com/visualization/financial/charts/v1/HistoricalPrices?app-key=945CDE3317B845C6BE4021FFF18F65F5&symbol="+symbol+"&days=1&width=600&height=420";
    }
}
