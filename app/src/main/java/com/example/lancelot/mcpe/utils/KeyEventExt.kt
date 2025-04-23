package com.example.lancelot.mcpe.utils

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import android.view.KeyEvent.KEYCODE_ENTER

val KeyEvent.isEnterKey: Boolean
    get() = key.nativeKeyCode == KEYCODE_ENTER