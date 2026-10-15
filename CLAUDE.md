# CLAUDE.md

카공지도 앱. 화면은 전부 웹(`../about-web`의 `/cafe-map` 하위, Next.js)이고 이 저장소는 React Native 0.77 WebView 셸입니다. 로직은 사실상 `App.tsx` 한 파일에 있습니다. 서비스 도메인은 `../about-docs/summary.md`, 브리지·딥링크 구조의 상세 설명은 원본 앱의 `../about-app/README.md`(이 저장소 README는 RN 템플릿 그대로)를 보세요.

## 쌍둥이 저장소 주의: `../about-app`

이 저장소는 어바웃 앱(`../about-app`)을 복제해 만든 거의 동일한 코드입니다. iOS 프로젝트 폴더명(`ios/어바웃`), Android `namespace`(`com.about.studyaboutclubapp`), Java 패키지 경로, `package.json`/`app.json`의 name("어바웃")까지 그대로라 **경로·이름만 보고는 어느 앱인지 구분되지 않습니다.** 수정 전 cwd가 `cafemap-app/`인지 확인하세요. 공통 로직 수정은 자동 동기화되지 않으니 다른 쪽 반영 여부를 매번 판단하세요.

`../about-app` 대비 실제 차이(`diff App.tsx`로 확인 가능):

- applicationId / bundle id `club.about20s.cafemap` (namespace는 어바웃 값 그대로 — 바꾸면 Java 패키지가 깨짐)
- 스킴은 `App.tsx` 상단 `APP_SCHEME = 'kagongmap'` 상수 하나로 관리 (어바웃 앱은 `about20s` 문자열 하드코딩)
- 시작 URL `https://study-about.club/cafe-map`, 이 URL에서는 iOS 스와이프 백 차단(탭이 `?tab=` + `router.replace`라 히스토리가 없어 빈 화면으로 빠짐)
- 스토어 URL·`FORCE_UPDATE_VERSION_*`가 카공지도 값(현재 `'0.0.0'` 자리표시자 — TODO 주석 참고)
- 어바웃 전용인 `requestNotificationPermission` 브리지와 `kagongmap://` 외부 실행 허용이 없음
- UA 지정이 `userAgent`(UA 전체를 `about_club_app`으로 **교체**). 어바웃 앱은 `applicationNameForUserAgent`(기본 UA 뒤에 덧붙임)

**두 앱 사이에 웹뷰 경로는 없습니다.** 둘 다 UA 문자열이 `about_club_app`(`App.tsx`의 `appConfig.agentSelector`)이고, 웹의 `isApp()`(`about-web/utils/validationUtils.ts`)은 `window.ReactNativeWebView` 존재만 보므로 **웹은 어느 앱 안인지 알 수 없습니다.** 웹은 대신 "세션이 /cafe-map에서 시작했는가"를 sessionStorage에 남겨 판별합니다(`about-web/features/cafeMap/utils/cafeMapSession.ts`). 그래서 `appConfig.uri`를 `/cafe-map` 밖으로 바꾸면 웹의 카공지도 컨텍스트 판별이 깨집니다.

## Commands

```bash
npm ci                    # postinstall에서 patch-package 실행 (lockfile은 npm)
npm start                 # Metro
npm run android           # 디버그 실행 / android:prod = release 모드
cd ios && bundle exec pod install   # iOS는 ios/어바웃.xcworkspace로 열기
npm run aab:android       # 스토어 업로드용 AAB (끝의 `open`은 macOS 전용)
```

- `bump-*` 스크립트는 fastlane을 부르지만 **Fastfile도 Gemfile의 fastlane도 없어 동작하지 않습니다.** 버전은 수동: `android/app/build.gradle`의 `versionCode`/`versionName`, iOS `project.pbxproj`의 `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`.
- release 서명은 `android/key.properties`의 `RELEASE_*`를 `app/build.gradle`이 주입합니다. keystore 경로는 `android/app/` 기준.
- `app/build.gradle` 상단 TODO는 google-services.json이 임시값이라고 하지만, 현재 `android/app/google-services.json`·`ios/GoogleService-Info.plist` 모두 `club.about20s.cafemap`으로 등록돼 있습니다(어바웃 앱과 같은 Firebase 프로젝트). 교체 전 콘솔에서 확인하세요.
- 테스트(`__tests__/`)는 템플릿 수준입니다. 검증은 실기기 확인입니다.

## patches/

`@react-native/community-cli-plugin@0.77.3` 하나(어바웃 앱과 동일) — `runServer.js`의 `unstable_extraMiddleware`에 `.filter(Boolean)` 추가. 사유 주석은 없지만 cli 20.x + RN 0.77 조합에서 undefined 미들웨어로 Metro가 죽는 것을 막는 용도로 보입니다. RN/cli 버전을 올리면 패치 적용이 실패합니다.

## WebView ↔ 네이티브 브리지

- 로드 URL은 `appConfig.uri` 하나로 **운영 고정**(dev 분기 없음). 로컬 웹 테스트 시 임시로 바꾸고 커밋하지 마세요.
- 웹→앱: `onGetMessage`가 `data.type || data.name`으로 `messageHandlers`를 찾습니다. 키: `share`, `callPhone`, `sendTextMessage`, `vibrate`, `haptic`, `getDeviceInfo`, `openExternalLink`, `exitApp`, `webviewReady`.
- 앱→웹(`name` 필드): `deeplink`{path, params}, `deviceInfo`{fcmToken, deviceId, platform, appVersion, buildNumber}, `backAction`(Android 하드웨어 백은 웹에 위임).
- 딥링크는 웹이 `webviewReady`를 보내기 전까지 큐잉됩니다. 브리지 메시지 변경은 `../about-web` 쪽 코드와 짝으로 바꿔야 합니다.

## 딥링크

진입 URL은 카카오링크/웹 URL → `kagongmap://` 정규화를 거친 것만 웹으로 전달됩니다(`handleDeepLink`).

- 커스텀 스킴: `kagongmap` (AndroidManifest, `ios/어바웃/Info.plist`)
- Android App Links: `study-about.club`·`about20s.club` 필터 모두 **주석 처리**(어바웃 앱과 충돌 방지 목적으로 보임)
- iOS: entitlements에 `applinks:study-about.club`이 어바웃 앱과 **동일하게 남아 있음**
- 카카오링크: `kakao0ab4a527fe2b99e3c18e904244a97e20://kakaolink` — 어바웃 앱과 같은 키·같은 필터. 두 앱이 모두 설치된 기기에서 어느 앱이 열릴지 보장되지 않습니다.
- 스킴 전체 목록은 `../딥링크스킴.xlsx`. 어바웃 웹의 카공지도 설치/실행 링크(`about-web/features/cafeMap/screens/CafeMapAppInstallDrawer.tsx`)와 스토어 URL·스킴을 맞춰야 합니다.
