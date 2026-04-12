package com.example.concerto.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.concerto.R;

import java.util.List;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {

    private final List<OnboardingPage> pages;
    private final Runnable onConnectSpotifyClick;

    public OnboardingPagerAdapter(List<OnboardingPage> pages, Runnable onConnectSpotifyClick) {
        this.pages = pages;
        this.onConnectSpotifyClick = onConnectSpotifyClick;
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
        holder.tvTitle.setText(page.title);
        holder.tvSubtext.setText(page.subtext);
        holder.ivIllustration.setImageResource(page.iconRes);

        if (page.showActionButton) {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (onConnectSpotifyClick != null) onConnectSpotifyClick.run();
            });
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIllustration;
        TextView tvTitle;
        TextView tvSubtext;
        Button btnAction;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIllustration = itemView.findViewById(R.id.ivOnboardingIllustration);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvSubtext = itemView.findViewById(R.id.tvOnboardingSubtext);
            btnAction = itemView.findViewById(R.id.btnOnboardingAction);
        }
    }
}
