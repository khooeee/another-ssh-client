package com.ssher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.hostDataStore: DataStore<Preferences> by preferencesDataStore(name = "hosts")

class HostRepository(private val context: Context) {
    private val hostsKey = stringPreferencesKey("hosts_json")

    val hosts: Flow<List<HostProfile>> = context.hostDataStore.data.map { prefs ->
        decode(prefs[hostsKey].orEmpty())
    }

    suspend fun upsert(profile: HostProfile) {
        context.hostDataStore.edit { prefs ->
            val current = decode(prefs[hostsKey].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == profile.id }
            if (index >= 0) {
                current[index] = profile
            } else {
                current.add(0, profile)
            }
            prefs[hostsKey] = encode(current)
        }
    }

    suspend fun delete(id: String) {
        context.hostDataStore.edit { prefs ->
            val current = decode(prefs[hostsKey].orEmpty()).filterNot { it.id == id }
            prefs[hostsKey] = encode(current)
        }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        private fun encode(hosts: List<HostProfile>): String {
            val array = JSONArray()
            hosts.forEach { host ->
                array.put(
                    JSONObject()
                        .put("id", host.id)
                        .put("name", host.name)
                        .put("host", host.host)
                        .put("port", host.port)
                        .put("username", host.username),
                )
            }
            return array.toString()
        }

        private fun decode(raw: String): List<HostProfile> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(
                            HostProfile(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                host = obj.getString("host"),
                                port = obj.optInt("port", 22),
                                username = obj.getString("username"),
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
