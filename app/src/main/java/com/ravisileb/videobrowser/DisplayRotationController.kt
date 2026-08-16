package com.ravisileb.videobrowser

import android.content.pm.ActivityInfo

class DisplayRotationController(private var currentOrientation: Int) {
    fun nextOrientation(): Int {
        currentOrientation = if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        return currentOrientation
    }

    fun currentOrientation(): Int = currentOrientation

    fun isLandscape(): Boolean = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}
