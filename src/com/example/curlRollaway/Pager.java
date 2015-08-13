package com.example.curlRollaway;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

public class Pager extends View {
    private int mWidth = 1280;
    private int mHeight = 768;
    private float mRadius = 0;
    private List<ImagePiece> curImagePieces;
    private boolean isSplitCurImage = true;
    private List<ImagePiece> nextImagePieces;
    private boolean isSplitNextImage = true;
    private int mRows = 4;
    private int mColumns = 6;
    private boolean isSetCoord = false;
    private float[] oldCoordX;
    private float[] oldCoordY;
    private float[] coordX;
    private float[] coordY;
    private float deltaDistance;
    private int mRotate = 180;
    //拖拽点
    PointF mTouch = new PointF();
    //第一次拖拽的点
    PointF mFirstTouch = new PointF();

    Bitmap mCurPageBitmap = null;
    Bitmap mNextPageBitmap = null;

    Scroller mScroller;
    public Pager(Context context, int screenWidth, int screenHeight) {
        super(context);
        this.mWidth = screenWidth;
        this.mHeight = screenHeight;
        mScroller = new Scroller(getContext());
    }

    /**
     * bitmap单位
     */
    private class ImagePiece {
        public int index = 0;
        public Bitmap bitmap = null;
    }

    /**
     * 切割bitmap
     *
     * @param bitmap
     * @param row
     * @param column
     * @return
     */
    private List<ImagePiece> split(Bitmap bitmap, int row, int column) {

        List<ImagePiece> pieces = new ArrayList<ImagePiece>(row * column);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int pieceWidth = width / column;
        int pieceHeight = height / row;

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                ImagePiece imagePiece = new ImagePiece();
                imagePiece.index = j + i * column;

                int xValue = j * pieceWidth;
                int yValue = i * pieceHeight;

                imagePiece.bitmap = Bitmap.createBitmap(bitmap, xValue, yValue,
                        pieceWidth, pieceHeight);
                pieces.add(imagePiece);
            }
        }
        return pieces;
    }

    /**
     * 计算bitmap现在位于的坐标以及将要位于的坐标
     */
    private void setCoordinate() {
        oldCoordX = new float[mRows * mColumns];
        oldCoordY = new float[mRows * mColumns];
        coordX = new float[mRows * mColumns];
        coordY = new float[mRows * mColumns];

        /**
         * 计算bitmap将要位于的坐标
         */
        double angle = 360.0f / mRows / mColumns * Math.PI / 180;
        float centerX = getMeasuredWidth() / 2.0f - getMeasuredWidth() / mColumns / 2.0f;
        float centerY = (getMeasuredHeight() - getMeasuredHeight() / mRows) / 2.0f;
        float radius = (getMeasuredHeight() - getMeasuredHeight() / mRows) / 2.0f;
        mRadius = radius;
        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < mColumns; j++) {
                /**
                 * x1 = dx * cos(a) + dy * sin(a);
                 * y1 = dy * cos(a) - dx * sin(a);
                 */
                double diffAngel = (i * mColumns + j) * angle;
                coordX[i * mColumns + j] = (float) (radius * Math.sin(diffAngel) + centerX);
                coordY[i * mColumns + j] = (float) (centerY - radius * Math.cos(diffAngel));
            }
        }

        /**
         * 计算bitmap现在所处的坐标
         */
        if (curImagePieces != null && curImagePieces.size() > 0) {
            for (int i = 0; i < mRows; i++) {
                for (int j = 0; j < mColumns; j++) {
                    oldCoordX[i * mColumns + j] = getPieceBitmapX(i, j);
                    oldCoordY[i * mColumns + j] = getPieceBitmapY(i, j);
                }
            }
        }
    }

    private float getPieceBitmapX(int row, int col) {
        float x = 0.0f;
        for (int i = 0; i < col; i++) {
            x += curImagePieces.get(row * mColumns + i).bitmap.getWidth();
        }
        return x;
    }

    private float getPieceBitmapY(int row, int col) {
        float y = 0.0f;
        for (int i = 0; i < row; i++) {
            y += curImagePieces.get(i * mColumns + col).bitmap.getHeight();
        }
        return y;
    }

    private void drawCurPageArea(Canvas canvas, Bitmap bitmap) {
        /**
         * 将图片切成mColumn*mColumn份
         */
        if (isSplitCurImage) {
            isSplitCurImage = false;
            curImagePieces = split(bitmap, mRows, mColumns);
            if(!isSetCoord){
                setCoordinate();
                isSetCoord = true;
            }

        }
        float transformRatio = Math.abs(deltaDistance) / (mWidth / 4);
        if (transformRatio <= 1.0f) {
            for (int i = 0; i < mRows * mColumns; i++) {
                float x = (coordX[i] - oldCoordX[i]) * transformRatio + oldCoordX[i] + mWidth / 2 * (deltaDistance < 0? -1: 1) * transformRatio;
                float y = (coordY[i] - oldCoordY[i]) * transformRatio + oldCoordY[i];

                canvas.save();
                float rotateAngle = 360 / (mRows * mColumns) * i * transformRatio;
                Matrix matrix = new Matrix();
                matrix.postTranslate(x, y);
                matrix.postRotate(rotateAngle, x + curImagePieces.get(i).bitmap.getWidth() / 2, y + curImagePieces.get(i).bitmap.getHeight() / 2);
                canvas.drawBitmap(curImagePieces.get(i).bitmap, matrix, null);
                canvas.restore();
            }
        }else{
            transformRatio = (Math.abs(deltaDistance) - mWidth * 1 / 4)/ (mWidth * 1 / 4);
            transformRatio = transformRatio > 1.0f?1.0f:transformRatio;
            if(deltaDistance < 0){
                transformRatio = -transformRatio;
            }
            for (int i = 0; i < mRows * mColumns; i++) {
                float x = coordX[i] + mWidth / 2 * (deltaDistance < 0? -1: 1) + mWidth / 2 * transformRatio;
                float y = coordY[i];

                canvas.save();
                float rotateAngle = 360 / (mRows * mColumns) * i;
                Matrix matrix = new Matrix();
                matrix.postTranslate(x, y);
                matrix.postRotate(rotateAngle, x + curImagePieces.get(i).bitmap.getWidth() / 2, y + curImagePieces.get(i).bitmap.getHeight() / 2);
                matrix.postRotate(transformRatio * mRotate, (deltaDistance < 0? 0: mWidth) + mWidth / 2 * transformRatio, mHeight / 2);
                canvas.drawBitmap(curImagePieces.get(i).bitmap, matrix, null);
                canvas.restore();
            }
        }
    }

    private void drawNextPageArea(Canvas canvas, Bitmap bitmap) {
        /**
         * 将图片切成mColumn*mColumn份
         */
        if (isSplitNextImage) {
            isSplitNextImage = false;
            nextImagePieces = split(bitmap, mRows, mColumns);
        }
        float distance = mWidth / 2 - mRadius;
        float transformRatio = Math.abs(deltaDistance) / (mWidth / 8);
        float startX = 0;
        if(deltaDistance < 0){
            startX = mWidth;
        }else{
            startX = -mWidth;
        }
        if (transformRatio <= 1.0f) {
            for (int i = 0; i < mRows * mColumns; i++) {
                float x = (coordX[i] - oldCoordX[i]) * transformRatio + oldCoordX[i] + distance * (deltaDistance < 0? -1: 1) * transformRatio + startX;
                float y = (coordY[i] - oldCoordY[i]) * transformRatio + oldCoordY[i];

                canvas.save();
                float rotateAngle = 360 / (mRows * mColumns) * i * transformRatio;
                Matrix matrix = new Matrix();
                matrix.postTranslate(x, y);
                matrix.postRotate(rotateAngle, x + nextImagePieces.get(i).bitmap.getWidth() / 2, y + nextImagePieces.get(i).bitmap.getHeight() / 2);
                canvas.drawBitmap(nextImagePieces.get(i).bitmap, matrix, null);
                canvas.restore();
            }
        }else if(transformRatio > 1.0f && transformRatio <= 2.0f){
            transformRatio = (Math.abs(deltaDistance) - mWidth * 1 / 8)/ (mWidth * 1 / 8);
            if(deltaDistance < 0){
                transformRatio = -transformRatio;
            }
            for (int i = 0; i < mRows * mColumns; i++) {
                float x = coordX[i] + distance * (deltaDistance < 0?-1: 1) + (mWidth / 2 - distance) * transformRatio + startX;
                float y = coordY[i];

                canvas.save();
                float rotateAngle = 360 / (mRows * mColumns) * i;
                Matrix matrix = new Matrix();
                matrix.postTranslate(x, y);
                matrix.postRotate(rotateAngle, x + nextImagePieces.get(i).bitmap.getWidth() / 2, y + nextImagePieces.get(i).bitmap.getHeight() / 2);
                matrix.postRotate(360 - mRotate, mWidth / 2 + distance * (deltaDistance < 0?-1: 1) + (mWidth / 2 - distance) * transformRatio + startX, mHeight / 2);
                matrix.postRotate(transformRatio * mRotate, mWidth / 2 + distance * (deltaDistance < 0?-1: 1) + (mWidth / 2 - distance) * transformRatio + startX, mHeight / 2);
                canvas.drawBitmap(nextImagePieces.get(i).bitmap, matrix, null);
                canvas.restore();
            }
        }else{
            transformRatio = (Math.abs(deltaDistance) - mWidth * 1 / 4)/ (mWidth * 1 / 4);
            transformRatio = transformRatio > 1.0f?1.0f:transformRatio;
            transformRatio = 1 - transformRatio;
            for (int i = 0; i < mRows * mColumns; i++) {
                float x = (coordX[i] - oldCoordX[i]) * transformRatio + oldCoordX[i] + mWidth / 2 * (deltaDistance > 0? -1: 1) * transformRatio;
                float y = (coordY[i] - oldCoordY[i]) * transformRatio + oldCoordY[i];

                canvas.save();
                float rotateAngle = 360 / (mRows * mColumns) * i * transformRatio;
                Matrix matrix = new Matrix();
                matrix.preTranslate(x, y);
                matrix.postRotate(rotateAngle, x + nextImagePieces.get(i).bitmap.getWidth() / 2, y + nextImagePieces.get(i).bitmap.getHeight() / 2);
                canvas.drawBitmap(nextImagePieces.get(i).bitmap, matrix, null);
                canvas.restore();
            }

            if(transformRatio == 0.0f){
                mScroller.abortAnimation();
            }
        }
    }

    public void setBitmaps(Bitmap bm1, Bitmap bm2) {
        mCurPageBitmap = bm1;
        mNextPageBitmap = bm2;
        isSplitCurImage = true;
        isSplitNextImage = true;
        this.postInvalidate();
    }

    public boolean doTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mTouch.x = (float) Math.ceil(event.getX());
            mTouch.y = (float) Math.ceil(event.getY());
            this.postInvalidate();
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mFirstTouch.x = (float) Math.ceil(event.getX());
            mFirstTouch.y = (float) Math.ceil(event.getY());
            mTouch.x = (float) Math.ceil(event.getX());
            mTouch.y = (float) Math.ceil(event.getY());
            deltaDistance = 0.0f;
            this.postInvalidate();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (canDragOver()) {
                startAnimation(2000);
            }else{
                startAnimation(1500);
            }
            this.postInvalidate();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFAAAAAA);
        deltaDistance = mTouch.x - mFirstTouch.x;
        if(deltaDistance < 0.0f){
            if(deltaDistance > -2.0f) {
                deltaDistance = 0.0f;
            }
        }else{
            if(deltaDistance < 2.0f) {
                deltaDistance = 0.0f;
            }
        }
        drawCurPageArea(canvas, mCurPageBitmap);
        if(mNextPageBitmap != null){
            drawNextPageArea(canvas, mNextPageBitmap);
        }
    }

    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            float x = mScroller.getCurrX();
            float y = mScroller.getCurrY();
            mTouch.x = x;
            mTouch.y = y;
            postInvalidate();
        }
    }

    private void startAnimation(int delayMillis) {
        int dx;
        float tmpDeltaDistance = mTouch.x - mFirstTouch.x;
        if (tmpDeltaDistance < 0) {
            if(Math.abs(tmpDeltaDistance) > mWidth / 4){
                dx = -(int) (mWidth/2 + tmpDeltaDistance + 10);
            }else{
                dx = (int) Math.abs(tmpDeltaDistance);
            }
        } else {
            if(tmpDeltaDistance > mWidth / 4){
                dx = (int) (mWidth/2 - tmpDeltaDistance + 10);
            }else{
                dx = (int) -tmpDeltaDistance;
            }
        }
        mScroller.startScroll((int) mTouch.x, 0, dx, 0, delayMillis);
    }

    public void abortAnimation() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    public boolean isAnimationRunning(){
        return !mScroller.isFinished();
    }

    public boolean canDragOver() {
        if (Math.abs(deltaDistance) >= mWidth / 4)
            return true;
        return false;
    }
}
