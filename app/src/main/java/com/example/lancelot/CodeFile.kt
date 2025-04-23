package com.example.lancelot

import android.net.Uri

data class CodeFile(
    val name: String,
    val content: TextState = TextState(""),
    val uri: Uri? = null,
    val isUnsaved: Boolean = false
)