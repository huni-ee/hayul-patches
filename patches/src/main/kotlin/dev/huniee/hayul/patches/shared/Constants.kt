package dev.huniee.hayul.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    // 패치한 카카오톡과 Termux가 함께 쓰는 UID 그룹. 원본 Hayul(main.py)과 같은 값입니다.
    const val SHARED_USER_ID = "seola.patcher.abcde.shared"

    // assets/patcher.dex 안의 앱 시작 클래스. 원본 HayulBasicStub의 이름이라 바꾸면 안 됩니다.
    const val FACTORY_TYPE = "Ldev/seola/apppatcher/stub/PatcherAppComponentFactory;"
    const val FACTORY_NAME = "dev.seola.apppatcher.stub.PatcherAppComponentFactory"

    val COMPATIBILITY_KAKAOTALK = Compatibility(
        name = "KakaoTalk",
        packageName = "com.kakao.talk",
        description = "카카오톡 26.10.1 설치본 또는 APKMirror 파일을 사용합니다.",
        // APKMirror의 파일 형식(.apkm 분할 번들)과 같아야 Manager가 APKMirror로 안내합니다.
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFEE500,
        signatures = setOf(
            "2b06cc3d47782d7c497c07f17cb5f859cd6bbcb66829f3e67b96b7a44820d2ce"
        ),
        targets = listOf(
            AppTarget(
                version = "26.10.1",
                minSdk = 29
            )
        )
    )

    val COMPATIBILITY_TERMUX = Compatibility(
        name = "Termux",
        packageName = "com.termux",
        description = "Termux googleplay.2026.10.09 설치본 또는 APKMirror 파일을 사용합니다.",
        apkFileType = ApkFileType.APKM,
        signatures = setOf(
            "738f0a30a04d3c8a1be304af18d0779bcf3ea88fb60808f657a3521861c2ebf9"
        ),
        targets = listOf(
            AppTarget(
                version = "googleplay.2026.10.09",
                minSdk = 30,
                description = "Android 14 가상기기에서 Play판 공개 소스 빌드로 검증했습니다."
            )
        )
    )
}
