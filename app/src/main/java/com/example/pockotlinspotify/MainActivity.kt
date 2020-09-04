package com.example.pockotlinspotify

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector.ConnectionListener
import com.spotify.android.appremote.api.SpotifyAppRemote

class MainActivity : AppCompatActivity() {
    private val CLIENT_ID = "096e228266944f71b227252d7703cae8"
    private val REDIRECT_URI = "com.example.pockotlinspotify://callback"
    private var mSpotifyAppRemote: SpotifyAppRemote? = null
    private val TAG = "AudioFocus"

    var mediaController: MediaController? = null
    var videoView: VideoView? = null
    var ifClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.openSpotify).setOnClickListener { // call sdk method for intiating spotify app
            starts()
            ifClicked = true
        }
        if (detect()) playVideo() else openPlaystore()
    }


    fun playVideo() {
        videoView = findViewById(R.id.videoView1)
        mediaController = MediaController(this)
        mediaController!!.setAnchorView(videoView)
        videoView?.setMediaController(mediaController)
        videoView?.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.new_divide))
        //videoView?.setVideoPath("https://vod-progressive.akamaized.net/exp=1599142462~acl=%2Fvimeo-prod-skyfire-std-us%2F01%2F3470%2F17%2F442351120%2F1936340589.mp4~hmac=e9a37a1069f99e1d296c10c06cea796c2ebc8f7df16dc80c8294f8460eead8dc/vimeo-prod-skyfire-std-us/01/3470/17/442351120/1936340589.mp4");
        videoView?.requestFocus()
        videoView?.start()
        videoView?.stopPlayback()
        videoView?.setOnPreparedListener(OnPreparedListener { mp -> mp.setVolume(0f, 0.2f) })
    }


    // detects whetherspotify app is installed or not...
    private fun detect(): Boolean {
        val pm = packageManager
        val isSpotifyInstalled: Boolean
        isSpotifyInstalled = try {
            pm.getPackageInfo("com.spotify.music", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        return isSpotifyInstalled
    }

    //  via intent opening spotiapp and playing song
    private fun test() {
        if (detect()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("spotify:playlist:37i9dQZF1DXaTIN6XNquoW:play")
            intent.putExtra(
                Intent.EXTRA_REFERRER,
                Uri.parse("android-app://" + this.packageName)
            )
            startActivityForResult(intent, 0)
        }
    }


    private fun openPlaystore() {
        val appPackageName = "com.spotify.music"
        // final String referrer = "adjust_campaign=PACKAGE_NAME&adjust_tracker=ndjczk&utm_source=adjust_preinstall";
        try {
            val uri = Uri.parse("market://details")
                .buildUpon()
                .appendQueryParameter(
                    "id",
                    appPackageName
                ) // .appendQueryParameter("referrer", referrer)
                .build()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (ignored: ActivityNotFoundException) {
            val uri = Uri.parse("https://play.google.com/store/apps/details")
                .buildUpon()
                .appendQueryParameter(
                    "id",
                    appPackageName
                ) //.appendQueryParameter("referrer", referrer)
                .build()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }


    override fun onStop() {
        super.onStop()
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }


    override fun onResume() {
        super.onResume()
        Log.e("cycle", "Onresume")
        if (mSpotifyAppRemote == null && ifClicked) {
            starts()
        } else {
        }
    }

    private fun starts() {
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()
        SpotifyAppRemote.connect(this, connectionParams,
            object : ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    mSpotifyAppRemote = spotifyAppRemote
                    Log.d("MainActivity", "Connected! Yay!")
                    // Now you can start interacting with App Remote
                    connected()
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("MyActivity", " here" + throwable.message, throwable)
                    val str = throwable.message
                    if (str!!.contains("The user must go to the Spotify and log-in")) {
                        val pm = this@MainActivity.packageManager
                        val intent = pm.getLaunchIntentForPackage("com.spotify.music")
                        startActivity(intent)
                    }

                    // Something went wrong when attempting to connect! Handle errors here
                }
            })
    }

    private fun connected() {

        // pass playist uri from here .... if pass empty string then it will start laying any song
        // Play a playlist
        mSpotifyAppRemote!!.playerApi.play("")

        // Subscribe to PlayerState
        mSpotifyAppRemote!!.playerApi
            .subscribeToPlayerState()
            .setEventCallback { playerState ->
                val track = playerState.track
                if (track != null) {
                    Log.d("MainActivity", track.name + " by " + track.artist.name)
                }
            }
    }


    // trying audio focus for duckking video audio , not working right now
//    private fun focus8() {
//        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        val handler = Handler()
//        val afChangeListener = OnAudioFocusChangeListener { focusChange ->
//            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//                // Permanent loss of audio focus
//                // Pause playback immediately
//                //  mediaController.getTransportControls().pause();
//                // Wait 30 seconds before stopping playback
//                // handler.postDelayed(delayedStopRunnable,
//                // TimeUnit.SECONDS.toMillis(30));
//                Log.d(TAG, "AUDIOFOCUS_LOSS")
//            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
//                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
//                // Pause playback
//            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
//                // Lower the volume, keep playing
//                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
//                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
//            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
//                Log.d(TAG, "AUDIOFOCUS_GAIN")
//                // Your app has been granted audio focus again
//                // Raise volume to normal, restart playback if necessary
//            }
//        }
//        val playbackAttributes = AudioAttributes.Builder()
//            .setUsage(AudioAttributes.USAGE_MEDIA)
//            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//            .build()
//        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//            .setAudioAttributes(playbackAttributes)
//            .setAcceptsDelayedFocusGain(true)
//            .setOnAudioFocusChangeListener(afChangeListener, handler)
//            .build()
//        val focusLock = Any()
//        var playbackDelayed = false
//        var playbackNowAuthorized = false
//
//// ...
//        val res = audioManager.requestAudioFocus(focusRequest)
//        synchronized(focusLock) {
//            if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
//                playbackNowAuthorized = false
//            } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//                playbackNowAuthorized = true
//                playVideo()
//            } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
//                playbackDelayed = true
//                playbackNowAuthorized = false
//            }
//        }
//    }


}