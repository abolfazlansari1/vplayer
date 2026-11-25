package com.vplayer.app

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("vplayer", Context.MODE_PRIVATE)

    var subSize: Float
        get() = sp.getFloat("subSize", 20f)
        set(v) {
            sp.edit().putFloat("subSize", v).apply()
        }

    var subColor: Int
        get() = sp.getInt("subColor", 0xFFFFFFFF.toInt())
        set(v) {
            sp.edit().putInt("subColor", v).apply()
        }

    var subBg: Float
        get() = sp.getFloat("subBg", 0.45f)
        set(v) {
            sp.edit().putFloat("subBg", v).apply()
        }

    var subBottom: Float
        get() = sp.getFloat("subBottom", 28f)
        set(v) {
            sp.edit().putFloat("subBottom", v).apply()
        }

    var subBold: Boolean
        get() = sp.getBoolean("subBold", true)
        set(v) {
            sp.edit().putBoolean("subBold", v).apply()
        }

    var autoSub: Boolean
        get() = sp.getBoolean("autoSub", true)
        set(v) {
            sp.edit().putBoolean("autoSub", v).apply()
        }

    var aspect: Int
        get() = sp.getInt("aspect", 0)
        set(v) {
            sp.edit().putInt("aspect", v).apply()
        }

    fun getPos(key: String): Long = sp.getLong("pos_$key", 0L)

    fun setPos(key: String, value: Long) {
        sp.edit().putLong("pos_$key", value).apply()
    }
}
