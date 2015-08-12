package com.example.curlRollaway;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.*;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.InputStream;

public class MyActivity extends Activity {
    private static final String[] pages = {"one", "two", };
    private Pager pager;
    private PagerFactory pagerFactory;
    private Bitmap mCurPageBitmap, mRightPageBitmap, mLeftPageBitmap;
    private Canvas mCurPageCanvas, mRightPageCanvas, mLeftPageCanvas;
    private int screenWidth;
    private int screenHeight;
    //第一次拖拽的点
    PointF mFirstTouch = new PointF();
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initData();
        initView();
    }

    private void initData(){
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
    }

    private void initView(){
        pager = new Pager(this, screenWidth, screenHeight);
        manageLayer(pager, true);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(pager, layoutParams);

        initCanvas();
        pagerFactory = new PagerFactory(getApplicationContext());

        loadImage(mCurPageCanvas, 0);
        pager.setBitmaps(mCurPageBitmap, null);

        pager.setOnTouchListener(new View.OnTouchListener() {

            private int count = pages.length;
            private int currentIndex = 0;
            private int lastIndex = 0;
            private boolean canSetLeftBitmap = true;
            private boolean canSetRightBitmap = true;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                boolean ret = false;
                if (v == pager) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        if(pager.isAnimationRunning()) return false;

                        mFirstTouch.x = e.getX();
                        mFirstTouch.y = e.getY();

                        loadImage(mCurPageCanvas, currentIndex);
                        pager.setBitmaps(mCurPageBitmap, null);

                        lastIndex = currentIndex;
                        if(currentIndex == 0){
                            if(count > 1){
                                loadImage(mRightPageCanvas, currentIndex + 1);
                            }
                        }else if(currentIndex + 1 == count){
                            loadImage(mLeftPageCanvas, currentIndex - 1);
                        }else{
                            loadImage(mLeftPageCanvas, currentIndex - 1);
                            loadImage(mRightPageCanvas, currentIndex + 1);
                        }
                    } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                        if((e.getX() - mFirstTouch.x) > 0){
                            if(currentIndex == 0){
                                return false;
                            }else{
                                if(canSetLeftBitmap){
                                    canSetLeftBitmap = false;
                                    canSetRightBitmap = true;
                                    pager.setBitmaps(mCurPageBitmap, mLeftPageBitmap);
                                }
                            }
                        }else{
                            if(currentIndex + 1 == count){
                                return false;
                            }else{
                                if(canSetRightBitmap) {
                                    canSetRightBitmap = false;
                                    canSetLeftBitmap = true;
                                    pager.setBitmaps(mCurPageBitmap, mRightPageBitmap);
                                }
                            }
                        }
                    } else if (e.getAction() == MotionEvent.ACTION_UP) {
                        canSetLeftBitmap = true;
                        canSetRightBitmap = true;
                        if (!pager.canDragOver()) {
                            currentIndex = lastIndex;
                        }else{
                            if((e.getX() - mFirstTouch.x) > 0){
                                if(currentIndex > 0){
                                    currentIndex--;
                                }
                            }else{
                                if(currentIndex < count - 1){
                                    currentIndex++;
                                }
                            }
                        }
                    }

                    ret = pager.doTouchEvent(e);
                    return ret;
                }
                return false;
            }
        });
    }

    private void initCanvas(){
        mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        mRightPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        mLeftPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);

        mCurPageCanvas = new Canvas(mCurPageBitmap);
        mRightPageCanvas = new Canvas(mRightPageBitmap);
        mLeftPageCanvas = new Canvas(mLeftPageBitmap);
    }

    private void loadImage(final Canvas canvas, int index) {
        Bitmap bitmap = getBitmap(pages[index]);
        pagerFactory.onDraw(canvas, bitmap);
    }

    private Bitmap getBitmap(String name) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        ApplicationInfo appInfo = getApplicationInfo();
        int resID = getResources().getIdentifier(name, "drawable", appInfo.packageName);
        InputStream is = getResources().openRawResource(resID);
        Bitmap tempBitmap = BitmapFactory.decodeStream(is, null, opt);
        int width = tempBitmap.getWidth();
        int height = tempBitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(((float)screenWidth)/width, ((float)screenHeight)/height);
        Bitmap bitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height, matrix, true);
        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void manageLayer(View v, boolean enableHardware) {
        int layerType = enableHardware ? View.LAYER_TYPE_HARDWARE
                : View.LAYER_TYPE_NONE;
        if (layerType != v.getLayerType())
            v.setLayerType(layerType, null);
    }
}
