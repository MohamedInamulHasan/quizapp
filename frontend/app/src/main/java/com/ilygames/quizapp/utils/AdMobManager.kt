package com.ilygames.quizapp.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {

    // LIVE ADMOB REWARDED AD UNIT ID
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-9896007608885239/8625133425"

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { status ->
            isInitialized = true
            Log.d("AdMobManager", "AdMob initialized successfully: ${status.adapterStatusMap}")
        }
    }

    /**
     * Loads and displays a Google AdMob Rewarded Video Ad
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        Toast.makeText(activity, "Loading Video Ad...", Toast.LENGTH_SHORT).show()
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d("AdMobManager", "Rewarded Ad Loaded!")
                    rewardedAd.show(activity) { rewardItem: RewardItem ->
                        Log.d("AdMobManager", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobManager", "Rewarded Ad Failed to Load: ${loadAdError.message}")
                    Toast.makeText(activity, "Ad failed to load. Please try again later.", Toast.LENGTH_SHORT).show()
                    onAdClosed()
                }
            }
        )
    }
}
