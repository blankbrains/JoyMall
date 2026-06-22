# joymall → joymall 项目重命名 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将整个 `joymall` 多模块 Maven 项目重命名为 `joymall`，替换所有 `joy`/`Joy`/`JOY` 出现

**Architecture:** 通过字符串内容替换 + 目录重命名两步完成，先改文件内容再改目录结构，确保最终一致性

**Tech Stack:** Bash (sed/perl), git, Maven/Java

## 全局约束

- `target/` 目录跳过（编译产物，后续删除重建）
- `.git/` 目录跳过
- `renren-fast/` 和 `renren-generator/` 模块无 joy 引用，保持不变
- .idea/ 目录的配置也要更新以保持 IDE 可用
- 长匹配优先：先替换复合词（`joymall`/`JoyMall`），再替换独立词（`joy`/`Joy`）

---

### Task 1: 内容替换 — 所有非 target / 非 .git 文件

**文件说明：** 对整个项目约 600 个文件的字符串内容进行批量替换，按从长到短顺序避免部分匹配污染。

**执行方式：** 使用 `find` + `sed` 在各文件类型中执行替换。优先级从长到短：

| 优先级 | 匹配模式 | 替换为 |
|--------|----------|--------|
| 1 | `JoyMall` | `JoyMall` |
| 2 | `JoyMall` | `JoyMall` |
| 3 | `joymall` | `joymall` |
| 4 | `Joymall` | `Joymall` |
| 5 | `JOY` | `JOY` |
| 6 | `joy` | `joy` |
| 7 | `Joy` | `Joy` |

- [ ] **Step 1: 确认 git 状态干净，记录当前状态**

```bash
cd /e/IDEAProject/joymall && git status --short
```

- [ ] **Step 2: 执行第一轮替换 — 长匹配（JoyMall / JoyMall / joymall / Joymall）**

```bash
cd /e/IDEAProject/joymall

# 1. JoyMall → JoyMall
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' -o -name '*.ftl' \) -print | xargs grep -l 'JoyMall' 2>/dev/null | while read f; do sed -i 's/JoyMall/JoyMall/g' "$f"; done

# 2. JoyMall → JoyMall
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' \) -print | xargs grep -l 'JoyMall' 2>/dev/null | while read f; do sed -i 's/JoyMall/JoyMall/g' "$f"; done

# 3. joymall → joymall
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' \) -print | xargs grep -l 'joymall' 2>/dev/null | while read f; do sed -i 's/joymall/joymall/g' "$f"; done

# 4. Joymall → Joymall
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' \) -print | xargs grep -l 'Joymall' 2>/dev/null | while read f; do sed -i 's/Joymall/Joymall/g' "$f"; done
```

- [ ] **Step 3: 执行第二轮替换 — 短匹配（JOY / joy / Joy）**

```bash
cd /e/IDEAProject/joymall

# 5. JOY → JOY
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' \) -print | xargs grep -l 'JOY' 2>/dev/null | while read f; do sed -i 's/JOY/JOY/g' "$f"; done

# 6. joy → joy
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' \) -print | xargs grep -l 'joy' 2>/dev/null | while read f; do sed -i 's/joy/joy/g' "$f"; done

# 7. Joy → Joy
find . -path '*/target/*' -prune -o -path '*/.git/*' -prune -o -type f \( -name '*.java' -o -name '*.yaml' -o -name '*.yml' -o -name '*.properties' -o -name '*.html' -o -name '*.xml' -o -name '*.json' -o -name '*.md' \) -print | xargs grep -l 'Joy' 2>/dev/null | while read f; do sed -i 's/Joy/Joy/g' "$f"; done
```

- [ ] **Step 4: 确认替换后的 git diff 合理**

```bash
cd /e/IDEAProject/joymall
git diff --stat | tail -20
# 检查是否有意外的变更量
```

---

### Task 2: 重命名 Java 包目录

**文件说明：** 文件内容已替换，现在需要移动 Java 源文件到新包路径。

**重构方式及原因：** 包名变更对应目录结构变更：
- `com/joy/joymall/<module>/` → `com/joy/joymall/<module>/`（子模块）
- `com/joy/common/` → `com/joy/common/`（common 模块）

- [ ] **Step 1: 重命名 common 模块的包目录（无 joymall 层）**

```bash
cd /e/IDEAProject/joymall/joymall-common
git mv src/main/java/com/joy src/main/java/com/joy
git mv src/test/java/com/joy src/test/java/com/joy
```

- [ ] **Step 2: 重命名各子模块的包目录（需要两层重命名）**

```bash
cd /e/IDEAProject/joymall

for module in joymall-auth-server joymall-cart joymall-coupon joymall-gateway joymall-member joymall-order joymall-product joymall-search joymall-seckill joymall-third-party joymall-ware; do
  if [ -d "$module/src/main/java/com/joy" ]; then
    (cd "$module" && git mv src/main/java/com/joy src/main/java/com/joy)
  fi
  if [ -d "$module/src/main/java/com/joy/joymall" ]; then
    (cd "$module" && git mv src/main/java/com/joy/joymall src/main/java/com/joy/joymall)
  fi
  if [ -d "$module/src/test/java/com/joy" ]; then
    (cd "$module" && git mv src/main/java/com/joy src/main/java/com/joy 2>/dev/null || true)
  fi
done

# 单独处理 test 目录
for module in joymall-auth-server joymall-cart joymall-coupon joymall-gateway joymall-member joymall-order joymall-product joymall-search joymall-seckill joymall-third-party joymall-ware; do
  if [ -d "$module/src/test/java/com/joy" ]; then
    (cd "$module" && git mv src/test/java/com/joy src/test/java/com/joy)
  fi
  if [ -d "$module/src/test/java/com/joy/joymall" ]; then
    (cd "$module" && git mv src/test/java/com/joy/joymall src/test/java/com/joy/joymall)
  fi
done
```

---

### Task 3: 重命名顶层模块目录

**文件说明：** 将 12 个 `joymall-*` 目录重命名为 `joymall-*`

- [ ] **Step 1: git mv 所有模块目录**

```bash
cd /e/IDEAProject/joymall

for old in joymall-*; do
  new="${old/joymall/joymall}"
  git mv "$old" "$new"
done
```

---

### Task 4: 更新 .idea 项目配置文件

**文件说明：** IDEA 的 `.idea/` 目录文件包含模块路径和名称引用，需要在目录重命名后更新。

- [ ] **Step 1: 更新 .idea 目录中的 joy 引用（由于内容替换已在 Task 1 完成，检查 .idea/ 中的 xml/json 是否还有残留）**

```bash
cd /e/IDEAProject/joymall
# 检查是否有残留
grep -rn 'joy\|Joy' .idea/ 2>/dev/null || echo "无残留"
```

如果还有残留，手动替换或使用 sed 处理。

- [ ] **Step 2: 更新根目录 .iml 文件和 README**

```bash
cd /e/IDEAProject/joymall
# 重命名 .iml 文件
if [ -f joymall.iml ]; then
  git mv joymall.iml joymall.iml
fi
# README.md 内容已在 Task 1 中替换
```

---

### Task 5: 清理并验证

- [ ] **Step 1: 删除所有 target/ 目录**

```bash
cd /e/IDEAProject/joymall
find . -name 'target' -type d -not -path '*/.git/*' -exec rm -rf {} + 2>/dev/null || true
```

- [ ] **Step 2: 最终扫描确认无 joy 残留**

```bash
cd /e/IDEAProject/joymall
grep -rn 'joy\|Joy\|JOY' --include='*.java' --include='*.yaml' --include='*.yml' --include='*.properties' --include='*.html' --include='*.xml' . 2>/dev/null | grep -v '/.git/' || echo "✅ 无残留"
```

- [ ] **Step 3: 更新 .gitignore 中的路径引用（如果有）**

检查 `.gitignore` 中是否有 `joymall` 路径引用并更新。

- [ ] **Step 4: 尝试增量编译验证**

```bash
cd /e/IDEAProject/joymall
mvn compile -q 2>&1 | tail -30
```

- [ ] **Step 5: git status 确认最终状态**

```bash
cd /e/IDEAProject/joymall
git status --short | head -30
```

---

### 可能遇到的问题及应对

1. **sed -i 兼容性**：Windows Git Bash 的 sed 可能行为不同，如果遇到问题改用 `perl -pi -e` 替代
2. **git mv 路径冲突**：如果某个模块没有 test 目录，`git mv` 会报错，用 `2>/dev/null || true` 忽略
3. **Maven 编译失败**：项目中可能存在非标准路径引用（如 resource 中的外部文件），需要手动修正
4. **.idea 文件难以自动更新**：如果 IDEA 配置更新不完整，可以删除 .idea 后让 IDEA 重新导入
