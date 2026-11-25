package dev.huniee.hayul.patches.uid

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml
import dev.huniee.hayul.patches.runtime.installRuntimePatch
import dev.huniee.hayul.patches.shared.Constants.COMPATIBILITY_KAKAOTALK
import dev.huniee.hayul.patches.shared.Constants.COMPATIBILITY_TERMUX
import dev.huniee.hayul.patches.shared.Constants.SHARED_USER_ID

@Suppress("unused")
val sharedUidPatch = patch(
    name = "앱 UID 공유",
    description = "카카오톡과 Termux가 같은 sharedUserId로 실행되어 서로의 앱 데이터에 접근할 수 있게 합니다. 두 앱은 같은 키로 서명해야 합니다.",
    default = true
) {
    compatibleWith(COMPATIBILITY_KAKAOTALK, COMPATIBILITY_TERMUX)
    availability { _, _ -> PatchAvailability.REQUIRED }

    dependsOn(installRuntimePatch)

    execute {
        resourceXml("AndroidManifest.xml").use { xml ->
            xml.root.setAttribute("android:sharedUserId", SHARED_USER_ID)
            // 기기 SDK가 sharedUserMaxSdkVersion보다 높으면 새로 설치할 때 sharedUserId가 무시되므로 지웁니다.
            xml.root.removeAttribute("android:sharedUserMaxSdkVersion")
        }
    }
}
