package dev.huniee.hayul.patches.signature

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.listApkEntries
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.patch.resourceXml
import dev.huniee.hayul.patches.shared.Constants.COMPATIBILITY_KAKAOTALK
import dev.huniee.hayul.patches.shared.Constants.COMPATIBILITY_TERMUX
import dev.huniee.hayul.patches.shared.Constants.FACTORY_NAME
import dev.huniee.hayul.patches.shared.Constants.FACTORY_TYPE
import dev.huniee.hayul.patches.shared.Constants.SHARED_USER_ID
import dev.huniee.hayul.patches.shared.requireSupportedPackage

val preserveOriginalSignaturePatch = patch(
    name = "원본 서명 유지",
    description = "Hayul 실행 코드가 앱에 돌려줄 원본 서명 인증서를 APK 안에 저장합니다.",
    default = true
) {
    compatibleWith(COMPATIBILITY_KAKAOTALK, COMPATIBILITY_TERMUX)
    availability { _, _ -> PatchAvailability.REQUIRED }

    execute {
        requireSupportedPackage(packageMetadata.packageName)
        if (classDefByOrNull(FACTORY_TYPE) != null && MARKER_PATH !in listApkEntries()) {
            throw PatchException("이 APK에는 이미 Hayul 실행 코드가 들어 있습니다. 원본 APK를 선택하세요.")
        }
        val signature = originalSignature()
        resourceFile(SIGNATURE_PATH, copy = false).writeText(signature)
        resourceFile(MARKER_PATH, copy = false).apply {
            parentFile.mkdirs()
            writeText(MARKER_CONTENT)
        }
    }

    // 다른 패치 번들의 패치와 함께 고르면 Manager가 필수 선택 잠금을 풀 수 있습니다.
    // 실행 코드나 UID 공유가 빠진 APK가 나오지 않도록 마지막에 다시 확인합니다.
    finalize { requireCompleteHayulPatch() }
}

private fun requireCompleteHayulPatch() {
    val (uidApplied, factoryApplied) = resourceXml("AndroidManifest.xml").use { xml ->
        (xml.root.attribute("android:sharedUserId") == SHARED_USER_ID) to
            (xml.root.children("application").singleOrNull()?.attribute("android:appComponentFactory") == FACTORY_NAME)
    }
    val missing = buildList {
        if (!factoryApplied || classDefByOrNull(FACTORY_TYPE) == null) add("Hayul 실행 코드 적용")
        if (!uidApplied) add("앱 UID 공유")
    }
    if (missing.isNotEmpty()) {
        throw PatchException(
            "[Hayul 필수 패치 누락]\n" +
                "이 상태에서는 패치를 완료할 수 없습니다.\n" +
                "누락된 필수 패치: ${missing.joinToString(", ")}\n" +
                "다른 소스와 함께 사용하면 필수 선택 잠금이 풀릴 수 있습니다.\n" +
                "Hayul 패치 3개를 모두 선택한 뒤 다시 패치하세요.",
        )
    }
}
