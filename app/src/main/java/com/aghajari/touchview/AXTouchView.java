package com.aghajari.touchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AXTouchView extends View {

    private PointF nextPoint;
    private final ArrayList<PointF> points = new ArrayList<>();
    private final float[] point = new float[2];
    private boolean isTouching = false;

    private int hoverColor = 0xFFDDDDDD,
            touchingHoverColor = Color.LTGRAY,
            touchedColor = Color.BLUE;
    private final Paint paint = new Paint();
    private final Paint helperPaint;
    private boolean helperEnabled = true;

    private float step = 0.01f;
    private float progress;
    private Path path, orgPath;
    private PathMeasure pathMeasure;
    private boolean toCenter;

    private OnTouchViewListener onTouchViewListener;
    private List<Pair<Float, Float>> helpers;

    private ValueAnimator animator = null;
    private float animationProgress = -1;

    public interface OnTouchViewListener {
        void onProgressChanged(AXTouchView touchView, float progress);

        void onStartTrackingTouch(AXTouchView touchView);

        void onStopTrackingTouch(AXTouchView touchView);
    }

    public AXTouchView(Context context) {
        this(context, null);
    }

    public AXTouchView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AXTouchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        helperPaint = new Paint(paint);
        paint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 40.0f);
        helperPaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 4.0f);
        helperPaint.setColor(Color.RED);
        setHelperArrows(0.1f, 0.1f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (path == null)
            return;

        if (animationProgress >= 0 && animationProgress <= 1.0f) {
            paint.setColor(hoverColor);
            canvas.drawPath(getSubPath(0, animationProgress), paint);
            return;
        }

        paint.setColor(isTouching ? touchingHoverColor : hoverColor);
        canvas.drawPath(path, paint);

        if (helperEnabled) {
            int orgAlpha = -1;
            if (animationProgress > 1.0f) {
                orgAlpha = helperPaint.getAlpha();
                helperPaint.setAlpha((int) (orgAlpha * (animationProgress - 1.0f)));
            }

            float radius = 40;
            double angle = Math.toRadians(55);
            for (Pair<Float, Float> pair : helpers) {
                canvas.drawPath(getSubPath(pair.first, pair.second), helperPaint);
                PointF point = getPoint(pair.second);
                PointF secondPoint = getPoint(pair.second - step);

                double m = Math.atan2(point.y - secondPoint.y, point.x - secondPoint.x);

                canvas.drawLine(point.x, point.y, (float) (point.x - radius * Math.cos(m - (angle / 2.0))),
                        (float) (point.y - radius * Math.sin(m - (angle / 2.0))), helperPaint);
                canvas.drawLine(point.x, point.y, (float) (point.x - radius * Math.cos(m + (angle / 2.0))),
                        (float) (point.y - radius * Math.sin(m + (angle / 2.0))), helperPaint);
            }

            if (orgAlpha != -1)
                helperPaint.setAlpha(orgAlpha);
        }

        if (progress == 0)
            return;

        paint.setColor(touchedColor);
        canvas.drawPath(getSubPath(), paint);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isEnabled() || progress >= 1 || animationProgress != -1)
            return super.dispatchTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouching = canStartTouching(event)) {
                    if (onTouchViewListener != null)
                        onTouchViewListener.onStartTrackingTouch(this);
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                boolean old = isTouching;
                isTouching = false;
                if (old) {
                    if (onTouchViewListener != null)
                        onTouchViewListener.onStopTrackingTouch(this);
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isTouching) {
                    if (isTouchingNextPoint(event)) {
                        goToNext();
                        invalidate();
                    }
                    return true;
                }
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (toCenter) {
            fixPath();
            setProgress(progress);
        }
    }

    /**
     * Current point has touched, move to the next step
     */
    private void goToNext() {
        progress += step;
        points.add(nextPoint);
        nextPoint = getPoint(progress + step);

        progress = Math.min(Math.max(0, progress), 1);
        if (onTouchViewListener != null)
            onTouchViewListener.onProgressChanged(this, progress);
    }

    /**
     * @return true if (event.x, event.y) is in the circle
     * with center of the specified point and radius of strokeWidth/2
     */
    private boolean isTouching(PointF point, MotionEvent event) {
        return Math.sqrt(Math.pow(event.getX() - point.x, 2)
                + Math.pow(event.getY() - point.y, 2)) <= (paint.getStrokeWidth() / 2);
    }

    /**
     * @return true if (event.x, event.y) is in area of one of the touched points
     */
    private boolean canStartTouching(MotionEvent event) {
        for (int i = points.size() - 1; i >= 0; i--) {
            if (isTouching(points.get(i), event))
                return true;
        }
        return false;
    }

    /**
     * @see #isTouching(PointF, MotionEvent)
     */
    private boolean isTouchingNextPoint(MotionEvent event) {
        return isTouching(nextPoint, event);
    }

    /**
     * Computes the corresponding position with specified progress
     */
    private PointF getPoint(float v) {
        pathMeasure.getPosTan(pathMeasure.getLength() * Math.min(Math.max(0.0f, v), 1.0f),
                point, null);
        return new PointF(point[0], point[1]);
    }

    /**
     * Sets path and offsets it to the center of this view
     */
    public void setPath(Path path) {
        setPath(path, true);
    }


    /**
     * Sets path
     *
     * @param path     New path
     * @param toCenter Offsets the path to the center of this view
     */
    public void setPath(Path path, boolean toCenter) {
        if (path != orgPath)
            this.orgPath = new Path(path);
        this.toCenter = toCenter;
        fixPath();

        progress = 0.0f;
        points.clear();
        points.add(getPoint(0));
        nextPoint = getPoint(progress + step);
        invalidate();
    }

    /**
     * Animates path drawing
     *
     * @param animationDuration   Animates first drawing if duration is > 0
     * @param animationStartDelay Delays animation's starter
     * @param interpolator        Animation's interpolator
     */
    public void animate(int animationDuration,
                        int animationStartDelay,
                        @Nullable TimeInterpolator interpolator) {
        animationProgress = animationDuration > 0 ? 0 : -1;
        invalidate();

        if (animator != null && animator.isRunning())
            animator.cancel();

        if (animationDuration > 0) {
            animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setDuration(animationDuration);
            animator.setInterpolator(interpolator);
            animator.setStartDelay(animationStartDelay);
            animator.addUpdateListener(a -> {
                animationProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (isHelperArrowsEnabled()) {
                        animator = ValueAnimator.ofFloat(1.0f, 2.0f);
                        animator.setDuration(300);
                        animator.addUpdateListener(a -> {
                            animationProgress = (float) a.getAnimatedValue();
                            if (animationProgress > 1.0f && !isHelperArrowsEnabled()) {
                                animationProgress = -1;
                                a.cancel();
                            }
                            invalidate();
                        });
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                animationProgress = -1;
                                invalidate();
                            }
                        });
                        animator.start();
                    } else {
                        animationProgress = -1;
                        invalidate();
                    }
                }
            });
            animator.start();
        }
    }

    /**
     * Sets the current progress to the specified value.
     *
     * @param progress the new progress, between 0.0 and 1.0
     * @see #getProgress()
     */
    public void setProgress(float progress) {
        this.progress = Math.min(Math.max(0, progress), 1);
        points.clear();

        float position = 0.0f;

        do {
            points.add(getPoint(position));
            position += step;
        } while (progress > position);

        if (position != progress)
            points.add(getPoint(progress));

        nextPoint = getPoint(progress + step);
        invalidate();
    }

    /**
     * Sets path and offsets it to the center if needed
     */
    private void fixPath() {
        path = new Path(orgPath);
        if (toCenter) {
            RectF bound = new RectF();
            path.computeBounds(bound, true);
            System.out.println(bound);
            path.offset((getMeasuredWidth() - bound.right) / 2,
                    (getMeasuredHeight() - bound.bottom) / 2f);
        }
        pathMeasure = new PathMeasure(this.path, false);
    }

    /**
     * @return the touched segment of the path
     */
    public Path getSubPath() {
        return getSubPath(0, progress);
    }

    private Path getSubPath(float start, float end) {
        Path sub = new Path();
        pathMeasure.getSegment(start * pathMeasure.getLength(), end * pathMeasure.getLength(), sub, true);
        return sub;
    }

    /**
     * @return the total length of the current path.
     */
    public float getPathLength() {
        return pathMeasure.getLength();
    }

    /**
     * @return path
     */
    public Path getPath() {
        return new Path(orgPath);
    }

    /**
     * Sets a listener to receive notifications of changes to the AXTouchView's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the AXTouchView.
     *
     * @param l The progress notification listener
     */
    public void setOnTouchViewListener(OnTouchViewListener l) {
        this.onTouchViewListener = l;
    }

    /**
     * @return true if the path is touching currently, false otherwise
     */
    public boolean isTouching() {
        return isTouching;
    }

    /**
     * @return step size, between 0.0 and 1.0
     */
    public float getStep() {
        return step;
    }

    /**
     * Sets the step size of this view, 0.01 by default
     *
     * @param step step size, between 0.0 and 1.0
     */
    public void setStep(float step) {
        if (step < 0 || step >= 1)
            throw new IllegalArgumentException("the step must be between 0.0 and 1.0");

        this.step = step;
    }

    /**
     * @return the current progress, between 0.0 and 1.0
     */
    public float getProgress() {
        return progress;
    }

    /**
     * @return the hover color
     */
    public int getHoverColor() {
        return hoverColor;
    }

    /**
     * Sets this view's hover color
     */
    public void setHoverColor(int hoverColor) {
        this.hoverColor = hoverColor;
    }

    /**
     * @return the hover color while this view is on touching mode
     */
    public int getTouchingHoverColor() {
        return touchingHoverColor;
    }

    /**
     * Sets this view's hover color while it's on touching mode
     */
    public void setTouchingHoverColor(int touchingHoverColor) {
        this.touchingHoverColor = touchingHoverColor;
    }

    /**
     * @return the touched segment color
     */
    public int getTouchedColor() {
        return touchedColor;
    }

    /**
     * Sets the touched segment color
     */
    public void setTouchedColor(int touchedColor) {
        this.touchedColor = touchedColor;
    }

    /**
     * Sets the width for stroking.
     */
    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
        invalidate();
    }

    /**
     * Sets helper arrows positions
     *
     * @param helpers List of floating pairs,
     *                the values must be between 0.0 and 1.0
     */
    public void setHelperArrows(@NonNull List<Pair<Float, Float>> helpers) {
        this.helpers = helpers;
    }

    /**
     * Sets helper arrows positions
     *
     * @param len the length of each arrow, between 0.0 and 1.0
     * @param gap the gap between two arrow, between 0.0 and 1.0
     */
    public void setHelperArrows(float len, float gap) {
        float start = 0;
        helpers = new ArrayList<>();
        do {
            helpers.add(Pair.create(start, Math.min(start + len, 1)));
            start += len + gap;
        } while (start < 1);
    }

    /**
     * Sets helper arrows color
     */
    public void setHelperArrowsColor(int color) {
        helperPaint.setColor(color);
        invalidate();
    }

    /**
     * Sets the width for helper arrows stroking.
     */
    public void setHelperArrowsStrokeWidth(float width) {
        helperPaint.setStrokeWidth(width);
        invalidate();
    }

    /**
     * @return True if hlper arrows are enabled, false otherwise.
     */
    public boolean isHelperArrowsEnabled() {
        return helperEnabled;
    }

    /**
     * Set the enabled state of helper arrows
     *
     * @param enabled True if helper arrows are enabled, false otherwise.
     */
    public void setHelperArrowsEnabled(boolean enabled) {
        this.helperEnabled = enabled;
        invalidate();
    }

    /**
     * Resets path
     */
    public void reset() {
        if (path == null)
            return;

        setPath(orgPath, toCenter);
    }

    /**
     * @return current touch position
     */
    public PointF getCurrentPosition() {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }

    public Paint getPaint() {
        return paint;
    }

    public Paint getHelperArrowsPaint() {
        return helperPaint;
    }

}
