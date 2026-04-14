package com.example.concerto.onboarding;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.concerto.R;

import java.util.List;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {

    private final List<OnboardingPage> pages;

    public OnboardingPagerAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);

        // Subtext & Icon
        holder.tvSubtext.setText(page.subtext);
        holder.ivIllustration.setImageResource(page.iconRes);

        // Title Gradient Parser (Uses #7C72E0 to #EFDEF5)
        setGradientTitle(holder.tvTitle, page.title);
    }

    private void setGradientTitle(TextView tv, String text) {
        if (text == null) return;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] parts = text.split("\\*");

        for (int i = 0; i < parts.length; i++) {
            int start = builder.length();
            builder.append(parts[i]);

            // Words wrapped in asterisks get the gradient
            if (i % 2 == 1) {
                // CAPTURE THE VARIABLE HERE:
                final String gradientWord = parts[i];

                builder.setSpan(new CharacterStyle() {
                    @Override
                    public void updateDrawState(TextPaint tp) {
                        // USE gradientWord INSTEAD OF parts[i]
                        Shader shader = new LinearGradient(0, 0, tp.measureText(gradientWord), tp.getTextSize(),
                                new int[]{Color.parseColor("#7C72E0"), Color.parseColor("#EFDEF5")},
                                null, Shader.TileMode.CLAMP);
                        tp.setShader(shader);
                    }
                }, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tv.setText(builder);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIllustration;
        TextView tvTitle;
        TextView tvSubtext;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIllustration = itemView.findViewById(R.id.ivOnboardingIllustration);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvSubtext = itemView.findViewById(R.id.tvOnboardingSubtext);
        }
    }
}