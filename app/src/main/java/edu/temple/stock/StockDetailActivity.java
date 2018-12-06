package edu.temple.stock;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import edu.temple.stock.model.Symbol;
//implement activity to display details of the stock, triggered by click certain stock in the stock list
public class StockDetailActivity extends AppCompatActivity {

    private TextView companyName;
    private TextView currPrice;
    private TextView openPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own detail action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        //show the Up(use to go back to stock list) button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //saveInstanceState is non-null when fragment status is saved from a previous movement of the activity
        //for instance, switch from portrait mode to landscape mode
        //Fragment will automatically re-added to container and we do not have to do this process mamually
        if (savedInstanceState == null) {
            //create the detail fragment and add it to the activity using a fragment transaction
            Bundle arguments = new Bundle();
            arguments.putString(StockDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(StockDetailFragment.ARG_ITEM_ID));
            arguments.putString(StockDetailFragment.ARG_COMPANY_NAME,
                    getIntent().getStringExtra(StockDetailFragment.ARG_COMPANY_NAME));
            arguments.putDouble(StockDetailFragment.ARG_CURR_STOCK_PRICE,
                    getIntent().getDoubleExtra(StockDetailFragment.ARG_CURR_STOCK_PRICE, 0.0));
            arguments.putDouble(StockDetailFragment.ARG_OPENING_STOCK_PRICE,
                    getIntent().getDoubleExtra(StockDetailFragment.ARG_OPENING_STOCK_PRICE, 0.0));
            StockDetailFragment fragment = new StockDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
            toolbar.setTitle(getIntent().getStringExtra(StockDetailFragment.ARG_ITEM_ID));
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //ID represent the home button which allows to display a button to navigate back to stock list.
            navigateUpTo(new Intent(this, StockListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
