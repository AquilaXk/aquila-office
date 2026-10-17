## 요약 (Summary)
- 관련 이슈: Closes #
- 변경 목적과 핵심 요약을 기술합니다.

## 주요 변경 사항 (Changes)
-

## 검증 결과 (Verification)
- [ ] Backend (`./back/gradlew -p back check`)
- [ ] Frontend (`npm --prefix front run lint && npm --prefix front run test:node && npm --prefix front run build`)
- [ ] Browser E2E (`npm --prefix front run test:browser`)
- [ ] Desktop (`BUILD_TARGET=desktop npm --prefix front run build && cargo test --manifest-path front/src-tauri/Cargo.toml`)
- [ ] Repository Contracts (`bash tools/test/check-staging-cd-contract.sh && node tools/readme/validate-readme.mjs`)
