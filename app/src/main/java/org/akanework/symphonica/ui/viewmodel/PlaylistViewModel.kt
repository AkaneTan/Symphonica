package org.akanework.symphonica.ui.viewmodel

import androidx.lifecycle.ViewModel
import org.akanework.symphonica.logic.data.Song

class PlaylistViewModel: ViewModel() {
    var playList = mutableListOf<Song>()
    var currentLocation: Int = 0
}
