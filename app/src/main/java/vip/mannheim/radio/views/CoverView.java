package vip.mannheim.radio.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import vip.mannheim.radio.R;

public class CoverView extends View {

    Paint paint;
    Bitmap bitmapSource;
    Bitmap bitmapFon;
    Bitmap bitmapFrame;
    Bitmap stationLogoFlare;

    private Bitmap bitmap;

    private Bitmap getVariable() {
        //Log.d("LOG_TAG", "getSomeVariable() ");
        return bitmap;
    }

    public void setVariable(Bitmap bitmap) {
        this.bitmap = bitmap;
        //Log.d("LOG_TAG", "setSomeVariable() ");
        requestLayout();
    }

    public CoverView(Context context) {
        super(context);
        init(null);
    }

    public CoverView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CoverView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public CoverView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void init(@Nullable AttributeSet set){
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapFon = resizeImageForImageView(BitmapFactory.decodeResource(getResources(), R.drawable.logo), 285);
        bitmapFrame = resizeImageForImageView(BitmapFactory.decodeResource(getResources(), R.drawable.stations_frame), 350);
        stationLogoFlare = resizeImageForImageView(BitmapFactory.decodeResource(getResources(), R.drawable.station_logo_flare), 320);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try{
            bitmapSource = resizeImageForImageView(getVariable(), 285);
        }catch (Exception e){
            bitmapSource = resizeImageForImageView(BitmapFactory.decodeResource(getResources(), R.drawable.station_logo_default), 285);
        }
        bitmapReflection(bitmapOverlayToCenter(bitmapFrame, bitmapSource, bitmapFon));
        canvas.drawRect(0, 0, 350, 700, paint);
    }

    private Bitmap resizeImageForImageView(Bitmap bitmap, int scaleSize) {
        Bitmap resizedBitmap = null;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int newWidth = -1;
        int newHeight = -1;
        float multFactor = -1.0F;
        if(originalHeight > originalWidth) {
            newHeight = scaleSize ;
            multFactor = (float) originalWidth/(float) originalHeight;
            newWidth = (int) (newHeight*multFactor);
        } else if(originalWidth > originalHeight) {
            newWidth = scaleSize ;
            multFactor = (float) originalHeight/ (float)originalWidth;
            newHeight = (int) (newWidth*multFactor);
        } else if(originalHeight == originalWidth) {
            newHeight = scaleSize ;
            newWidth = scaleSize ;
        }
        resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
        return resizedBitmap;
    }

    private void bitmapReflection(Bitmap bitmap){

        Bitmap finalBitmap = Bitmap.createBitmap(bitmap.getWidth(), (bitmap.getHeight()*2)+100, bitmap.getConfig());
        Canvas canvas = new Canvas(finalBitmap);

        canvas.drawBitmap(bitmap, 0, 0, null);

        final Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        Bitmap refBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        canvas.drawBitmap(fastBlur(refBitmap, 1, 3), 0, bitmap.getHeight()-15, null);
        canvas.drawBitmap(stationLogoFlare, 0, bitmap.getHeight()-17, null);

        Shader shaderA = new LinearGradient(0, bitmap.getHeight()-10, 0, bitmap.getHeight()+60, 0xffffffff, 0x00ffffff, Shader.TileMode.CLAMP);
        Shader shaderB = new BitmapShader(finalBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(new ComposeShader(shaderA, shaderB, PorterDuff.Mode.SRC_IN));
    }

    private Bitmap bitmapOverlayToCenter(Bitmap mBitmapFrame, Bitmap mBitmapSource, Bitmap fon) {
        int bitmap1Width = mBitmapFrame.getWidth();
        int bitmap1Height = mBitmapFrame.getHeight();
        int bitmap2Width = mBitmapSource.getWidth();
        int bitmap2Height = mBitmapSource.getHeight();

        float marginLeft = (float) (bitmap1Width * 0.5 - bitmap2Width * 0.5);
        float marginTop = (float) (bitmap1Height * 0.5 - bitmap2Height * 0.5);

        float fonMarginLeft = (float) (bitmap1Width * 0.5 - fon.getWidth() * 0.5);
        float fonMarginTop = (float) (bitmap1Height * 0.5 - fon.getHeight() * 0.5);

        Bitmap finalBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, mBitmapFrame.getConfig());
        Canvas canvas = new Canvas(finalBitmap);

        canvas.drawBitmap(fon, fonMarginLeft, fonMarginTop + 11, null);
        canvas.drawBitmap(mBitmapSource, marginLeft, marginTop + 11, null);
        canvas.drawBitmap(mBitmapFrame, 0, 0, null);

        return finalBitmap;
    }

    private Bitmap fastBlur(Bitmap sentBitmap, float scale, int radius) {

        int width = Math.round(sentBitmap.getWidth() * scale);
        int height = Math.round(sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }
}
