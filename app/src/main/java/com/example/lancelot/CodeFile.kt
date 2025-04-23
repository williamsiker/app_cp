package com.example.lancelot

import android.net.Uri
import com.example.lancelot.mcpe.model.TextState

data class CodeFile(
    val name: String,
    val content: TextState = TextState(""),
    val uri: Uri? = null,
    val isUnsaved: Boolean = false
)