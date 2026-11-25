# Patches

> Generated from `patches-list.json` — **v1.0.0-dev.2** (`dev`) · **3 patches** across **2 apps** · back to [README](README.md)

---

## KakaoTalk (com.kakao.talk)

**Supported versions:** `26.10.1`

| Patch | Details |
|---|---|
| **Hayul 실행 코드 적용** | 앱이 자기 서명을 확인하면 재서명된 서명 대신 원본 서명을 돌려주는 Hayul 실행 코드를 넣습니다. |
| **앱 UID 공유** | 카카오톡과 Termux가 같은 sharedUserId로 실행되어 서로의 앱 데이터에 접근할 수 있게 합니다. 두 앱은 같은 키로 서명해야 합니다. |
| **원본 서명 유지** | Hayul 실행 코드가 앱에 돌려줄 원본 서명 인증서를 APK 안에 저장합니다. |

---

## Termux (com.termux)

**Supported versions:** `googleplay.2026.10.09`

| Patch | Details |
|---|---|
| **Hayul 실행 코드 적용** | 앱이 자기 서명을 확인하면 재서명된 서명 대신 원본 서명을 돌려주는 Hayul 실행 코드를 넣습니다. |
| **앱 UID 공유** | 카카오톡과 Termux가 같은 sharedUserId로 실행되어 서로의 앱 데이터에 접근할 수 있게 합니다. 두 앱은 같은 키로 서명해야 합니다. |
| **원본 서명 유지** | Hayul 실행 코드가 앱에 돌려줄 원본 서명 인증서를 APK 안에 저장합니다. |

---
