# Hayul Patches

원본 [ye-seola/Hayul](https://github.com/ye-seola/Hayul)의 `patcher.dex`를 사용하는 모르페 패치입니다.
카카오톡(`com.kakao.talk`)과 Termux(`com.termux`)만 대상으로 합니다.

[Morphe에 저장소 추가](https://morphe.software/add-source?github=huni-ee/hayul-patches)

위 링크 또는 저장소 주소 `https://github.com/huni-ee/hayul-patches`로 추가할 수 있습니다.

## 패치 목록

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.2](https://github.com/huni-ee/hayul-patches/releases/tag/v1.0.0-dev.2)**&nbsp;&nbsp;&middot;&nbsp;&nbsp;`dev`&nbsp;&nbsp;&middot;&nbsp;&nbsp;**3 patches** across **2 apps**&nbsp;&nbsp;&middot;&nbsp;&nbsp;[Full details](PATCHES.md)

| # | App | Patches | Version | Package |
|---|---|---|---|---|
| 1 | [**KakaoTalk**](PATCHES.md#kakaotalk-comkakaotalk) | 3 | `26.10.1` | [`com.kakao.talk`](https://play.google.com/store/apps/details?id=com.kakao.talk) |
| 2 | [**Termux**](PATCHES.md#termux-comtermux) | 3 | `googleplay.2026.10.09` | [`com.termux`](https://play.google.com/store/apps/details?id=com.termux) |
<!-- PATCHES_END -->

## 원본과 다른 점

제조사·모델·`PRODUCT` 변조와 WebView 서비스 훅은 적용하지 않습니다.
WebView 제공자를 포함한 다른 패키지의 서명을 바꾸지 않도록 원본 PackageManager 훅의 적용 범위를 자기 패키지로 제한했습니다.

## 적용

카카오톡과 Termux가 같은 UID 그룹으로 설치되려면 **두 결과 APK를 같은 서명키로 서명해야 합니다.**

기존 설치 앱의 UID·서명이 바뀌는 패치입니다. 기존 앱 위에 설치되지 않을 수 있으며, 기존 앱 삭제 시 데이터도 삭제됩니다.
Termux:API 등 기존 Termux 플러그인은 별도 UID 그룹·서명 조건을 가지므로 이 패치의 지원 대상이 아닙니다.

## 빌드

빌드 방법은 [Morphe 문서](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md#-prepare-the-environment)를 참고하세요.

## 원본 및 참고 도구

- [HayulBasicStub](https://github.com/ye-seola/HayulBasicStub)
- [SigKill](https://github.com/xxxyanchenxxx/SigKill)
- [Morphe Patcher](https://github.com/MorpheApp/morphe-patcher)
