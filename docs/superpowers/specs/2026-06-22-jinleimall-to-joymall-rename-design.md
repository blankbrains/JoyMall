# joymall → joymall 项目重命名设计

## 概述

将整个 `joymall` 项目（Java Spring Boot 多模块 Maven 项目）重命名为 `joymall`，替换所有出现 `joy`/`Joy`/`JOY` 的目录、文件、代码、配置。

## 变更范围

| 类别 | 源 | 目标 | 涉及文件数 |
|------|-----|------|-----------|
| Maven artifactId/name | `joymall` | `joymall` | 1 (顶层 pom) + 12 子模块 |
| Maven groupId | `com.joy.joymall` | `com.joy.joymall` | 所有子 pom |
| 模块目录 | `joymall-*` | `joymall-*` | ~12 个目录 |
| Java 包名 | `com.joy` | `com.joy` | ~300+ Java 文件 |
| Java 类名 (Joy*) | `Joymall*Application` | `Joymall*Application` | ~24 个应用类 + 测试类 |
| Java 类名 (Joy*) | `JoyMall*` | `JoyMall*` | 配置类、拦截器等 |
| 配置文件 | `joymall-*` 引用 | `joymall-*` | ~30+ yaml/properties |
| HTML 模板 | 文字引用 | 对应替换 | ~10+ 模板 |
| .idea 配置 | 模块引用 | 对应替换 | ~5 个 .idea 文件 |

## 映射规则

### 字符串替换

| 匹配模式 | 替换为 | 示例 |
|----------|--------|------|
| `joymall` (全小写) | `joymall` | `spring.application.name=joymall-auth-server` → `joymall-auth-server` |
| `joy` (全小写，独立) | `joy` | 包路径 `com.joy` → `com.joy` |
| `Joymall` (首字母大写) | `Joymall` | `JoymallAuthServerApplication` → `JoymallAuthServerApplication` |
| `JoyMall` | `JoyMall` | `JoyMallInterceptorConfig` → `JoyMallInterceptorConfig` |
| `JoyMall` | `JoyMall` | `JoyMallSessionConfig` → `JoyMallSessionConfig` |
| `Joy` (首字母大写) | `Joy` | 部分类名中的引用 |
| `JOY` (全大写) | `JOY` | 常量中的引用 |

### 替换优先级（长匹配优先）

为避免 `joy` 替换破坏 `joymall`，替换顺序为：
1. 先替换 `joymall`/`Joymall`/`JoyMall`/`JoyMall` 等复合词
2. 再替换独立的 `joy`/`Joy`

## 执行顺序

详见执行计划文档。

## 注意事项

- `renren-fast` 和 `renren-generator` 模块不包含 joy，保持不变
- `target/` 目录会在重命名后清理重建
- `.git/` 目录不操作
- 重命名后需要重新导入 IDE 项目
- 部分配置中可能有外部系统引用（如 Nacos、Sentinel），需手动核验

## 验证标准

1. 所有 `.java` 文件中无残留 `joy`/`Joy` 字符串
2. 所有配置文件中无残留 `joy` 引用
3. Maven 编译通过
4. 各模块能正常启动
