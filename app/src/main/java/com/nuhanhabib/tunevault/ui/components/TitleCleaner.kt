package com.nuhanhabib.tunevault.ui.components

/** A song title and artist made readable from a raw YouTube video title. */
data class SongName(val title: String, val artist: String)

private val noise = Regex(
    "\\s*[(\\[][^)\\]]*?(official|lyric|audio|video|visuali[sz]er|\\bmv\\b|\\bhd\\b|\\b4k\\b|explicit|" +
        "remaster|full song|music video|clip officiel)[^)\\]]*[)\\]]",
    RegexOption.IGNORE_CASE,
)
private val dashes = Regex("\\s+[-–—]\\s+")
private val pipes = Regex("\\s+[|｜]\\s+")

/**
 * Turns "Gracie Abrams - That's So True (Lyrics)" uploaded by "Dan Music" into
 * title "That's So True" and artist "Gracie Abrams".
 */
fun cleanSongName(rawTitle: String, rawArtist: String): SongName {
    // "Song | Artist | Label" -> "Song"
    var title = rawTitle.replace(noise, "").split(pipes).first().trim()
    var artist = rawArtist.trim()
    val parts = title.split(dashes, limit = 2)
    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        artist = parts[0].trim()
        title = parts[1].trim()
    }
    // "LAAVAN Jasmine Sandlas" by "Jasmine Sandlas" -> "LAAVAN"
    if (artist.isNotBlank() && title.length > artist.length && title.endsWith(artist, ignoreCase = true)) {
        title = title.dropLast(artist.length).trimEnd(' ', '-', '–', '|', ',', ':')
    }
    title = title.removeSurrounding("\"").trim().ifEmpty { rawTitle }
    return SongName(title, artist)
}
