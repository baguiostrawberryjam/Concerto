package com.example.concerto.onboarding;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.concerto.R;
import com.example.concerto.auth.ConnectSpotifyFragment;
import com.example.concerto.dashboard.DashboardFragment;
import com.example.concerto.databinding.FragmentOnboardingBinding;

import java.util.ArrayList;
import java.util.List;

public class OnboardingFragment extends Fragment {

    private FragmentOnboardingBinding bind;
    private OnboardingPagerAdapter pagerAdapter;
    private List<OnboardingPage> pages;
    private ImageView[] dotViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bind = FragmentOnboardingBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buildPages();
        setupViewPager();
        setupDots();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void buildPages() {
        pages = new ArrayList<>();

        pages.add(new OnboardingPage(
                "View *Spotify* Tracks!",
                "Discover millions of tracks from Spotify's full catalogue.",
                R.drawable.img_onboarding_1,
                false
        ));

        pages.add(new OnboardingPage(
                "Play *Music*",
                "Stream full tracks instantly with Spotify Premium.",
                R.drawable.img_onboarding_2,
                false
        ));

        pages.add(new OnboardingPage(
                "*Personalized* Playlist Creation",
                "Curate your soundtrack and craft personalised joint playlists with Concerto.",
                R.drawable.img_onboarding_3,
                false
        ));

        pages.add(new OnboardingPage(
                "*Connect* to Spotify!",
                "Link your Spotify Premium account to unlock full playback and hosting.",
                R.drawable.img_onboarding_4,
                true
        ));
    }

    private void setupViewPager() {
        pagerAdapter = new OnboardingPagerAdapter(pages);
        bind.viewPagerOnboarding.setAdapter(pagerAdapter);
        bind.viewPagerOnboarding.setOffscreenPageLimit(pages.size());

        bind.viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                updateButtons(position);
            }
        });
    }

    private void setupDots() {
        dotViews = new ImageView[pages.size()];
        int dotSizePx = (int) (8 * getResources().getDisplayMetrics().density);
        int dotMarginPx = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < pages.size(); i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
            params.setMargins(dotMarginPx, 0, dotMarginPx, 0);
            dot.setLayoutParams(params);

            // Set tint to primary color if active
            dot.setImageResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            if (i == 0) {
                dot.setColorFilter(Color.parseColor("#7C72E0"));
            }

            bind.llDots.addView(dot);
            dotViews[i] = dot;
        }
    }

    private void updateDots(int activeIndex) {
        for (int i = 0; i < dotViews.length; i++) {
            dotViews[i].setImageResource(i == activeIndex ? R.drawable.dot_active : R.drawable.dot_inactive);
            if (i == activeIndex) {
                dotViews[i].setColorFilter(Color.parseColor("#7C72E0"));
            } else {
                dotViews[i].clearColorFilter();
            }
        }
    }

    private void setupButtons() {
        bind.btnSkip.setOnClickListener(v -> navigateToDashboard());

        bind.btnNext.setOnClickListener(v -> {
            int current = bind.viewPagerOnboarding.getCurrentItem();
            if (current < pages.size() - 1) {
                bind.viewPagerOnboarding.setCurrentItem(current + 1, true);
            } else {
                // On last page, it connects to Spotify
                navigateToConnectSpotify();
            }
        });
    }

    private void updateButtons(int position) {
        if (bind == null) return;
        boolean isLastPage = (position == pages.size() - 1);
        bind.btnNext.setText(isLastPage ? "Connect to Spotify" : "Next");
    }

    private void navigateToDashboard() {
        if (!isAdded() || getActivity() == null) return;
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.layoutFragmentContainer, new DashboardFragment())
                .commitAllowingStateLoss();
    }

    private void navigateToConnectSpotify() {
        if (!isAdded() || getActivity() == null) return;
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.layoutFragmentContainer, new ConnectSpotifyFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }
}