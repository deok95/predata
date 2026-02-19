#!/bin/bash

# Mass conversion of remaining Korean strings
cd /Users/harrykim/Desktop/predata/backend/src/main/kotlin

# Run multiple passes of replacements
find . -name "*.kt" -type f -exec sed -i '' \
  -e 's/입니다\././g' \
  -e 's/입니다"/"/g' \
  -e 's/합니다\././g' \
  -e 's/합니다"/"/g' \
  -e 's/됩니다\././g' \
  -e 's/됩니다"/"/g' \
  -e 's/않습니다\././g' \
  -e 's/않습니다"/"/g' \
  -e 's/이어야 //g' \
  -e 's/여야 //g' \
  -e 's/해야 //g' \
  -e 's/시간\./hour./g' \
  -e 's/분\./minute./g' \
  -e 's/초\./second./g' \
  -e 's/시간"/hour"/g' \
  -e 's/분"/minute"/g' \
  -e 's/초"/second"/g' \
  -e 's/주\./week./g' \
  -e 's/일\./day./g' \
  -e 's/개\./items./g' \
  -e 's/번\./times./g' \
  -e 's/회\./times./g' \
  {} \;

echo "First pass complete"

# Second pass - specific patterns
find . -name "*.kt" -type f -exec sed -i '' \
  -e 's/유효하지 않은/invalid/g' \
  -e 's/필수 항목/required field/g' \
  -e 's/올바른/valid/g' \
  -e 's/잘못된/invalid/g' \
  -e 's/필요/need/g' \
  -e 's/불가능/impossible/g' \
  -e 's/가능/possible/g' \
  -e 's/허용되지/not allowed/g' \
  -e 's/허용/allowed/g' \
  -e 's/있음/exists/g' \
  -e 's/없음/none/g' \
  -e 's/여부/whether/g' \
  {} \;

echo "Second pass complete"

# Count remaining Korean
count=$(find . -name "*.kt" -type f | xargs grep -l '[가-힣]' | wc -l | tr -d ' ')
echo "Remaining files with Korean: $count"
