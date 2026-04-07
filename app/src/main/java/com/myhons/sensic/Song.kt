package com.myhons.sensic

class Song(private var trackName : String, private var artistName : String, private var spotifyLink : String, private var spotifyID : String) {

    fun getTrackName() : String
    {
        return trackName
    }
    fun getArtistName() : String
    {
        return artistName
    }
    fun setTrackName(trackName : String)
    {
        this.trackName = trackName
    }
    fun setArtistName(artistName : String)
    {
        this.artistName = artistName
    }
    fun getSpotifyLink() : String
    {
        return spotifyLink
    }
    fun setSpotifyLink(spotifyLink : String)
    {
        this.spotifyLink = spotifyLink
    }
    fun getSpotifyID() : String
    {
        return spotifyID
    }
    fun setSpotifyID(spotifyID: String)
    {
        this.spotifyID = spotifyID
    }
}