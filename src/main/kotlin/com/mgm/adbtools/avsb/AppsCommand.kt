package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.MIN_TIME_TO_OUTPUT_RESPONSE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.avsb.AppsCommand.AppAction
import com.mgm.adbtools.command.Command2
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.isAppInstall

class AppsCommand : Command2<String, AppAction, String> {

    enum class App(val openCommand: String, val closeCommand: String, val appName: String, val packageName: String) {
        NETFLIX(
            "am start -a com.netflix.action.NETFLIX_KEY_START -n com.netflix.ninja/.MainActivity",
            "am force-stop com.netflix.ninja",
            "Netflix",
            "com.netflix.ninja"
        ),
        HBO_MAX(
            "am start -a android.intent.action.MAIN -n com.wbd.stream/com.wbd.beam.BeamActivity",
            "am force-stop com.wbd.stream",
            "HBO Max",
            "com.wbd.stream"
        ),
        DISNEY_PLUS(
            "am start -a android.intent.action.VIEW -n com.disney.disneyplus/com.bamtechmedia.dominguez.main.MainActivity",
            "am force-stop com.disney.disneyplus",
            "Disney+",
            "com.disney.disneyplus"
        ),
        PRIME_VIDEO(
            "am start -a android.intent.action.MAIN -n com.amazon.amazonvideo.livingroom/com.amazon.ignition.IgnitionActivity",
            "am force-stop com.amazon.amazonvideo.livingroom",
            "Prime Video",
            "com.amazon.amazonvideo.livingroom"
        ),
        YOUTUBE(
            "am start -a com.google.android.youtube.tv com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity",
            "am force-stop com.google.android.youtube.tv",
            "YouTube",
            "com.google.android.youtube.tv"
        ),
//        HBO_GO(
//            "am start -a android.intent.action.MAIN -n com.hbo.hbonow/.MainActivity",
//            "am force-stop com.hbo.hbonow",
//            "HBO Go",
//            "com.hbo.hbonow"
//        ),
        PLAY_STORE(
            "am start -a android.intent.action.MAIN -n com.android.vending/com.google.android.finsky.tvmainactivity.TvMainActivity",
            "am force-stop com.android.vending",
            "Play Store",
            "com.android.vending"
        ),
        PLAY_MOVIES(
            "am start -a android.intent.action.MAIN -n com.google.android.videos/.tv.presenter.activity.TvLauncherActivity",
            "am force-stop com.google.android.videos",
            "Play Movies",
            "com.google.android.videos"
        ),
        PLAY_GAMES(
            "am start -a android.intent.action.MAIN -n com.google.android.play.games/com.google.android.apps.play.games.app.atv.features.home.HomeActivity",
            "am force-stop com.google.android.play.games",
            "Play Games",
            "com.google.android.play.games"
        ),
//        SELF_CARE_IE(
//            "am start -a android.intent.action.MAIN -n com.vodafone.vtv.app02/.ui.activities.SplashActivity",
//            "am force-stop com.vodafone.vtv.app02",
//            "Self Care IE",
//            "com.vodafone.vtv.app02"
//        ),
//        SELF_CARE_PT(
//            "am start -a android.intent.action.MAIN -n com.vodafone.vtv.app01/.ui.activities.SplashActivity",
//            "am force-stop com.vodafone.vtv.app01",
//            "Self Care PT",
//            "com.vodafone.vtv.app01"
//        ),
        VTV(
            "am start -a android.intent.action.MAIN -n com.vodafone.vtv.avsb/com.witsoftware.vodafonetv.vsb.screen.main.MainActivity",
            "am force-stop com.vodafone.vtv.avsb",
            "VTV",
            "com.vodafone.vtv.avsb"
        ),
//        TV_RECOMMENDATION(
//            "am start -a android.intent.action.MAIN -n com.google.android.tvrecommendations/.TvRecommendationActivity",
//            "am force-stop com.google.android.tvrecommendations",
//            "TV Recommendation",
//            "com.google.android.tvrecommendations"
//        ),
//        TWITCH(
//            "am start -a android.intent.action.MAIN -n tv.twitch.android.app/tv.orange.features.app.splash.SplashActivity",
//            "am force-stop tv.twitch.android.app",
//            "Twitch",
//            "tv.twitch.android.app"
//        ),
//        VLC(
//            "am start -a android.intent.action.MAIN -n org.videolan.vlc/org.videolan.vlc.gui.MainActivity",
//            "am force-stop org.videolan.vlc",
//            "VLC",
//            "org.videolan.vlc"
//        ),
        FILMIN_ES(
            "am start -a android.intent.action.MAIN -n com.filmin.androidtv.portugal/com.filmin.androidtv.MainActivity",
            "am force-stop com.filmin.androidtv.portugal",
            "Filmin",
            "com.filmin.androidtv"
        ),
        RTP_PLAY(
            "am start -a android.intent.action.MAIN -n pt.rtp.play/io.didomi.sdk.notice.ctv.TVNoticeDialogActivity",
            "am force-stop pt.rtp.play",
            "RTP Play",
            "pt.rtp.play"
        ),
        OPTO(
            "am start -a android.intent.action.MAIN -n com.impresa.opta/com.magycal.androidott.ui.tv.TVBaseActivity",
            "am force-stop com.impresa.opta",
            "Opto",
            "com.impresa.opta"
        ),
        APPLE_TV(
            "am start -a android.intent.action.MAIN -n com.apple.atve.androidtv.appletv/.MainActivity",
            "am force-stop com.apple.atve.androidtv.appletv",
            "Apple TV",
            "com.apple.atve"
        ),
        FILMTWIST(
            "am start -a android.intent.action.MAIN -n com.lisbonworks.filmtwist/com.vodfactory.otto.ui.main.MainActivity",
            "am force-stop com.lisbonworks.filmtwist",
            "Filmtwist",
            "com.lisbonworks.filmtwist"
        ),
        SMART_REPLAY_SOCCER(
            "am start -a android.intent.action.MAIN -n com.sixfloorsolutions.smartreplaysoccer/.MainActivity",
            "am force-stop com.sixfloorsolutions.smartreplaysoccer",
            "Smart Replay Soccer",
            "com.sixfloorsolutions.smartreplaysoccer"
        ),
        PANDA_PLUS(
            "am start -a android.intent.action.MAIN -n pt.dreamia.pandaplus/com.magycal.androidott.ui.tv.TVBaseActivity",
            "am force-stop pt.dreamia.pandaplus",
            "Panda+",
            "pt.dreamia.pandaplus"
        ),
        IOL_TVIPLAYER(
            "am start -a android.intent.action.MAIN -n pt.iol.tviplayer.androidtv/.ui.base.activities.SplashScreenActivity",
            "am force-stop pt.iol.tviplayer.androidtv",
            "IOL TVIPlayer",
            "pt.iol.tviplayer"
        )

    }

    enum class AppAction {
        OPEN, CLOSE
    }

    override fun execute(p: String, p2: AppAction, project: Project, device: IDevice): String {
        val app = App.entries.firstOrNull {
            it.appName.equals(p, true)
        } ?: return EMPTY

        if (device.isAppInstall(app.packageName).not()) {
            return "App ${app.appName} not installed."
        }

        return when (p2) {
            AppAction.OPEN -> {
                device.executeShellCommandWithTimeout(app.openCommand, ShellOutputReceiver(), MIN_TIME_TO_OUTPUT_RESPONSE)
                "Opened ${app.appName}"
            }

            AppAction.CLOSE -> {
                device.executeShellCommandWithTimeout(app.closeCommand, ShellOutputReceiver(), MIN_TIME_TO_OUTPUT_RESPONSE)
                "Closed ${app.appName}"
            }
        }
    }
}
