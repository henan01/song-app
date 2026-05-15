package com.songapp.ktv

import android.app.Application
import com.songapp.ktv.data.KtvDatabase
import com.songapp.ktv.data.SongRepository
import com.songapp.ktv.player.KtvPlayer

class KtvApp : Application() {
    lateinit var database: KtvDatabase
        private set
    lateinit var repository: SongRepository
        private set
    lateinit var player: KtvPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = KtvDatabase.create(this)
        repository = SongRepository(database.songDao(), database.queueDao(), database.searchDao())
        player = KtvPlayer(this, repository)
    }

    override fun onTerminate() {
        super.onTerminate()
        player.release()
    }

    companion object {
        @Volatile
        private var instance: KtvApp? = null
        fun get(): KtvApp = requireNotNull(instance) { "KtvApp not yet created" }
    }
}
