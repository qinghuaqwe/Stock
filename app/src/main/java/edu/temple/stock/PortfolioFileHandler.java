package edu.temple.stock;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
//used to read data info from internal file
public class PortfolioFileHandler {
    private static final String fileName = "portfolio_file.txt";
    //default constructor
    public PortfolioFileHandler() {
    }//end constructor
    public List<String> readSymbols(Context context) {
        List<String> lstSymbols = new ArrayList<>();
        try {
            File directory = context.getFilesDir();
            File file = new File(directory, fileName);
            FileReader fileReader = new FileReader(file);//create a FileReader to read input
            //if the file reader is not null
            if (fileReader != null) {
                BufferedReader bufferedReaderFile = new BufferedReader(fileReader); //create a buffer reader to read the file
                String line = null;
                //while there is a line in the text file
                while ((line = bufferedReaderFile.readLine()) != null) {
                    Log.d("Reading symbols: ", line);
                    lstSymbols.add(line);
                } //end while loop
                bufferedReaderFile.close();//close the buffer reader
                fileReader.close();//close the file reader
            }//end if - file reader  = null
        } catch (Exception e) {
            e.printStackTrace();
        }//end try catch
        return lstSymbols;
    }
    public static String getPortfolioPath(Context context){
        File internalDirectory = context.getFilesDir();
        //create a new file instance with the path: internal directory/category
        File fileDirectory = new File(internalDirectory, fileName);
        return fileDirectory.getPath();
    }
    //write the information to file
    public void writeSymbols(List<String> symbols, Context context) {
        try {
            File internalDirectory = context.getFilesDir();//get the directory to store data internally
            //create a new file instance with the path: internal directory/category
            File fileDirectory = new File(internalDirectory, fileName);
            if(fileDirectory.isDirectory()){
                fileDirectory.delete();
            }
            //open file for output in private mode and append mode
            FileOutputStream outputStream = new FileOutputStream(fileDirectory, false);
            outputStream.flush();//flush the buffer in case there is anything in it
            for(String symbol: symbols) {
                outputStream.write(symbol.getBytes());//write stock information to the file
                outputStream.write("\n".getBytes());//write a new line to the file
            }
            outputStream.flush();//flush the buffer
            outputStream.close();//close the output stream
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
