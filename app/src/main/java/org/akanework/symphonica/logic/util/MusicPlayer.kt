package org.akanework.symphonica.logic.util

import android.content.Context
import android.os.SystemClock
import android.util.Log

enum class LoopingMode {
	LOOPING_MODE_NONE, LOOPING_MODE_PLAYLIST, LOOPING_MODE_TRACK
}

class Playlist<T>(initialList: List<T>?) {
	companion object {
		private const val TAG = "Playlist"
	}

	private inner class Entry<T>(private val track: T) {
		fun toTrack(): T = track
	}

	private var currentEntry: Entry<T>? = null
	var callbacks: PlaylistCallbacks<T>? = null

	var currentPosition: Int
		get() = currentEntry?.let { trackList.indexOf(it) } ?: 0
		set(value) {
			Log.v(TAG, "currentPosition=$value (was $currentPosition)")
			// Explicitly allow to set 0 on empty playlist.
			if (value > 0 && value >= size) {
				throw IllegalArgumentException("playlist of size $size trying to set pos to $value")
			} else if (size == 0) {
				currentEntry = null
			}
			val oldValue = currentPosition
			if (currentPosition != value) {
				currentEntry = if (size == 0)
					null
				else
					trackList[value]
			}
			if (size != 0) {
				// It is intended that this is NOT called if we add/remove track and thus the
				// current position changes. We have two other callback methods for that case.
				callbacks?.onPlaylistPositionChanged(oldValue, currentPosition)
			}
			Log.v(TAG, "currentPosition done")
		}
	private val trackList = ArrayList<Entry<T>>()
	val size: Int
		get() = trackList.size

	init {
		initialList?.forEach { t -> add(t, size) }
	}

	fun add(track: T, to: Int) {
		Log.v(TAG, "add(..., to=$to)")
		trackList.add(to, Entry(track))
		callbacks?.onPlaylistItemAdded(to)
		Log.v(TAG, "add done")
	}

	fun remove(at: Int) {
		Log.v(TAG, "remove(at=$at)")
		val oldPos = currentPosition
		trackList.removeAt(at)
		callbacks?.onPlaylistItemRemoved(at)
		if (at == oldPos) {
			// Currently playing item got removed.
			currentPosition = oldPos.mod(size)
		}
		Log.v(TAG, "remove done")
	}

	fun getItem(pos: Int?): T? {
		if (pos == null || pos < 0 || pos >= size)
			return null
		return trackList[pos].toTrack()
	}

	fun toMutableList(): MutableList<T> {
		return trackList.map { it.toTrack() }.toMutableList()
	}

}

interface PlaylistCallbacks<T> {
	fun onPlaylistReplaced(oldPlaylist: Playlist<T>?, newPlaylist: Playlist<T>?)
	fun onPlaylistPositionChanged(oldPosition: Int, newPosition: Int)
	fun onPlaylistItemAdded(position: Int)
	fun onPlaylistItemRemoved(position: Int)

	class Dispatcher<T> : PlaylistCallbacks<T> {
		private val callbacks = ArrayList<PlaylistCallbacks<T>>()

		fun registerPlaylistCallback(callback: PlaylistCallbacks<T>) {
			callbacks.add(callback)
		}

		fun unregisterPlaylistCallback(callback: PlaylistCallbacks<T>) {
			callbacks.remove(callback)
		}

		override fun onPlaylistReplaced(oldPlaylist: Playlist<T>?, newPlaylist: Playlist<T>?) {
			callbacks.forEach { it.onPlaylistReplaced(oldPlaylist, newPlaylist) }
		}

		override fun onPlaylistPositionChanged(oldPosition: Int, newPosition: Int) {
			callbacks.forEach { it.onPlaylistPositionChanged(oldPosition, newPosition) }
		}

		override fun onPlaylistItemAdded(position: Int) {
			callbacks.forEach { it.onPlaylistItemAdded(position) }
		}

		override fun onPlaylistItemRemoved(position: Int) {
			callbacks.forEach { it.onPlaylistItemRemoved(position) }
		}
	}

}

class MusicPlayer<T : Playable>(applicationContext: Context) : NextTrackPredictor(),
	MediaStateCallback, PlaylistCallbacks<T> {
	companion object {
		private const val TAG = "MusicPlayer"
		const val DEBUG = false
	}
	private val mediaPlayer = TransitionMediaPlayer(applicationContext)
	private val playlistCallbacks = PlaylistCallbacks.Dispatcher<T>()
	private var hasConsumedFirst = false
	var volume = 1f
		set(value) {
			if (field != value) {
				field = value
				dispatchPlaybackSettings(volume, speed, pitch)
			}
		}
	var pitch = 1f
		set(value) {
		if (field != value) {
			field = value
			dispatchPlaybackSettings(volume, speed, pitch)
		}
	}
	var speed = 1f
		set(value) {
		if (field != value) {
			field = value
			dispatchPlaybackSettings(volume, speed, pitch)
		}
	}
	var loopingMode = LoopingMode.LOOPING_MODE_NONE
		set(value) {
			if (field != value) {
				Log.v(TAG, "loopingMode=$value")
				val oldNext = predictNextTrack(false)
				field = value
				val newNext = predictNextTrack(false)
				if (oldNext != newNext) {
					dispatchPredictionChange(false)
				}
				Log.v(TAG, "loopingMode done")
			}
		}
	private var handledPositionChange = false
	var isPlaying = false
		private set
	var isUserPlaying = false
		private set
	var isSeekable = false
		private set
	var currentTimestamp = 0L
		private set
	var duration = 0L
		private set
	var decreasedPerformance = false
		private set
	var slowBuffer = false
		private set
	var bufferProgress = 0f
		private set
	var liveInfo: String? = null
		private set
	var timestampBase: Timestamp? = null
		private set
	var timestampUpdateTime = 0L
		private set
	var playlist: Playlist<T>? = null
		set(value) {
			if (field != value) {
				val old = field
				field = value
				old?.callbacks = null
				value?.callbacks = playlistCallbacks
				playlistCallbacks.onPlaylistReplaced(old, value)
			}
		}


	init {
		addMediaStateCallback(this)
		registerPlaylistCallback(this)
		mediaPlayer.setNextTrackPredictor(this)
		dispatchPlaybackSettings(volume, speed, pitch)
	}

	override fun predictNextTrack(consume: Boolean): Playable? {
		if (consume) Log.v(TAG, "predictNextTrack(consume=true)")
		return (if (!hasConsumedFirst) {
			hasConsumedFirst = consume
			getCurrentItem()
		} else if (isLooping()) {
			getCurrentItem()
		} else {
			getNextItem(consume)
		}).also {
			if (consume) Log.v(TAG, "predictNextTrack done")
		}
	}

	override fun isLooping(): Boolean {
		return loopingMode == LoopingMode.LOOPING_MODE_TRACK
				|| (loopingMode == LoopingMode.LOOPING_MODE_PLAYLIST && getCurrentItem()
					== getNextItem(false))
	}

	override fun onPlaybackCompleted() {
		Log.v(TAG, "onPlaybackCompleted()")
		hasConsumedFirst = false
		Log.v(TAG, "onPlaybackCompleted done")
	}

	fun play() {
		Log.v(TAG, "play()")
		if (!isPlaying) {
			playOrPause()
		}
		Log.v(TAG, "play done")
	}

	fun pause() {
		Log.v(TAG, "pause()")
		if (isPlaying) {
			playOrPause()
		}
		Log.v(TAG, "pause done")
	}

	fun playOrPause() {
		Log.v(TAG, "playOrPause()")
		dispatchPlayOrPause()
		Log.v(TAG, "playOrPause done")
	}

	fun prev() {
		Log.v(TAG, "prev()")
		val prevPosition = getPrevPosition()
		if (prevPosition == null) {
			seekTo(0)
		} else {
			playlist?.currentPosition = prevPosition
		}
		Log.v(TAG, "prev done")
	}

	fun next() {
		Log.v(TAG, "next()")
		getNextPosition()?.let { playlist?.currentPosition = it }
		Log.v(TAG, "next done")
	}

	private fun getCurrentItem(): Playable? {
		return playlist?.let {
			return@let it.getItem(it.currentPosition)
		}
	}

	private fun getNextItem(consume: Boolean): Playable? {
		if (consume) Log.v(TAG, "getNextItem(consume=true)")
		return playlist?.getItem(getNextPosition().also {
			if (consume) it?.let {
				// If someone sets currentPosition, we usually update media player state.
				// There however is one case where it is undesirable, and that is when media player
				// tells us that it advanced to the next song normally. We need to catch this and
				// just update the next song prediction in that case.
				handledPositionChange = true
				playlist?.currentPosition = it
			}
		}).also {
			if (consume) Log.v(TAG, "getNextItem done")
		}
	}

	private fun getPrevPosition(cpos: Int? = playlist?.currentPosition): Int? {
		return getPosition(cpos?.minus(1))
	}

	private fun getNextPosition(cpos: Int? = playlist?.currentPosition): Int? {
		return getPosition(cpos?.plus(1))
	}

	private fun getPosition(pos: Int?): Int? {
		var npos: Int? = null
		pos?.let { p ->
			playlist?.let {
				npos = p
				if (npos!! < 0 || npos!! >= it.size) {
					npos = if (loopingMode == LoopingMode.LOOPING_MODE_PLAYLIST) {
						npos!!.mod(it.size) // % is rem, not mod, don't use it here
					} else null
				}
			}
		}
		return npos
	}

	fun seekTo(positionMills: Long) {
		Log.v(TAG, "seekTo(positionMills=$positionMills)")
		try {
			dispatchSeek(positionMills)
		} catch (e: IllegalStateException) {
			Log.w(TAG, "ignoring seek to $positionMills: $e")
		}
		Log.v(TAG, "seekTo done")
	}

	/**
	 * This method does clean up, but this object can still be re-used afterwards.
	 */
	fun recycle() {
		Log.v(TAG, "recycle()")
		hasConsumedFirst = false
		playlist = null
		mediaPlayer.destroy()
		Log.v(TAG, "recycle done")
	}

	override fun onPlaylistReplaced(oldPlaylist: Playlist<T>?, newPlaylist: Playlist<T>?) {
		Log.v(TAG, "onPlaylistReplaced(...)")
		hasConsumedFirst = false
		dispatchPredictionChange(true)
		Log.v(TAG, "onPlaylistReplaced done")
	}

	override fun onPlaylistPositionChanged(oldPosition: Int, newPosition: Int) {
		Log.v(TAG, "onPlaylistPositionChanged(oldPosition=$oldPosition, newPosition=$newPosition)")
		this.decreasedPerformance = false
		if (handledPositionChange) {
			handledPositionChange = false
			dispatchPredictionChange(false)
		} else {
			hasConsumedFirst = false
			dispatchPredictionChange(true)
		}
		Log.v(TAG, "onPlaylistPositionChanged done")
	}

	override fun onPlaylistItemAdded(position: Int) {
		Log.v(TAG, "onPlaylistItemAdded(position=$position)")
		if (position == getNextPosition()) {
			dispatchPredictionChange(false)
		}
		Log.v(TAG, "onPlaylistItemAdded done")
	}

	override fun onPlaylistItemRemoved(position: Int) {
		Log.v(TAG, "onPlaylistItemRemoved(position=$position)")
		if (position == getNextPosition()) {
			dispatchPredictionChange(false)
		}
		Log.v(TAG, "onPlaylistItemRemoved done")
	}

	override fun onPlayingStatusChanged(playing: Boolean) {
		Log.v(TAG, "onPlayingStatusChanged(playing=$playing)")
		this.isPlaying = playing
		Log.v(TAG, "onPlayingStatusChanged done")
	}

	override fun onUserPlayingStatusChanged(playing: Boolean) {
		Log.v(TAG, "onUserPlayingStatusChanged(playing=$playing)")
		this.isUserPlaying = playing
		Log.v(TAG, "onUserPlayingStatusChanged done")
	}

	override fun onLiveInfoAvailable(text: String) {
		Log.v(TAG, "onLiveInfoAvailable(text=$text)")
		this.liveInfo = text
		Log.v(TAG, "onLiveInfoAvailable done")
	}

	override fun onMediaTimestampChanged(timestampMillis: Long) {
		if (DEBUG) Log.v(TAG, "onMediaTimestampChanged(timestampMillis=$timestampMillis)")
		this.currentTimestamp = timestampMillis
		if (DEBUG) Log.v(TAG, "onMediaTimestampChanged done")
	}

	override fun onMediaTimestampBaseChanged(timestampBase: Timestamp) {
		Log.v(TAG, "onMediaTimestampBaseChanged(...)")
		this.timestampBase = timestampBase
		this.timestampUpdateTime = SystemClock.elapsedRealtime()
		Log.v(TAG, "onMediaTimestampBaseChanged done")
	}

	override fun onSetSeekable(seekable: Boolean) {
		Log.v(TAG, "onSetSeekable(seekable=$seekable)")
		this.isSeekable = seekable
		Log.v(TAG, "onSetSeekable done")
	}

	override fun onMediaBufferSlowStatus(slowBuffer: Boolean) {
		Log.v(TAG, "onMediaBufferSlowStatus(slowBuffer=$slowBuffer)")
		this.slowBuffer = slowBuffer
		Log.v(TAG, "onMediaBufferSlowStatus done")
	}

	override fun onMediaBufferProgress(progress: Float) {
		Log.v(TAG, "onMediaBufferProgress(progress=$progress)")
		this.bufferProgress = progress
		Log.v(TAG, "onMediaBufferProgress done")
	}

	override fun onMediaHasDecreasedPerformance() {
		Log.v(TAG, "onMediaHasDecreasedPerformance()")
		this.decreasedPerformance = true
		Log.v(TAG, "onMediaHasDecreasedPerformance done")
	}

	override fun onPlaybackError(what: Int) {
		Log.v(TAG, "onPlaybackError(what=$what)")
		// Let UI bother with this.
		Log.v(TAG, "onPlaybackError done")
	}

	override fun onDurationAvailable(durationMillis: Long) {
		Log.v(TAG, "onDurationAvailable(durationMillis=$durationMillis)")
		this.duration = durationMillis
		Log.v(TAG, "onDurationAvailable done")
	}

	override fun onPlaybackSettingsChanged(volume: Float, speed: Float, pitch: Float) {
		Log.v(TAG, "onPlaybackSettingsChanged(volume=$volume, speed=$speed, pitch=$pitch)")
		if (volume != volume || speed != speed || pitch != pitch) {
			throw IllegalStateException()
		}
		Log.v(TAG, "onPlaybackSettingsChanged done")
	}

	fun registerPlaylistCallback(callback: PlaylistCallbacks<T>) {
		Log.v(TAG, "registerPlaylistCallback(...)")
		playlistCallbacks.registerPlaylistCallback(callback)
		callback.onPlaylistReplaced(null, playlist)
		callback.onPlaylistPositionChanged(0, playlist?.currentPosition ?: 0)
		Log.v(TAG, "registerPlaylistCallback done")
	}

	fun unregisterPlaylistCallback(callback: PlaylistCallbacks<T>) {
		Log.v(TAG, "unregisterPlaylistCallback(...)")
		playlistCallbacks.unregisterPlaylistCallback(callback)
		Log.v(TAG, "unregisterPlaylistCallback done")
	}

	fun addMediaStateCallback(callback: MediaStateCallback) {
		Log.v(TAG, "addMediaStateCallback(...)")
		mediaPlayer.addMediaStateCallback(callback)
		callback.onSetSeekable(isSeekable)
		callback.onMediaBufferProgress(0f)
		callback.onMediaBufferSlowStatus(false)
		callback.onPlaybackSettingsChanged(volume, speed, pitch)
		callback.onPlayingStatusChanged(isPlaying)
		callback.onUserPlayingStatusChanged(isUserPlaying)
		timestampBase?.let { callback.onMediaTimestampBaseChanged(it) }
		callback.onMediaTimestampChanged(currentTimestamp)
		Log.v(TAG, "addMediaStateCallback done")
	}

	fun removeMediaStateCallback(callback: MediaStateCallback) {
		Log.v(TAG, "removeMediaStateCallback(...)")
		mediaPlayer.removeMediaStateCallback(callback)
		Log.v(TAG, "removeMediaStateCallback done")
	}
}