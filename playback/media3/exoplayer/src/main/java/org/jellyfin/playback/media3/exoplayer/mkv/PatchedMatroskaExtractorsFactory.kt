package org.jellyfin.playback.media3.exoplayer.mkv

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.mkv.MatroskaExtractor as Media3MatroskaExtractor

/**
 * Jellyfin SW (TV): [DefaultExtractorsFactory] with Media3's Matroska extractor swapped for the
 * patched copy in this package, which finds Cues listed only in a nested SeekHead (otherwise such
 * MKV files are unseekable and cannot resume). Everything else is delegated unchanged.
 */
@OptIn(UnstableApi::class)
class PatchedMatroskaExtractorsFactory(
	private val delegate: DefaultExtractorsFactory,
) : ExtractorsFactory {
	// Mirrors DefaultExtractorsFactory's defaults; kept in sync through the setters below.
	private var subtitleParserFactory: SubtitleParser.Factory = DefaultSubtitleParserFactory()
	private var textTrackTranscodingEnabled = true

	@Deprecated("Deprecated in Media3, still called by DefaultMediaSourceFactory")
	override fun experimentalSetTextTrackTranscodingEnabled(textTrackTranscodingEnabled: Boolean): ExtractorsFactory {
		this.textTrackTranscodingEnabled = textTrackTranscodingEnabled
		@Suppress("DEPRECATION")
		delegate.experimentalSetTextTrackTranscodingEnabled(textTrackTranscodingEnabled)
		return this
	}

	override fun setSubtitleParserFactory(subtitleParserFactory: SubtitleParser.Factory): ExtractorsFactory {
		this.subtitleParserFactory = subtitleParserFactory
		delegate.setSubtitleParserFactory(subtitleParserFactory)
		return this
	}

	override fun experimentalSetCodecsToParseWithinGopSampleDependencies(
		codecsToParseWithinGopSampleDependencies: Int,
	): ExtractorsFactory {
		delegate.experimentalSetCodecsToParseWithinGopSampleDependencies(codecsToParseWithinGopSampleDependencies)
		return this
	}

	override fun createExtractors(): Array<Extractor> = swapMatroska(delegate.createExtractors())

	override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
		swapMatroska(delegate.createExtractors(uri, responseHeaders))

	private fun swapMatroska(extractors: Array<Extractor>): Array<Extractor> = Array(extractors.size) { i ->
		when (extractors[i]) {
			is Media3MatroskaExtractor -> MatroskaExtractor(
				subtitleParserFactory,
				if (textTrackTranscodingEnabled) 0 else MatroskaExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA,
			)
			else -> extractors[i]
		}
	}
}
