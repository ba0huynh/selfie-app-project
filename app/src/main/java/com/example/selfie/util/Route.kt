package com.example.selfie.util

import android.net.Uri
import java.net.URLEncoder

fun encodeRoute(route: String): String {
    return URLEncoder.encode(route, "utf-8")
}

fun decodeRoute(route: String): String {
    return java.net.URLDecoder.decode(route, "utf-8")
}

