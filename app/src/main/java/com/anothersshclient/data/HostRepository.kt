package com.anothersshclient.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.hostDataStore: DataStore<Preferences> by preferencesDataStore(name = "hosts")

class HostRepository(context: Context) {
    private val appContext = context.applicationContext
    private val hostsKey = stringPreferencesKey("hosts_json")
    private val vault = PasswordVault(appContext)

    val hosts: Flow<List<HostProfile>> = appContext.hostDataStore.data.map { prefs ->
        decode(prefs[hostsKey].orEmpty()).map { host ->
            host.copy(hasPassword = vault.has(host.id))
        }
    }

    suspend fun upsert(profile: HostProfile, password: String?) {
        withContext(Dispatchers.IO) {
            if (password != null) {
                vault.set(profile.id, password)
            }
            appContext.hostDataStore.edit { prefs ->
                val current = decode(prefs[hostsKey].orEmpty()).toMutableList()
                val index = current.indexOfFirst { it.id == profile.id }
                val stored = profile.copy(hasPassword = false)
                if (index >= 0) {
                    current[index] = stored
                } else {
                    current.add(0, stored)
                }
                prefs[hostsKey] = encode(current)
            }
        }
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            vault.delete(id)
            appContext.hostDataStore.edit { prefs ->
                val current = decode(prefs[hostsKey].orEmpty()).filterNot { it.id == id }
                prefs[hostsKey] = encode(current)
            }
        }
    }

    suspend fun getPassword(hostId: String): String? = withContext(Dispatchers.IO) {
        vault.get(hostId)
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
