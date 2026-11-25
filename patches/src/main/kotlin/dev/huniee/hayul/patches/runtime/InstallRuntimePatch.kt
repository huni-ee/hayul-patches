package dev.huniee.hayul.patches.runtime

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml
import dev.huniee.hayul.patches.shared.Constants.COMPATIBILITY_KAKAOTALK
import dev.huniee.hayul.patches.shared.Constants.COMPATIBILITY_TERMUX
import dev.huniee.hayul.patches.shared.Constants.FACTORY_NAME
import dev.huniee.hayul.patches.shared.requireSupportedPackage
import dev.huniee.hayul.patches.signature.preserveOriginalSignaturePatch

val installRuntimePatch = patch(
    name = "Hayul 실행 코드 적용",
    description = "앱이 자기 서명을 확인하면 재서명된 서명 대신 원본 서명을 돌려주는 Hayul 실행 코드를 넣습니다.",
    default = true
) {
    compatibleWith(COMPATIBILITY_KAKAOTALK, COMPATIBILITY_TERMUX)
    availability { _, _ -> PatchAvailability.REQUIRED }

    dependsOn(preserveOriginalSignaturePatch)

    // 서명 훅이 자기 패키지 이름과 비교하므로 DEX를 앱마다 따로 만듭니다.
    extendWith {
        val packageName = packageMetadata.packageName
        requireSupportedPackage(packageName)
        HayulPayload.forPackage(packageName).inputStream()
    }

    execute {
        resourceXml("AndroidManifest.xml").use { xml ->
            val application = xml.root.children("application").singleOrNull()
                ?: throw PatchException("AndroidManifest.xml에 application 요소가 정확히 1개 있어야 합니다.")
            application.setAttribute("android:appComponentFactory", FACTORY_NAME)
        }
    }
}
