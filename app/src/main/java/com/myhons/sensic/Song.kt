package com.myhons.sensic

/**
 * Song: Represents a song returned from ReccoBeats.
 * @param trackName: title of the song.
 * @param artistNames: full list of artists attributed to the song.
 * @param spotifyLink: hyperlink to the track on Spotify.
 * @param spotifyID: the ID of the track on Spotify.
 */
class Song(private var trackName : String, private var artistNames : List<String>, private var spotifyLink : String, private var spotifyID : String) {
    /**
     * Returns the name of the track.
     * @return the song name.
     */
    fun getTrackName() : String
    {
        return trackName
    }

    /**
     * Returns the list of artists who worked on the song.
     * @return the list of artists as a string seperated by commas.
     */
    fun getArtistNames() : String
    {
        var artists = ""
        // Add each artist to the string.
        artistNames.forEach { artist ->
            artists += "$artist, "
        }
        // Remove the last additional comma.
        artists = artists.substringBeforeLast(", ")
        return artists
    }

    /**
     * Returns the hyperlink to the track on Spotify.
     * @return the link to the saved track.
     */
    fun getSpotifyLink() : String
    {
        return spotifyLink
    }

    /**
     * Returns the ID of the track on Spotify.
     * @return the Spotify ID of the track.
     */
    fun getSpotifyID() : String
    {
        return spotifyID
    }
}