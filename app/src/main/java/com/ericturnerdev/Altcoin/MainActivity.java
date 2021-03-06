package com.ericturnerdev.Altcoin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends Activity {

    String TAG = "MainActivity";

    BasicNameValuePair nvp;

    Context mContext;

    //Exchange API URLs:
    public final String CRYPTSY_API = "http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=";
    //public final String CRYPTSY_API = "http://www.cryptocoincharts.info/v2/api/tradingPairs";
    public final String CRYPTOCOIN_API = "http://www.cryptocoincharts.info/v2/api/tradingPairs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.setTitle("Altcoin");
        super.onCreate(savedInstanceState);
        //If Pairs doesn't exist
        new Pairs();
        //Log.i(TAG, "aaa onCreate CALLED");

        /*
        //DEBUGGING FORMATTING ERROR THAT WAS CAUSING CRASHES:
       // String debugTest = "0.014";
        double debugTestD = 110.0014;
        String debugTest = String.format("%.3f", debugTestD);
        //Lines: 213, 198, 200, 202
        BigDecimal bd = new BigDecimal(debugTest);
        DecimalFormat df = new DecimalFormat("0.0##");
        debugTest = df.format(bd.stripTrailingZeros());
        Log.i(TAG, "debugTest is: " + debugTest);
        */


    } //End onCreate()

    @Override
    protected void onStart() {


        super.onStart();
        mContext = this;
        //new Pairs();
        //Log.i(TAG, "aaa onStart() CALLED");


        //Check for SQLite Database
        DatabaseHandler db = new DatabaseHandler(this);
        //db.clearTable("visibility");
       // if (marketsCount == 0) //Log.i(TAG, "Vis table is empty");
        //else //Log.i(TAG, "Vis table has " + marketsCount + " rows");

        Cursor cur;

        //Set Pairs visibility from SQLite database
        cur = db.printMarkets();
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {

                if (cur.getInt(1) == 1) {
                    try {
                        Pairs.getMarket(cur.getInt(0)).setVisible(true);
                    } catch (NullPointerException e) {
                        Log.v(TAG, "The 1.2 crash bug has occurred.  Stack trace:\n" + e.getMessage() + "\n" + e.getStackTrace());
                    }

                } else {
                    try {
                        Pairs.getMarket(cur.getInt(0)).setVisible(false);
                    } catch (NullPointerException e) {
                        Log.v(TAG, "The 1.2 crash bug has occurred.  Stack trace:\n" + e.getMessage() + "\n" + e.getStackTrace());
                    }
                }
            }
        }

        //new CryptoCoin(Pairs.getVisibleMarkets()).execute();

        String marketLabel = "";

        for (Market m : Pairs.getVisibleMarkets()) {


            /*EDIT THIS*/
            marketLabel += m.getSecondarycode().toLowerCase() + "_" + m.getPrimarycode().toLowerCase() + ",";


        }

        if (marketLabel.length() > 0) {
            marketLabel = marketLabel.substring(0, marketLabel.length() - 1);
        }

        nvp = new BasicNameValuePair("pairs", marketLabel);

        /*
        nvp = new ArrayList<NameValuePair>();
        nvp.add(new BasicNameValuePair("marketid", "" + m.getMarketid()));
        new APIData(CRYPTSY_API, false, nvp, m.getSecondarycode(), m.getPrimarycode()).execute();
        */

        //Display Visible Pairs
        if (Pairs.getVisibleMarkets().size() > 0) {
            setContentView(R.layout.fragment_main2);
            populateListView();
            //Log.i(TAG, "getvisible is greater than 0!");
           // new CryptoCoin(marketLabel).execute();
            new Cryptsy(marketLabel).execute();

        } else {
            setContentView(R.layout.init_splash);
            //Log.i(TAG, "getvisible is not greater than 0!");
        }

    }

    @Override
    protected void onStop() {

        super.onStop();
        //DatabaseHandler db = new DatabaseHandler(this);
        //db.clearTable("visibility");
        //db.addVis(Pairs.getMarket(132), 1);
        //db.dropTable("visibility");
        //Log.i(TAG, "aaa onStop CALLED");
        //db.close();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //Log.i(TAG, "aaa onRestart CALLED");

    }

    @Override
    protected void onResume() {

        //Log.i(TAG, " aaa onResume CALLED");
        super.onResume();

    }

    //Place items on the action bar
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the menu items
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        inflater.inflate(R.menu.refresh, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Handle presses on the action bar items
        switch (item.getItemId()) {

            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);

            case R.id.menu_refresh:
                //new Pairs();

                this.onStart();
                //new Cryptsy(this).execute(CRYPTSY_API);


        }

        return true;
    }


    public void populateListView() {


        try {
            ListView list = (ListView) findViewById(R.id.fragment_list_view);
            list.setAdapter(new PairAdapter(mContext, R.layout.pair_item_view, Pairs.getVisibleMarkets()));
            list.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    //Log.i(TAG, "List item clicked");


                }

            });

        }catch(Exception e){ System.out.println("populateListView failed!" + e.getStackTrace()); }

    }

    public class CryptoCoin extends AsyncTask<String, Void, Double> {

        //public String TAG = "CryptoCoin";
        BasicNameValuePair nvp;
        String pairsS;
        String API_URL = CRYPTOCOIN_API;

        public CryptoCoin(String s) {

            nvp = new BasicNameValuePair("pairs", s);
            pairsS = s;

        }

        public void getData() {

            String rawData = "";
            //String fullURL;
            int i = 0;
            boolean apiSuccess = false;
            Market currentMarket;


            //Get the data from the API
            while (!apiSuccess && i < 20) {

                try {

                    apiSuccess = true;
                    try {

                        rawData = new URLFetch().postURL(API_URL, nvp);
                        //Log.i(TAG, "POST rawData: " + rawData);

                    } catch (IOException e) {
                        //Log.e(TAG, "aaa Couldn't load data from api.  i is: " + i);
                        i++;
                        apiSuccess = false;
                    }


                    if (apiSuccess){

                        Gson gson = new GsonBuilder().serializeNulls().create();
                        JSONArray resultsJ = new JSONArray(rawData);
                        //Log.i(TAG, "resultsJ is: " + resultsJ);
                        //Log.i(TAG, "CRYPTSY IS " + Cryptsy);

                        for (i = 0; i < resultsJ.length(); i++) {

                            JSONObject marketJ = resultsJ.getJSONObject(i);
                            //Log.i(TAG, "  marketJ is: " + marketJ);
                            currentMarket = gson.fromJson(marketJ.toString(), Market.class);
                            currentMarket.setSecondarycode(currentMarket.getId().substring(0, currentMarket.getId().indexOf("/")));
                            currentMarket.setPrimarycode(currentMarket.getId().substring(currentMarket.getId().indexOf("/") + 1, currentMarket.getId().length()));
                            //Log.i(TAG, "  currentMarket price: " + currentMarket.getPrice());
                            //Log.i(TAG, "  currentMarket label: " + currentMarket.getId());
                            //Log.i(TAG, "  currentMarket 24hr : " + currentMarket.getPrice_before_24h());
                            //Log.i(TAG, " currentMarket volume: " + currentMarket.getVolume_btc());
                            //Log.i(TAG, " primaryCode: " + currentMarket.getPrimarycode() + " secondaryCode: " + currentMarket.getSecondarycode());

                            Pairs.getMarket(currentMarket.getPrimarycode(), currentMarket.getSecondarycode()).setPrice(currentMarket.getPrice());
                            Pairs.getMarket(currentMarket.getPrimarycode(), currentMarket.getSecondarycode()).setVolume_btc(currentMarket.getVolume_btc());
                            Pairs.getMarket(currentMarket.getPrimarycode(), currentMarket.getSecondarycode()).setPrice_before_24h(currentMarket.getPrice_before_24h());


                        }

                    }

                } catch (JSONException e) {
                    //Log.e(TAG, "JSON Exception! i is: " + i);
                    e.printStackTrace();
                    //Log.i(TAG, "Primarycode: " + Pairs.getMarket(_marketId).getPrimarycode());
                    i++;
                    apiSuccess = false;
                }

            }
        } //end getData method

        protected Double doInBackground(String... params) {

            getData();
            return 0.0;
        }

        protected void onPostExecute(Double d) {

            populateListView();
            }


    } //end CryptoCoin class


    public class Cryptsy extends AsyncTask<String, Void, Double> {

        //public String TAG = "CryptoCoin";
        BasicNameValuePair nvp;
        String pairsS;
        String API_URL = CRYPTSY_API;
        int[] marketIds;

        public Cryptsy(String label) {

            //Log.i(TAG, "CRYPTSY RUNNING");
            nvp = new BasicNameValuePair("pairs", label);
            pairsS = label;
            String[] splitLabels = label.split(",");
            marketIds = new int[splitLabels.length];

            for ( int i=0; i<splitLabels.length; i++){
                //Log.i(TAG, "splitLabels: " + splitLabels[i]);
                marketIds[i] = Pairs.getMarketId(splitLabels[i]);
            }

        }

        public void getData() {

           // Log.i(TAG, "marketIds: " + Arrays.toString(marketIds));
            String rawData = "";

            int i=0;
            //String fullURL;
            boolean apiSuccess = false;
            Market currentMarket;

            //Get the data from the API
            while (!apiSuccess && i < 20) {

                for (int j=0; j<marketIds.length; j++){

                    try {

                        apiSuccess = true;
                        try {

                            //Want to do get request for
                            //rawData = new URLFetch().postURL(API_URL, nvp);

                            //YOU WILL ACTUALLY NEED A FOR LOOP HERE:
                            rawData = new URLFetch().getURL( "" + API_URL + marketIds[j]);
                          //  Log.i(TAG, "GET rawData: " + rawData);

                        } catch (IOException e) {
                            //Log.e(TAG, "aaa Couldn't load data from api.  i is: " + i);
                            i++;
                            apiSuccess = false;
                        }

                        if (apiSuccess){

                            Gson gson = new GsonBuilder().serializeNulls().create();
                            JSONObject resultsJ = new JSONObject(rawData);
                            resultsJ = resultsJ.getJSONObject("return").getJSONObject("markets");
                            //JSONArray sellOrdersJ = resultsJ.getJSONObject();
                            //JSONArray resultsJ = new JSONArray(rawData);
                            //Log.i(TAG, "resultsJ is: " + resultsJ);
                            //Log.i(TAG, "CRYPTSY IS " + Cryptsy);
                            //currentMarket = gson.fromJson(resultsJ.getJSONObject(Pairs.getMarket(marketIds[j]).getSecondarycode()).toString(), Market.class);
                            currentMarket = gson.fromJson(resultsJ.toString(), Market.class);
                            //Log.i(TAG, "GSON currentMarket: " + currentMarket.getLasttradeprice());

                            //Figuring out what we have in the resultsJ variable:
                            //Log.i(TAG, "RESULTSJ ANALYSIS - raw: " + resultsJ.toString());
                            //Log.i(TAG, "RESULTSJ ANALYSIS - oneStepDown: " + gson.fromJson(resultsJ.getJSONObject(Pairs.getMarket(marketIds[j]).getSecondarycode()).toString(), Market.class));
                            JSONObject marketJ = resultsJ.getJSONObject(Pairs.getMarket(marketIds[j]).getSecondarycode());
                            //Log.i(TAG, "RESULTSJ ANALYSIS - price: " + marketJ.getString("lasttradeprice"));


                            //Get price from 24 hours ago:
                            JSONArray recentTradesJ = marketJ.getJSONArray("recenttrades");
                            //Log.i(TAG, "RESULTSJ ANALYSIS - recentTradesJ length: " + recentTradesJ.length());
                            double accumulator = 0.0;
                            if(recentTradesJ.length() > 0){
                                for (int k=0; k<recentTradesJ.length(); k++){
                                    accumulator+= recentTradesJ.getJSONObject(k).getDouble("price");
                                }
                            }

                            accumulator = accumulator/(double)recentTradesJ.length();

                            //Get buyorders
                            JSONArray buyOrdersJ = marketJ.getJSONArray("buyorders");
                            //Log.i(TAG, "BUYORDERSJ: " + buyOrdersJ.toString());
                            ArrayList<BuySellItem> buyOrdersT = new ArrayList<BuySellItem>();
                            BuySellItem temp_bsi = new BuySellItem(0.0, 0.0, 0.0);

                            //if (buyOrdersJ.length() > 0){
                                for (int l=0; l<buyOrdersJ.length(); l++) {


                                    temp_bsi.setPrice(buyOrdersJ.getJSONObject(l).getDouble("price"));
                                    temp_bsi.setQuantity(buyOrdersJ.getJSONObject(l).getDouble("quantity"));
                                    temp_bsi.setTotal(buyOrdersJ.getJSONObject(l).getDouble("total"));

                                    buyOrdersT.add(temp_bsi);
                                }
                            //}

                            //else{ buyOrdersT.add(new BuySellItem(0.0, 0.0, 0.0)); }

                            //Log.i(TAG, "BUYORDERST: " + buyOrdersT.toString());

                            //Get sellorders
                            JSONArray sellOrdersJ = marketJ.getJSONArray("sellorders");
                            //Log.i(TAG, "SELLORDERSJ: " + sellOrdersJ.toString());
                            ArrayList<BuySellItem> sellOrdersT = new ArrayList<BuySellItem>();
                            BuySellItem temp_ssi = new BuySellItem(0.0, 0.0, 0.0);

                            //if (sellOrdersJ.length() > 0){
                                for (int l=0; l<sellOrdersJ.length(); l++) {


                                    temp_ssi.setPrice(sellOrdersJ.getJSONObject(l).getDouble("price"));
                                    temp_ssi.setQuantity(sellOrdersJ.getJSONObject(l).getDouble("quantity"));
                                    temp_ssi.setTotal(sellOrdersJ.getJSONObject(l).getDouble("total"));

                                    sellOrdersT.add(temp_ssi);
                                }
                            //}

                            //else{ sellOrdersT.add(new BuySellItem(0.0, 0.0, 0.0)); }
                                 //end getting sellorders

                            double actualVolume = Double.parseDouble(marketJ.getString("volume"))*Double.parseDouble(marketJ.getString("lasttradeprice"));

                            //Set the values
                            Pairs.getMarket(marketIds[j]).setPrice(Double.parseDouble(marketJ.getString("lasttradeprice")));
                            Pairs.getMarket(marketIds[j]).setLabel(marketJ.getString("label"));
                            Pairs.getMarket(marketIds[j]).setMarketid(marketJ.getInt("marketid"));
                            Pairs.getMarket(marketIds[j]).setPrimarycode(marketJ.getString("secondarycode"));
                            Pairs.getMarket(marketIds[j]).setSecondarycode(marketJ.getString("primarycode"));
                            Pairs.getMarket(marketIds[j]).setPrimaryname(marketJ.getString("primaryname"));
                            Pairs.getMarket(marketIds[j]).setSecondaryname(marketJ.getString("secondaryname"));
                            Pairs.getMarket(marketIds[j]).setVolume_btc(actualVolume);
                            Pairs.getMarket(marketIds[j]).setVolume(actualVolume);
                            Pairs.getMarket(marketIds[j]).setPrice_before_24h(accumulator);
                            Pairs.getMarket(marketIds[j]).setLasttradeprice(marketJ.getDouble("lasttradeprice"));

                            //Store the values in currentMarket:
                            Pairs.getMarket(marketIds[j]).setBuyorders(buyOrdersT);
                            Pairs.getMarket(marketIds[j]).setSellorders(sellOrdersT);


                            //Log.i(TAG, "SELLORDERST: " + buyOrdersT.toString());

                            Pairs.getMarket(marketIds[j]).setPrice(Double.parseDouble(marketJ.getString("lasttradeprice")));
                            Pairs.getMarket(marketIds[j]).setPrice_before_24h(accumulator);

                            //Pairs.getMarket(marketIds[j]).setRecenttrades()

                            //Log.i(TAG, "YYYFFF CURRENTMARKET: " + currentMarket.toString());

                            //Log.i(TAG, "BBBBBBBBBB\n Pairs version: " + Pairs.getMarket(marketIds[j]).toString());
                        }


                    } catch (Exception e) {
                        //Log.e(TAG, "JSON Exception! i is: " + i);
                        e.printStackTrace();
                        //Log.i(TAG, "Primarycode: " + Pairs.getMarket(_marketId).getPrimarycode());
                        i++;
                        apiSuccess = false;
                    }
                }

            }
        } //end getData method

        protected Double doInBackground(String... params) {

            getData();
            return 0.0;
        }

        protected void onPostExecute(Double d) {

            populateListView();
        }


    } //end Cryptsy class
}





