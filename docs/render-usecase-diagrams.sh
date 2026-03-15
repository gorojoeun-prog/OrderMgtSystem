#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
DOCS_DIR="$ROOT_DIR/docs"
OUT_DIR="$DOCS_DIR/images"

mkdir -p "$OUT_DIR"

render_with_plantuml_cmd() {
  echo "[INFO] plantuml 명령으로 렌더링합니다."
  for f in "$DOCS_DIR"/usecase-*.puml; do
    plantuml -tpng -o "$OUT_DIR" "$f"
    plantuml -tsvg -o "$OUT_DIR" "$f"
  done
}

render_with_docker() {
  echo "[INFO] Docker 기반 PlantUML로 렌더링합니다."
  docker run --rm \
    -v "$DOCS_DIR":/workspace/docs \
    -v "$OUT_DIR":/workspace/docs/images \
    plantuml/plantuml \
    -tpng /workspace/docs/usecase-simple.puml /workspace/docs/usecase-full.puml

  docker run --rm \
    -v "$DOCS_DIR":/workspace/docs \
    -v "$OUT_DIR":/workspace/docs/images \
    plantuml/plantuml \
    -tsvg /workspace/docs/usecase-simple.puml /workspace/docs/usecase-full.puml
}

if command -v plantuml >/dev/null 2>&1; then
  render_with_plantuml_cmd
elif command -v docker >/dev/null 2>&1; then
  render_with_docker
else
  echo "[ERROR] plantuml 또는 docker 명령을 찾을 수 없습니다."
  echo "        다음 중 하나를 준비한 뒤 다시 실행하세요:"
  echo "        1) plantuml 설치"
  echo "        2) docker 설치"
  exit 1
fi

echo "[DONE] 다이어그램 생성 완료"
ls -1 "$OUT_DIR" | sed 's/^/ - /'
