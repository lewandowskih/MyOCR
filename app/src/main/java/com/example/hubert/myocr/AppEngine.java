package com.example.hubert.myocr;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;


public class AppEngine {

    private String trainedDataPath;
    private SharedPreferences settings;
    private TessBaseAPI mTess;
    private boolean isTrainedDataLoaded = false;


    public AppEngine(SharedPreferences settings){
        this.settings = settings;
        loadTrainedDataPathFromSharedPreferences();
        Log.e("DEBUG trainedDataPath", "XXX " + this.trainedDataPath + " XXX");
        if (this.trainedDataPath != null) {
            loadTrainedData(this.trainedDataPath);
        }
    }

    private void loadTrainedDataPathFromSharedPreferences(){
        this.trainedDataPath = this.settings.getString("trainedDataPath", null);
    }

    private void saveTrainedDataPathToSharedPreferences(){
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("trainedDataPath", this.trainedDataPath);
        editor.apply();
    }

    public boolean loadTrainedData(String datapath) {
        File datapathFile = new File(datapath);
        String[] fileparts = datapath.split(File.separator);
        if (datapathFile.isFile() && fileparts[fileparts.length - 2].equals("tessdata")) {
            String fileName = datapathFile.getName();
            String lang = "";
            if (fileName.length() >= 3) {
                lang = fileName.substring(0, 3);
            }
            String tessParent = (new File(datapathFile.getParent())).getParent();

            Log.e("DEBUG datapathFile", datapathFile.exists() + " " + datapathFile.toString());
            mTess = new TessBaseAPI();
            if (mTess.init(tessParent, lang)) {
                Log.e("DEBUG", "Overwriting trainedDataPath: " + trainedDataPath + " --> " + datapathFile.getAbsolutePath());
                if (this.trainedDataPath == null || !this.trainedDataPath.equals(datapathFile.getAbsolutePath())) {
                    this.trainedDataPath = datapathFile.getAbsolutePath();
                    saveTrainedDataPathToSharedPreferences();
                }
            }
            isTrainedDataLoaded = true;
            return true;
        }
        isTrainedDataLoaded = false;
        Log.getStackTraceString(new Throwable());
        return false;
    }

    public String getTrainedDataPath(){
        return trainedDataPath;
    }

    public boolean isTrainedDataLoaded(){
        return isTrainedDataLoaded;
    }

    public void setOCRArea(Bitmap image, Rect rect){
        mTess.clear();
        mTess.setImage(image);
        mTess.setRectangle(rect);
    }

    public void setOCRPageSegmMode(int mode){
        mTess.setPageSegMode(mode);
    }

    public String getOCRText(){
        return mTess.getUTF8Text();
    }

    public void end(){
        mTess.end();
    }
}
