package be.pxl.troger.ar.tools;

import android.util.Log;

import java.util.HashMap;

/**
 * includes hardcoded the barcodes to
 * handle and their corresponding commands
 * @author Michael Troger
 */
public class BarcodeDatabase {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = BarcodeDatabase.class.getName();
    /**
     * hold the database
     * key -> is the barcode value (String)
     * value -> is the command to apply (String)
     */
    private HashMap<String, String> dataBase;

    /**
     * create an instance of the BarcodeDatabase
     */
    public BarcodeDatabase() {
        dataBase = new HashMap<String, String>();
        fillDatabase();

        Log.d(TAG, "started :)");
    }

    /**
     * fill the database hardcoded
     */
    private void fillDatabase() {
        dataBase.put("123456", "right");
        dataBase.put("Was geht ab?\r\n\r\nEs funktioniert?", "left");
    }

    /**
     * get the barcode database
     * @return returns the database as HashMap
     */
    public HashMap<String, String> getDataBase() {
        return dataBase;
    }
}
