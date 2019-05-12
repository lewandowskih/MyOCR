package com.example.hubert.myocr;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.isseiaoki.simplecropview.CropImageView;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.example.hubert.myocr.Binarization.*;

public class MainActivity extends AppCompatActivity {
    private CropImageView imageView;
    private TextView textView;

    private Uri takenPhotoUri;
    private File takenPhotoFile;
    private static String uuid = UUID.randomUUID().toString();

    private static int DISK_CACHE_SIZE = 1024 * 1024 * 20; //20 MB
    private static DiskLruImageCache mDiskLruImageCache;

    private static final int PIC_TAKE = 100;
    private static final int PIC_GALLERY = 300;
    private static final int PIC_CROP = 200;
    private static final int[] segm = {TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT, TessBaseAPI.PageSegMode.PSM_SINGLE_LINE, TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK, TessBaseAPI.PageSegMode.PSM_AUTO};
    private static final String[] segmText = {"Page", "Line", "Block", "Auto"};
    private static int segmIter = 3;

    private static String MYOCR_IMGS = System.getenv("SECONDARY_STORAGE")+ "/MyOCR/imgs";
    private static String MYOCR_TXTS = System.getenv("SECONDARY_STORAGE")+ "/MyOCR/txts";
    private static final String SRC_IMAGE_KEY  = "source-image-key";
    private static final String OCR_IMAGE_KEY  = "blackwhite-image-key";

    private AppEngine ocrEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ocrEngine == null) {
            ocrEngine = new AppEngine(getSharedPreferences("MyOCRApp", MODE_PRIVATE));
        }


        EditText et = findViewById(R.id.editTextDataPath);
        if (ocrEngine.isTrainedDataLoaded()) {
            et.setText(ocrEngine.getTrainedDataPath());
        }
        else {
            et.setText("/");
        }

        if (ocrEngine.isTrainedDataLoaded()) {
            LinearLayout floating = findViewById(R.id.loadTrainedDataFloating);
            floating.setVisibility(GONE);

            Button b = findViewById(R.id.ocrButton);
            b.setEnabled(true);
            Log.e("DEBUG", "I'm in TRUE");
        }
        else{
            LinearLayout floating = findViewById(R.id.loadTrainedDataFloating);
            floating.setVisibility(VISIBLE);

            Button b = findViewById(R.id.ocrButton);
            b.setEnabled(false);

            Log.e("DEBUG", "I'm in FALSE");
        }

        TextView tv = findViewById(R.id.segmModeTV);
        tv.setText("Typ tekstu: " + segmText[segmIter]);

        imageView = findViewById(R.id.ocrImageView);
        textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if ( mDiskLruImageCache == null ) {
            mDiskLruImageCache = new DiskLruImageCache(getBaseContext(), "images", DISK_CACHE_SIZE);
        }
        mDiskLruImageCache.put(SRC_IMAGE_KEY, BitmapFactory.decodeResource(getResources(), R.drawable.page));

        imageView.load(resourceToUri(this.getBaseContext(), R.drawable.page)).execute(null);
        imageView.setCropMode(CropImageView.CropMode.FREE);
        imageView.setGuideShowMode(CropImageView.ShowMode.NOT_SHOW);

        Button menuButton = findViewById(R.id.button5);
        menuButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Button b = findViewById(R.id.button3);
                if (b.isShown()) {
                    b.setVisibility(View.GONE);
                }
                else {
                    b.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
    }

    public void onToggleTrainedDataClick(View view){
        LinearLayout floating = findViewById(R.id.loadTrainedDataFloating);
        floating.setVisibility(VISIBLE);
    }

    public void onToggleSearchClick(View view){
        LinearLayout floating = findViewById(R.id.browseInternetFloating);
        floating.setVisibility(VISIBLE);
        TextView tv = findViewById(R.id.textView);
        String keyword = tv.getText().toString().replace("\n", "").replace("\r","");
        EditText et = findViewById(R.id.editSearchedString);
        et.setText(keyword);
    }

    public void loadTrainedDataClick(View view) {
        EditText et = findViewById(R.id.editTextDataPath);
        if (ocrEngine.loadTrainedData(et.getText().toString())) {
            Button b = findViewById(R.id.ocrButton);
            b.setEnabled(true);
            LinearLayout floating = findViewById(R.id.loadTrainedDataFloating);
            floating.setVisibility(GONE);
        }
    }

    public void loadSearchButtonClick(View view) {
        PackageManager packageManager = getPackageManager();
        EditText et = findViewById(R.id.editSearchedString);
        String keyword = et.getText().toString();
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        if (intent.resolveActivity(packageManager) != null) {
            intent.putExtra(SearchManager.QUERY, keyword);
            startActivity(intent);
        }
        else {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.pl/search?q=" + keyword));
            startActivity(intent);
        }
    }

    public void onTakePicture(View view) {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle(getResources().getString(R.string.Image_photo_chooser_title));
        String[] pictureDialogItems = {getResources().getString(R.string.Image_photo_chooser_camera), getResources().getString(R.string.Image_photo_chooser_gallery)};
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                takePhotoFromCamera();
                                break;
                            case 1:
                                choosePhotoFromGallery();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(MYOCR_IMGS);
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(MYOCR_IMGS + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    private void choosePhotoFromGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PIC_GALLERY);
    }

    private void takePhotoFromCamera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takenPhotoFile = getOutputMediaFile();
        takenPhotoUri = Uri.fromFile(takenPhotoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, takenPhotoUri);
        startActivityForResult(intent, PIC_TAKE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            switch(requestCode) {
                case PIC_TAKE:
                    if (takenPhotoUri != null) {
                        imageView.load(takenPhotoUri).execute(null);
                        mDiskLruImageCache.put(SRC_IMAGE_KEY, getBitmapFromUri(takenPhotoUri));
                    }
                    else {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.load_photo_fail), Toast.LENGTH_SHORT);
                    }
                    break;
                case PIC_GALLERY:
                    Uri uri = data.getData();
                    if (uri != null) {
                        imageView.load(uri).execute(null);
                        mDiskLruImageCache.put(SRC_IMAGE_KEY, getBitmapFromUri(uri));
                    }
                    else {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.load_photo_fail), Toast.LENGTH_SHORT);
                    }
                    break;
                case PIC_CROP:
                    break;
            }
        }
    }

    public void segmentationModeClick(View view){
        TextView segmView = findViewById(R.id.segmModeTV);
        segmIter = (segmIter + 1) % segm.length;
        segmView.setText(getResources().getString(R.string.menu_segmentation_mode) + segmText[segmIter]);
        ocrEngine.setOCRPageSegmMode(segm[segmIter]);
    }

    @SuppressLint("StaticFieldLeak")
    public void onOCRButtonClick(View view) {
        hideAllDropDowns();
        final RectF rectf = imageView.getActualCropRect();
        Rect rect = new Rect();
        rectf.round(rect);
        ocrEngine.setOCRArea(imageView.getImageBitmap(), rect);
        final String[] OCRtext = new String[1];
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected void onPreExecute(){
                super.onPreExecute();
                textView.setVisibility(View.GONE);
            }

            @Override
            protected void onPostExecute(Void aVoid){
                super.onPostExecute(aVoid);
                if (OCRtext[0].isEmpty()){
                    textView.setText(getResources().getString(R.string.TEXTVIEW_OCR_failed));
                }
                else{
                    textView.bringToFront();
                    textView.setText(OCRtext[0]);
                    textView.setVisibility(VISIBLE);
                }
                textView.setVisibility(VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... params){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                OCRtext[0] = ocrEngine.getOCRText();
                return null;
            }
        }.execute();
    }

    public void onQuitButtonClick(View view){
        ocrEngine.end();
        this.finishAffinity();
    }

    public void onSaveButtonClick(View view){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String txt = textView.getText().toString();
        File dir = new File(MYOCR_TXTS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            if (dir.exists()) {
                File out = new File(MYOCR_TXTS + File.separator + "TEXT_" + timeStamp + ".txt");
                saveTextToFile(txt, out);
                Toast.makeText(getBaseContext(), getResources().getString(R.string.text_file_saved) + out.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.text_file_save_fail) + e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void onClearTmpButtonClick(View view){
        File dir = new File(MYOCR_IMGS);
        if (dir.isDirectory()) {
            for (File f:dir.listFiles()) {
                f.delete();
            }
        }
    }

    private void saveTextToFile(String text, File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        try {
            stream.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            stream.close();
        }
    }

    public void toggleOptionsClick(View view) {
        LinearLayout opts = findViewById(R.id.optionsDropDown);
        switch(opts.getVisibility()){
            case INVISIBLE:
            case GONE:
                hideAllDropDowns();
                opts.setVisibility(VISIBLE);
                break;
            case VISIBLE:
                opts.setVisibility(GONE);
        }
    }

    public void toggleMenuClick(View view) {
        LinearLayout opts = findViewById(R.id.menuDropDown);
        switch(opts.getVisibility()){
            case INVISIBLE:
            case GONE:
                hideAllDropDowns();
                opts.setVisibility(VISIBLE);
                break;
            case VISIBLE:
                opts.setVisibility(GONE);
        }
    }

    public void toggleEnhancementType(View view){
        TextView enhType = findViewById(R.id.enhType);
        SeekBar sb = findViewById(R.id.seekBar4);
        if (enhType.getText().equals(getResources().getString(R.string.menu_binarization_type_global))) {
            enhType.setText(getResources().getString(R.string.menu_binarization_type_segmented));
            sb.setMax(10);
        }
        else {
            enhType.setText(getResources().getString(R.string.menu_binarization_type_global));
            sb.setMax(255);
        }
    }

    public void enhanceButtonClick(View view) {
        TextView enhType = findViewById(R.id.enhType);
        SeekBar sb = findViewById(R.id.seekBar4);
        Bitmap outBmp;
        if (enhType.getText().equals(getResources().getString(R.string.menu_binarization_type_global))) {
            int threshold;
            if (sb.getProgress() == 0) {
                threshold = Binarization.findThreshold(createHistogram(mDiskLruImageCache.getBitmap(SRC_IMAGE_KEY)));
            } else {
                threshold = sb.getMax() - sb.getProgress();
            }
            outBmp = Binarization.binarize(mDiskLruImageCache.getBitmap(SRC_IMAGE_KEY), threshold);
        } else {
            int segmentSize = Math.max(sb.getProgress(), 1);
             outBmp = Binarization.segmentedBinarization(mDiskLruImageCache.getBitmap(SRC_IMAGE_KEY), mDiskLruImageCache.getBitmap(SRC_IMAGE_KEY).getWidth() / segmentSize);
        }
        mDiskLruImageCache.put(OCR_IMAGE_KEY, outBmp);
        imageView.load(getBitmapUri(outBmp)).execute(null);
    }

    public static Uri resourceToUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID) );
    }

    private void hideAllDropDowns(){
       findViewById(R.id.browseInternetFloating).setVisibility(GONE);
       findViewById(R.id.optionsDropDown).setVisibility(GONE);
       findViewById(R.id.menuDropDown).setVisibility(GONE);
       findViewById(R.id.textView).setVisibility(GONE);
    }


    private Uri getBitmapUri(Bitmap bmp) {
        File tempFile = null;
        try {
            File tempDir = Environment.getExternalStorageDirectory();
            tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
            tempDir.mkdirs();
            tempFile = File.createTempFile(this.uuid, ".jpg", tempDir);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            byte[] bitmapData = bytes.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(tempFile);
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        Bitmap bitmap = null;
        try {
            InputStream input = this.getContentResolver().openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;//optional
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();

            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inDither = true; //optional
            bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//
            input = this.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}