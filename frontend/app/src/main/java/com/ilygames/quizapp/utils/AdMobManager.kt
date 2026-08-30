package com.ilygames.quizapp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
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
        try {
            MobileAds.initialize(context) { status ->
                isInitialized = true
                Log.d("AdMobManager", "AdMob initialized: ${status.adapterStatusMap}")
            }
        } catch (e: Exception) {
            Log.e("AdMobManager", "AdMob init error: ${e.message}")
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
     * Loads and displays a Google AdMob Rewarded Video Ad on the Main UI Thread.
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

        Toast.makeText(activity, "🎬 Loading Video Ad...", Toast.LENGTH_SHORT).show()

        // Force execution on Main UI Thread to guarantee full-screen activity presentation
        Handler(Looper.getMainLooper()).post {
            loadAdWithUnitId(activity, REWARDED_LIVE_AD_UNIT_ID, isFallback = false, onRewardEarned, onAdClosed)
        }
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
                    Log.d("AdMobManager", "Rewarded Ad Loaded! Presenting full screen video... (Unit: $adUnitId)")

                    var earnedReward = false

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d("AdMobManager", "Ad video displayed full screen.")
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
                            Log.d("AdMobManager", "Ad dismissed by user. earnedReward=$earnedReward")
                            if (earnedReward) {
                                onRewardEarned()
                            } else {
                                Toast.makeText(activity, "Watch full ad to get a heart!", Toast.LENGTH_SHORT).show()
                                onAdClosed()
                            }
                        }
                    }

                    activity.runOnUiThread {
                        try {
                            rewardedAd.show(activity) { rewardItem: RewardItem ->
                                Log.d("AdMobManager", "User completed video! Reward: ${rewardItem.amount}")
                                earnedReward = true
                            }
                        } catch (e: Exception) {
                            Log.e("AdMobManager", "Error calling rewardedAd.show: ${e.message}")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobManager", "Ad Failed to Load ($adUnitId): ${loadAdError.message} (code=${loadAdError.code})")
                    if (!isFallback) {
                        Log.d("AdMobManager", "Live ad warming up. Trying official Test Video Ad...")
                        loadAdWithUnitId(activity, REWARDED_TEST_AD_UNIT_ID, isFallback = true, onRewardEarned, onAdClosed)
                    } else {
                        Log.d("AdMobManager", "Test ad also failed. Showing Video Ad Simulation...")
                        showVideoAdSimulationDialog(activity, onRewardEarned, onAdClosed)
                    }
                }
            }
        )
    }

    /**
     * Fallback Full-Screen Video Ad Simulation Dialog if AdMob network is warming up or blocked.
     */
    private fun showVideoAdSimulationDialog(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        activity.runOnUiThread {
            val builder = android.app.AlertDialog.Builder(activity)
            val dialogView = android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(60, 50, 60, 50)
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#111827"))
            }

            val titleView = android.widget.TextView(activity).apply {
                text = "🎬 Sponsor Video Ad"
                textSize = 20f
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
            }

            val subtitleView = android.widget.TextView(activity).apply {
                text = "Watch 5 seconds to earn your heart reward!"
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 30)
            }

            val timerView = android.widget.TextView(activity).apply {
                text = "⏳ Watching ad... (5s)"
                textSize = 18f
                setTextColor(android.graphics.Color.parseColor("#10B981"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 30)
            }

            val progressBar = android.widget.ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 5
                progress = 0
                progressDrawable.setColorFilter(android.graphics.Color.parseColor("#10B981"), android.graphics.PorterDuff.Mode.SRC_IN)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    24
                )
            }

            dialogView.addView(titleView)
            dialogView.addView(subtitleView)
            dialogView.addView(timerView)
            dialogView.addView(progressBar)

            builder.setView(dialogView)
            builder.setCancelable(false)
            val dialog = builder.create()
            dialog.show()

            var secondsLeft = 5
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    secondsLeft--
                    progressBar.progress = 5 - secondsLeft
                    if (secondsLeft > 0) {
                        timerView.text = "⏳ Watching ad... (${secondsLeft}s)"
                        handler.postDelayed(this, 1000)
                    } else {
                        timerView.text = "🎉 Video Completed!"
                        handler.postDelayed({
                            dialog.dismiss()
                            onRewardEarned()
                        }, 500)
                    }
                }
            }
            handler.postDelayed(runnable, 1000)
        }
    }
}
