package com.example.concerto.onboarding;

// ==========================================
// OnboardingPage.java
// ==========================================
// package com.example.concerto.onboarding;

public class OnboardingPage {
    public final String title;
    public final String subtext;
    public final int iconRes;
    public final boolean showActionButton;

    public OnboardingPage(String title, String subtext, int iconRes, boolean showActionButton) {
        this.title = title;
        this.subtext = subtext;
        this.iconRes = iconRes;
        this.showActionButton = showActionButton;
    }
}
