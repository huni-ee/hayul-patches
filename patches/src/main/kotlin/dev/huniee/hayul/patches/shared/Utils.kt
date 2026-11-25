package dev.huniee.hayul.patches.shared

import app.morphe.patcher.patch.PatchException

internal fun requireSupportedPackage(packageName: String) {
    if (packageName !in setOf("com.kakao.talk", "com.termux")) {
        throw PatchException("Hayul은 com.kakao.talk과 com.termux만 지원합니다. 입력한 패키지: $packageName")
    }
}
