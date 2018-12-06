package edu.temple.stock;

import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
//observes and triggers event if there is any changes in the file
public class PortfolioFileObserver extends FileObserver{
    static final int mask = (FileObserver.CREATE |
            FileObserver.DELETE |
            FileObserver.DELETE_SELF |
            FileObserver.MODIFY |
            FileObserver.MOVED_FROM |
            FileObserver.MOVED_TO |
            FileObserver.MOVE_SELF);

    private Context context;

    public PortfolioFileObserver(String path, Context context) {
        super(path, mask);
        this.context = context;
    }

    @Override
    public void onEvent(int i, @Nullable String s) {
        switch(i){
            case FileObserver.MODIFY://if any change happens for the file, it will initiate loacal broadcast
                //which can notify StockListActivity to read the update info form the file and update the UI
                boolean isCorrect = true;
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
                Intent localIntent = new Intent("file_change_listener");
                localIntent.putExtra("hasChanged", isCorrect);
                localBroadcastManager.sendBroadcast(localIntent);
                break;
            default:
                break;
        }
    }
}
