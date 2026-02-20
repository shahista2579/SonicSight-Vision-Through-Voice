package org.tensorflow.lite.examples.detection.currencygemniai;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlayView extends View {
    private String textToDraw;
    private Paint textPaint;

    // Constructor
    public CameraOverlayView(Context context) {
        super(context);
        init();
    }

    public CameraOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraOverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    // Initialize the paint for drawing text
    private void init() {
        textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF);  // White text color
        textPaint.setTextSize(100);      // Text size
        textPaint.setAntiAlias(true);    // Smooth text
    }

    // Set the text you want to draw
    public void setTextToDraw(String text) {
        this.textToDraw = text;
        invalidate();  // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw text at the specified position (center of the screen in this case)
        if (textToDraw != null) {
            canvas.drawText(textToDraw, 50, 50, textPaint);  // Draw text at (50, 50)
        }
    }
}
