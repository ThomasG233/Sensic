package com.myhons.sensic

class Song(private var trackName : String, private var artistNames : List<String>, private var spotifyLink : String, private var spotifyID : String) {

    fun getTrackName() : String
    {
        return trackName
    }
    fun getArtistNames() : String
    {
        var artists = ""
        artistNames.forEach { artist ->
            artists += "$artist, "
        }
        artists = artists.substringBeforeLast(", ")
        return artists
    }
    fun getSpotifyLink() : String
    {
        return spotifyLink
    }
    fun getSpotifyID() : String
    {
        return spotifyID
    }
}