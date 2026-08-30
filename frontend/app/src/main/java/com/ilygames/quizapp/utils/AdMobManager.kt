package com.ilygames.quizapp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {

    // GOOGLE OFFICIAL TEST REWARDED AD UNIT ID
    const val REWARDED_TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // YOUR LIVE ADMOB REWARDED AD UNIT ID
    const val REWARDED_LIVE_AD_UNIT_ID = "ca-app-pub-9896007608885239/8625133425"

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { status ->
            isInitialized = true
            Log.d("AdMobManager", "AdMob initialized: ${status.adapterStatusMap}")
        }
    }

    private fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Loads and displays a Google AdMob Rewarded Video Ad.
     */
    fun showRewardedAd(
        context: Context,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            Log.e("AdMobManager", "Could not find Activity from context!")
            Toast.makeText(context, "Unable to launch ad screen.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(activity, "Loading Video Ad...", Toast.LENGTH_SHORT).show()
        // Load live unit ID first, fallback to test unit ID if live is warming up
        loadAdWithUnitId(activity, REWARDED_LIVE_AD_UNIT_ID, isFallback = false, onRewardEarned, onAdClosed)
    }

    private fun loadAdWithUnitId(
        activity: Activity,
        adUnitId: String,
        isFallback: Boolean,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d("AdMobManager", "Rewarded Ad Loaded! (Unit: $adUnitId, isFallback=$isFallback)")

                    var hasEarnedReward = false

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d("AdMobManager", "Ad displayed full screen.")
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdMobManager", "Ad failed to show: ${adError.message}")
                            if (!isFallback) {
                                loadAdWithUnitId(activity, REWARDED_TEST_AD_UNIT_ID, isFallback = true, onRewardEarned, onAdClosed)
                            } else {
                                Toast.makeText(activity, "Ad playback failed.", Toast.LENGTH_SHORT).show()
                                onAdClosed()
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d("AdMobManager", "Ad dismissed by user.")
                            if (hasEarnedReward) {
                                onRewardEarned()
                            } else {
                                Toast.makeText(activity, "Watch full ad to get a heart!", Toast.LENGTH_SHORT).show()
                                onAdClosed()
                            }
                        }
                    }

                    rewardedAd.show(activity) { rewardItem: RewardItem ->
                        Log.d("AdMobManager", "User completed video! Reward: ${rewardItem.amount}")
                        hasEarnedReward = true
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobManager", "Ad Failed to Load ($adUnitId): ${loadAdError.message} (code=${loadAdError.code})")
                    if (!isFallback) {
                        Log.d("AdMobManager", "Live ad warming up. Trying official Test Video Ad...")
                        loadAdWithUnitId(activity, REWARDED_TEST_AD_UNIT_ID, isFallback = true, onRewardEarned, onAdClosed)
                    } else {
                        Toast.makeText(activity, "Ad unavailable (${loadAdError.code}). Try again later.", Toast.LENGTH_SHORT).show()
                        onAdClosed()
                    }
                }
            }
        )
    }
}
