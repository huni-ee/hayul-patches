package dev.huniee.hayul.patches.signature

import app.morphe.patcher.apk.ApkSignatureScheme
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.listApkEntries
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.resourceFile
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

// Hayul 실행 코드가 원본 서명을 읽는 파일. HayulBasicStub이 이 이름으로 찾습니다.
internal const val SIGNATURE_PATH = "dev.seola.apppatcher.sig.orig"

// 이 패치 번들로 패치했다는 표시 파일. 다시 패치할 때 저장된 인증서를 재사용할지 이 파일로 판단합니다.
internal const val MARKER_PATH = "assets/hayul-morphe.txt"
internal const val MARKER_CONTENT = "hayul-morphe/1\n"

/**
 * 원본 인증서를 16진수 DER 문자열로 돌려줍니다.
 *
 * 이 패치 번들로 이미 패치한 APK면 저장해 둔 인증서를 씁니다.
 * 처음 패치하는 APK면 v3.1, v3, v2 서명 블록 순서로 찾고, 셋 다 없으면 v1(META-INF) 서명에서 읽습니다.
 */
internal fun originalSignature(): String {
    val entries = listApkEntries()
    if (MARKER_PATH in entries) {
        if (resourceFile(MARKER_PATH).readText() != MARKER_CONTENT || SIGNATURE_PATH !in entries) {
            throw PatchException("기존 Hayul 패치 표시 파일이나 저장된 원본 인증서가 올바르지 않습니다.")
        }
        return validateCertificateHex(resourceFile(SIGNATURE_PATH).readText().trim())
    }
    if (SIGNATURE_PATH in entries) {
        throw PatchException("예전 Hayul(main.py)로 패치한 APK는 이 패치 번들과 섞어 쓸 수 없습니다. 원본 APK를 사용하세요.")
    }

    val schemes = packageMetadata.signingCertificates
    val certificates = sequenceOf(ApkSignatureScheme.V31, ApkSignatureScheme.V3, ApkSignatureScheme.V2)
        .mapNotNull { schemes[it]?.takeIf { certs -> certs.isNotEmpty() } }
        .firstOrNull()
        ?: readV1Certificates(entries)
    if (certificates.size != 1) {
        throw PatchException("원본 APK의 서명 인증서가 정확히 1개여야 합니다. 찾은 개수: ${certificates.size}")
    }
    return certificates.single().encoded.toCertificateHex()
}

private fun readV1Certificates(entries: List<String>): List<X509Certificate> {
    val factory = CertificateFactory.getInstance("X.509")
    return entries.filter {
        val name = it.uppercase()
        name.startsWith("META-INF/") && listOf(".RSA", ".DSA", ".EC").any(name::endsWith)
    }.flatMap { name ->
        resourceFile(name).inputStream().use { stream ->
            factory.generateCertificates(stream).map { it as X509Certificate }
        }
    }.distinctBy { it.encoded.toCertificateHex() }
}

private fun validateCertificateHex(hex: String): String {
    if (hex.isEmpty() || hex.length % 2 != 0 || hex.length > 131072 || hex.any { it.digitToIntOrNull(16) == null }) {
        throw PatchException("저장된 원본 인증서가 올바른 16진수 DER 형식이 아닙니다.")
    }
    val bytes = ByteArray(hex.length / 2) { index -> hex.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    try {
        val stream = ByteArrayInputStream(bytes)
        CertificateFactory.getInstance("X.509").generateCertificate(stream)
        if (stream.available() != 0) throw PatchException("저장된 인증서 뒤에 다른 데이터가 붙어 있습니다.")
    } catch (exception: java.security.cert.CertificateException) {
        throw PatchException("저장된 원본 인증서가 X.509 DER 형식이 아닙니다.", exception)
    }
    return hex.lowercase()
}

/** 소문자 16진수 문자열로 바꿉니다. HayulBasicStub이 이 문자열로 android.content.pm.Signature를 만듭니다. */
private fun ByteArray.toCertificateHex(): String {
    val alphabet = "0123456789abcdef"
    return buildString(size * 2) {
        for (byte in this@toCertificateHex) {
            val value = byte.toInt() and 255
            append(alphabet[value ushr 4])
            append(alphabet[value and 15])
        }
    }
}
