package com.openlauncher.app.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local copy of the map's style, fonts, sprites and tiles, so a driven or downloaded area still draws without internet.
 * Only the OpenFreeMap host is ever cached or fetched here, and only for the same requests the map already makes.
 */
object MapTileCache {
    const val HOST = "tiles.openfreemap.org"
    private const val MAX_BYTES = 400L * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 4L * 1024 * 1024
    private val writes = AtomicInteger()

    class Entry(val mimeType: String, val body: ByteArray)

    sealed class Fetch {
        class Ok(val entry: Entry) : Fetch()
        /** The server answered but has no data for this URL, such as an empty ocean tile. */
        object Empty : Fetch()
        /** No usable answer: offline, timed out or an error status. */
        object Failed : Fetch()
    }

    fun isCacheableUrl(url: String): Boolean = runCatching {
        val parsed = URL(url)
        parsed.protocol == "https" && parsed.host == HOST && parsed.port == -1
    }.getOrDefault(false)

    /** Style and TileJSON documents change over time; versioned tiles, fonts and sprites do not. */
    fun isMutable(url: String): Boolean {
        val path = runCatching { URL(url).path }.getOrDefault("")
        return path.startsWith("/styles/") || path == "/planet"
    }

    internal fun key(url: String): String =
        MessageDigest.getInstance("SHA-1").digest(url.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun directory(context: Context): File = File(context.applicationContext.filesDir, "map-tiles").also { it.mkdirs() }

    fun read(context: Context, url: String): Entry? = runCatching {
        val file = File(directory(context), key(url))
        if (!file.isFile) return null
        val bytes = file.readBytes()
        val split = bytes.indexOf('\n'.code.toByte())
        if (split <= 0) return null
        file.setLastModified(System.currentTimeMillis())
        Entry(String(bytes, 0, split, Charsets.UTF_8), bytes.copyOfRange(split + 1, bytes.size))
    }.getOrNull()

    fun write(context: Context, url: String, entry: Entry) {
        runCatching {
            val dir = directory(context)
            val temp = File(dir, "${key(url)}.${Thread.currentThread().id}.tmp")
            temp.outputStream().use {
                it.write(entry.mimeType.toByteArray(Charsets.UTF_8))
                it.write('\n'.code)
                it.write(entry.body)
            }
            val target = File(dir, key(url))
            if (!temp.renameTo(target)) { target.delete(); temp.renameTo(target) }
            if (writes.incrementAndGet() % 100 == 0) trim(context)
        }
    }

    fun fetch(url: String): Fetch {
        if (!isCacheableUrl(url)) return Fetch.Failed
        val connection = try { URL(url).openConnection() as HttpURLConnection } catch (_: IOException) { return Fetch.Failed }
        return try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "OpenLauncher (+https://github.com/itsTwistys/openlauncher)")
            when (connection.responseCode) {
                200 -> {
                    val body = connection.inputStream.use { readLimited(it, MAX_ENTRY_BYTES) } ?: return Fetch.Failed
                    val mime = connection.contentType?.substringBefore(';')?.trim().takeUnless { it.isNullOrBlank() } ?: guessMime(url)
                    Fetch.Ok(Entry(mime, body))
                }
                204, 404 -> Fetch.Empty
                else -> Fetch.Failed
            }
        } catch (_: IOException) {
            Fetch.Failed
        } finally {
            connection.disconnect()
        }
    }

    /** Documents that change prefer the network and fall back to the saved copy; everything else prefers the saved copy. */
    fun load(context: Context, url: String): Entry? {
        if (!isCacheableUrl(url)) return null
        if (isMutable(url)) {
            val fetched = fetch(url)
            if (fetched is Fetch.Ok) { write(context, url, fetched.entry); return fetched.entry }
            return read(context, url)
        }
        read(context, url)?.let { return it }
        val fetched = fetch(url)
        if (fetched is Fetch.Ok) { write(context, url, fetched.entry); return fetched.entry }
        return null
    }

    fun sizeBytes(context: Context): Long = directory(context).listFiles().orEmpty().sumOf { it.length() }

    fun hasData(context: Context): Boolean = directory(context).list().orEmpty().any { !it.endsWith(".tmp") }

    fun clear(context: Context) {
        directory(context).listFiles().orEmpty().forEach { it.delete() }
    }

    /** Drop the least recently used files when the cache grows past its limit. */
    fun trim(context: Context) {
        val files = directory(context).listFiles().orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return
        val target = (MAX_BYTES * 0.85).toLong()
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= target) break
            total -= file.length()
            file.delete()
        }
    }

    private fun readLimited(input: InputStream, limit: Long): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) return null
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    private fun guessMime(url: String): String {
        val path = runCatching { URL(url).path }.getOrDefault("")
        return when {
            path.endsWith(".json") || path.startsWith("/styles/") || path == "/planet" -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".pbf") -> "application/x-protobuf"
            else -> "application/octet-stream"
        }
    }
}
