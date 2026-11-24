package com.nuhanhabib.tunevault.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The few icons the app needs, drawn from Material path data.
 * Using these instead of the full icon library keeps the app small.
 */
object AppIcons {
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()

    val Pause by lazy { icon("Pause", "M6,19h4V5H6v14zM14,5v14h4V5h-4z") }
    val SkipNext by lazy { icon("SkipNext", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z") }
    val SkipPrevious by lazy { icon("SkipPrevious", "M6,6h2v12H6zM9.5,12l8.5,6V6z") }
    val Shuffle by lazy {
        icon(
            "Shuffle",
            "M10.59,9.17L5.41,4L4,5.41l5.17,5.17L10.59,9.17zM14.5,4l2.04,2.04L4,18.59L5.41,20L17.96,7.46L20,9.5V4H14.5z" +
                "M14.83,13.41l-1.41,1.41l3.13,3.13L14.5,20H20v-5.5l-2.04,2.04L14.83,13.41z",
        )
    }
    private const val REPEAT = "M7,7h10v3l4,-4l-4,-4v3H5v6h2V7zM17,17H7v-3l-4,4l4,4v-3h12v-6h-2V17z"
    val Repeat by lazy { icon("Repeat", REPEAT) }
    val RepeatOne by lazy { icon("RepeatOne", REPEAT, "M13,15V9h-1l-2,1v1h1.5v4H13z") }
    val MusicNote by lazy {
        icon("MusicNote", "M12,3v10.55C11.41,13.21 10.73,13 10,13c-2.21,0 -4,1.79 -4,4s1.79,4 4,4s4,-1.79 4,-4V7h4V3H12z")
    }
    val LibraryMusic by lazy {
        icon(
            "LibraryMusic",
            "M20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2zM18,7h-3v5.5" +
                "c0,1.38 -1.12,2.5 -2.5,2.5S10,13.88 10,12.5s1.12,-2.5 2.5,-2.5c0.57,0 1.08,0.19 1.5,0.51V5h4V7z" +
                "M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z",
        )
    }
    val Download by lazy { icon("Download", "M19,9h-4V3H9v6H5l7,7L19,9zM5,18v2h14v-2H5z") }
    val Paste by lazy {
        icon(
            "Paste",
            "M19,2h-4.18C14.4,0.84 13.3,0 12,0S9.6,0.84 9.18,2H5C3.9,2 3,2.9 3,4v16c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V4" +
                "C21,2.9 20.1,2 19,2zM12,2c0.55,0 1,0.45 1,1s-0.45,1 -1,1s-1,-0.45 -1,-1S11.45,2 12,2zM19,20H5V4h2v3h10V4h2V20z",
        )
    }
    val Sync by lazy {
        icon(
            "Sync",
            "M12,4V1L8,5l4,4V6c3.31,0 6,2.69 6,6c0,1.01 -0.25,1.97 -0.7,2.8l1.46,1.46C19.54,15.03 20,13.57 20,12" +
                "C20,7.58 16.42,4 12,4zM12,18c-3.31,0 -6,-2.69 -6,-6c0,-1.01 0.25,-1.97 0.7,-2.8L5.24,7.74" +
                "C4.46,8.97 4,10.43 4,12c0,4.42 3.58,8 8,8v3l4,-4l-4,-4V18z",
        )
    }
    val PlaylistAdd by lazy {
        icon("PlaylistAdd", "M14,10H2v2h12V10zM14,6H2v2h12V6zM18,14v-4h-2v4h-4v2h4v4h2v-4h4v-2H18zM2,16h8v-2H2V16z")
    }
    val PlayNext by lazy {
        icon("PlayNext", "M19,9H2v2h17V9zM19,5H2v2h17V5zM2,15h13v-2H2V15zM17,13v6l5,-3L17,13z")
    }
    val Queue by lazy {
        icon(
            "Queue",
            "M15,6H3v2h12V6zM15,10H3v2h12V10zM3,16h8v-2H3V16zM17,6v8.18C16.69,14.07 16.35,14 16,14c-1.66,0 -3,1.34 -3,3" +
                "s1.34,3 3,3s3,-1.34 3,-3V8h3V6H17z",
        )
    }
    val PlaylistRemove by lazy {
        icon("PlaylistRemove", "M14,10H2v2h12V10zM14,6H2v2h12V6zM2,16h8v-2H2V16zM14.41,22L17,19.41L19.59,22L21,20.59L18.41,18L21,15.41L19.59,14L17,16.59L14.41,14L13,15.41L15.59,18L13,20.59z")
    }
    val Storage by lazy {
        icon("Storage", "M2,20h20v-4H2V20zM4,17h2v2H4V17zM2,4v4h20V4H2zM6,7H4V5h2V7zM2,14h20v-4H2V14zM4,11h2v2H4V11z")
    }
    val Battery by lazy {
        icon("Battery", "M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33v15.33C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V5.33C17,4.6 16.4,4 15.67,4z")
    }
    val Link by lazy {
        icon(
            "Link",
            "M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7C5.29,15.1 3.9,13.71 3.9,12z" +
                "M8,13h8v-2H8V13zM17,7h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5S19.76,7 17,7z",
        )
    }
}
