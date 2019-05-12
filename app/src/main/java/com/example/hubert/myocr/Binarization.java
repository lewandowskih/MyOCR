package com.example.hubert.myocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static android.graphics.Bitmap.createBitmap;

/**
 * Created by Hubert on 2018-02-07.
 */

public class Binarization {

    static Bitmap binarize(Bitmap image, int threshold){
        int W = image.getWidth();
        int H = image.getHeight();

//        Bitmap imgOut = createBitmap(W, H, image.getConfig());
        Bitmap imgOut = image.copy(image.getConfig(), true);

        for (int w = 0; w < W; w++){
            for (int h = 0; h < H; h++){
                int c = image.getPixel(w, h);
                int r = Color.red(c);
                int g = Color.green(c);
                int b = Color.blue(c);
                int color = (r + g + b) / 3;
                imgOut.setPixel(w, h, ((color > threshold)? Color.WHITE : Color.BLACK));
            }
        }
        return imgOut;
    }

    static int[] createHistogram(Bitmap img) {
        int[] hist = new int[256];
        int W = img.getWidth();
        int H = img.getHeight();
        for (int w = 0; w < W; w++){
            for (int h = 0; h < H; h++){
                int c = img.getPixel(w, h);
                int r = Color.red(c);
                int g = Color.green(c);
                int b = Color.blue(c);
                int color = (r + g + b) / 3;
                hist[color]++;
            }
        }
        return hist;
    }

    static int findThreshold(int[] hist){
      /*attempt for otsu method*/
      int total = 0;
      for (int i : hist){
          total += i;
      }
      int sumB = 0, wB = 0;
      double maximum = 0.0;
      int threshold = 0;
      int sum1 = 0;
      for (int i = 0; i < 256; i++){
          sum1 += i * hist[i];
      }
      for (int i = 0; i < 256 ; i++){
          wB += hist[i];
          double wF = total - wB;
          if (wB == 0 || wF == 0) {
                continue;
            }
            sumB += (i * hist[i]);
            double mF = (sum1 - sumB) / wF;
            double between = wB * wF * ((sumB / wB) - mF) * ((sumB / wB) - mF);
            if (between >= maximum) {
                threshold = i;
                maximum = between;
            }
        }
        return threshold;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap segmentedBinarization(Bitmap sourceBitmap, int segmentSize){
        int W = sourceBitmap.getWidth();
        int H = sourceBitmap.getHeight();

        if (segmentSize > W || segmentSize > H){
            segmentSize = Math.min(W, H);
        }
//        Log.e("WIDTH", W + "");
//        Log.e("HEIGHT", H + "");
//        Log.e("SegmentSize", segmentSize + "");
        int DIM_X = segmentSize;
        int DIM_Y = DIM_X;
        Bitmap outImg = createBitmap(W, H, sourceBitmap.getConfig());
        //optimization
        ArrayList<Integer> widths = new ArrayList<Integer>();
        ArrayList<Integer> heights = new ArrayList<Integer>();

        int segmentsCountW = W / DIM_X;

        for (int x = 0; x < segmentsCountW; x++){
            widths.add(DIM_X);
        }
        int lastSegmentSize = widths.get(segmentsCountW - 1) + (W % DIM_X);
        widths.remove(segmentsCountW - 1);
        widths.add(lastSegmentSize);


        int segmentsCountH = H / DIM_Y;
        for (int y = 0 ; y < segmentsCountH; y++){
            heights.add(DIM_Y);
        }
        lastSegmentSize = heights.get(segmentsCountH - 1) + (H % DIM_Y);
        heights.remove(segmentsCountH - 1);
        heights.add(lastSegmentSize);

//        Log.e("widths", widths.toString());
//        Log.e("heights", heights.toString());

        for (int x = 0; x < segmentsCountW; x++){
            for (int y = 0; y < segmentsCountH; y++){
                Bitmap tmpImg = createBitmap(sourceBitmap, x * DIM_X, y * DIM_Y, widths.get(x), heights.get(y));
//                Log.e("coords", "from x=" + (x * DIM_X) + " y=" + (y * DIM_Y) + "   w=" + widths.get(x) + " h=" + heights.get(y));
                int threshold = findThreshold(createHistogram(tmpImg));
                tmpImg = binarize(tmpImg, threshold);
                int[] pxl = new int[widths.get(x) * heights.get(y)];
                tmpImg.getPixels(pxl, 0, widths.get(x), 0, 0, widths.get(x), heights.get(y));
                outImg.setPixels(pxl, 0, widths.get(x), x * DIM_X, y * DIM_Y, widths.get(x), heights.get(y));
            }
        }
        return outImg;
    }
}
