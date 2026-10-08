package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.SensorToken;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.ReceivedVars;

import java.util.regex.Matcher;

/**
 * Draws a {@code [source: key]} token as a pill (dot, source, bold value) while the underlying
 * text stays the raw token, so saving and code generation are unaffected.
 */
final class TokenChipSpan extends ReplacementSpan {

    final String token;

    private final String source;
    private final String value;
    private final int accent;
    private final int sourceColor;
    private final int valueColor;
    private final float maxWidth;

    private final float marginH;
    private final float padStart;
    private final float padEnd;
    private final float padV;
    private final float dotSize;
    private final float dotGap;
    private final float partsGap;
    private final float strokeWidth;

    private final TextPaint sourcePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private String shownSource;
    private float width;

    private TokenChipSpan(Context context, String token, String source, String value,
                          int accent, boolean known, float maxWidth) {
        this.token = token;
        this.source = source;
        this.value = value;
        this.accent = accent;
        this.maxWidth = maxWidth;
        this.sourceColor = known
                ? ContextCompat.getColor(context, R.color.text_secondary_light) : accent;
        this.valueColor = known
                ? ContextCompat.getColor(context, R.color.text_primary_light) : accent;

        float d = context.getResources().getDisplayMetrics().density;
        marginH = 2 * d;
        padStart = 8 * d;
        padEnd = 10 * d;
        padV = 3.5f * d;
        dotSize = 6 * d;
        dotGap = 6 * d;
        partsGap = 4 * d;
        strokeWidth = 1 * d;

        sourcePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        valuePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    }

    static TokenChipSpan of(Context context, Matcher match, float maxWidth) {
        String token = match.group();
        String sourceName = match.group(1);
        String key = match.group(2);

        if (ReceivedVars.isSource(sourceName)) {
            int index = ReceivedVars.indexOf(key);
            if (index >= 0) {
                return new TokenChipSpan(context, token, "Received", "var " + (index + 1),
                        accentFor(context, sourceName), true, maxWidth);
            }
        } else {
            SensorToken.Resolved resolved = SensorToken.resolve(match);
            if (resolved != null) {
                return new TokenChipSpan(context, token, resolved.sensor.displayName,
                        resolved.channel.displayName, accentFor(context, sourceName), true,
                        maxWidth);
            }
        }
        return new TokenChipSpan(context, token, sourceName, key,
                ContextCompat.getColor(context, R.color.accent_red), false, maxWidth);
    }

    /** Received data is green, sensors blue; the value buttons use the same colours. */
    static int accentFor(Context context, String sourceName) {
        return ContextCompat.getColor(context, ReceivedVars.isSource(sourceName)
                ? R.color.accent_green : R.color.accent_blue);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        preparePaints(paint);
        measure();

        if (fm != null) {
            Paint.FontMetricsInt base = paint.getFontMetricsInt();
            int center = (base.ascent + base.descent) / 2;
            int half = (int) Math.ceil(chipHeight() / 2f + strokeWidth);
            fm.ascent = Math.min(base.ascent, center - half);
            fm.descent = Math.max(base.descent, center + half);
            fm.top = Math.min(base.top, fm.ascent);
            fm.bottom = Math.max(base.bottom, fm.descent);
            fm.leading = base.leading;
        }
        return Math.round(width);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        preparePaints(paint);
        if (shownSource == null) measure();

        Paint.FontMetrics base = paint.getFontMetrics();
        float center = y + (base.ascent + base.descent) / 2f;
        float half = chipHeight() / 2f;
        rect.set(x + marginH, center - half, x + width - marginH, center + half);

        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(ColorUtils.setAlphaComponent(accent, 0x26));
        canvas.drawRoundRect(rect, half, half, shapePaint);

        shapePaint.setStyle(Paint.Style.STROKE);
        shapePaint.setStrokeWidth(strokeWidth);
        shapePaint.setColor(ColorUtils.setAlphaComponent(accent, 0x8C));
        float inset = strokeWidth / 2f;
        rect.inset(inset, inset);
        canvas.drawRoundRect(rect, half - inset, half - inset, shapePaint);

        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(accent);
        float dotX = x + marginH + padStart + dotSize / 2f;
        canvas.drawCircle(dotX, center, dotSize / 2f, shapePaint);

        Paint.FontMetrics chip = sourcePaint.getFontMetrics();
        float baseline = center - (chip.ascent + chip.descent) / 2f;
        float textX = dotX + dotSize / 2f + dotGap;

        sourcePaint.setColor(sourceColor);
        canvas.drawText(shownSource, textX, baseline, sourcePaint);

        textX += sourcePaint.measureText(shownSource) + partsGap;
        valuePaint.setColor(valueColor);
        canvas.drawText(value, textX, baseline, valuePaint);
    }

    private void preparePaints(Paint base) {
        float size = base.getTextSize() * 0.86f;
        sourcePaint.setTextSize(size);
        valuePaint.setTextSize(size);
    }

    private void measure() {
        float fixed = 2 * marginH + padStart + dotSize + dotGap + partsGap + padEnd
                + valuePaint.measureText(value);
        float sourceRoom = sourcePaint.measureText(source);

        if (maxWidth > 0 && fixed + sourceRoom > maxWidth) {
            sourceRoom = Math.max(0, maxWidth - fixed);
            shownSource = TextUtils.ellipsize(source, sourcePaint, sourceRoom,
                    TextUtils.TruncateAt.END).toString();
        } else {
            shownSource = source;
        }
        width = fixed + sourcePaint.measureText(shownSource);
    }

    private float chipHeight() {
        Paint.FontMetrics chip = sourcePaint.getFontMetrics();
        return (chip.descent - chip.ascent) + 2 * padV;
    }
}
