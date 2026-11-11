package ca.gbc.comp3074.sakugavault.player;

import android.content.Context;
import android.os.Build;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

/** Builds the ExoPlayer that the title page uses. */
@OptIn(markerClass = UnstableApi.class) // Media3's OkHttp data source, codec selector and media source factory
public final class PlayerFactory {

    private static final String EMULATOR_DECODER_PREFIX = "c2.goldfish.";

    private PlayerFactory() {
    }

    /**
     * Video requests go through the given OkHttp client, so the app's cookie jar sends the playback
     * cookie to the back end's stream proxy.
     */
    public static ExoPlayer create(Context context, OkHttpClient httpClient) {
        DataSource.Factory network = new OkHttpDataSource.Factory(httpClient);
        DataSource.Factory dataSources = new DefaultDataSource.Factory(context, network);
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(context)
                .setMediaCodecSelector(PlayerFactory::decodersFor)
                .setEnableDecoderFallback(true);
        ExoPlayer player = new ExoPlayer.Builder(context, renderers)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSources))
                .build();
        if (isEmulator()) {
            // The emulator decodes in software (see decodersFor), so keep it at 720p or below.
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                    .setMaxVideoSize(1280, 720)
                    .build());
        }
        return player;
    }

    /** True on the Android Emulator. */
    public static boolean isEmulator() {
        return Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish");
    }

    /**
     * The usual decoders, except that on the emulator its "goldfish" hardware decoders go last:
     * c2.goldfish.h264.decoder fails part-way through some HLS streams, while Android's own
     * software decoder (c2.android.avc.decoder) plays them fine. Real phones are unaffected.
     */
    private static List<MediaCodecInfo> decodersFor(String mimeType, boolean requiresSecureDecoder,
                                                    boolean requiresTunnelingDecoder)
            throws MediaCodecUtil.DecoderQueryException {
        List<MediaCodecInfo> decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
        if (!isEmulator()) {
            return decoders;
        }
        List<MediaCodecInfo> sorted = new ArrayList<>(decoders);
        sorted.sort((a, b) -> Boolean.compare(
                a.name.startsWith(EMULATOR_DECODER_PREFIX), b.name.startsWith(EMULATOR_DECODER_PREFIX)));
        return sorted;
    }
}
