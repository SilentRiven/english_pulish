# english_pulish
EOF

  ---

# English Pulish

个人用的 Android 背单词 app，为「碎片时间复习 + 阅读时随手收词 +
数据透明」而设计。纯本地存储，无后端、无登录、无广告。

> 名字取自 "English Polish"（打磨英语），单词是个人随手命名，不纠正。

## 为什么又造一个背单词 app

市面上已有很多（扇贝、墨、Anki、百词斩…），作者对它们的不满集中在三点：

- **记忆模型不透明**：算法是黑箱，不知道自己在哪个词上最薄弱
- **阅读场景收词不顺**：看到生词想丢进生词本，但现有 app 的导入流程都隔一层
- **词库不是自己的**：被迫按预置词表推进，自己的词串不上

English Pulish 的差异化思路：
- 用 **FSRS-4.5** 代替老的 SM-2 间隔重复算法，并把 stability/difficulty/retrievability
  直接暴露给用户（v0.3 的"词汇画像"页待做）
- 用 Android 系统级 **Share Sheet** 收词：在 Chrome / 任何 app
  选中文字，分享菜单直接能扔进词库
- 每一次评分都记进 `review_logs` 表，永不清理，为未来的数据可视化铺路

## 现在做到哪里了（v0.1）

- [x] Android 工程骨架（Kotlin + Jetpack Compose + Hilt + Room + Paging 3）
- [x] Share Sheet 接词（单词直接入，句子弹选词 UI）
- [x] 预装词库：高考 3500 + 考研 5500 合并去重，共 **6045 词**，带中文释义、英音
  IPA、词性、例句中英对照
- [x] 词库浏览器：分页、前缀搜索、按来源筛选、行内朗读、手动加词 FAB
- [x] FSRS-4.5 复习循环：四档评分、今日队列（老词先 + 新词上限 20/天）
- [x] 系统 TTS 朗读
- [ ] 每日提醒（WorkManager）
- [ ] 学习数据页
- [ ] 真机端到端验证


| 层 | 选型 |
  |---|---|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository（单模块，克制） |
| DI | Hilt | 
| 数据库 | Room（SQLite） |
| 分页 | Paging 3 |
| 序列化 | kotlinx.serialization |
| 算法 | FSRS-4.5（纯 Kotlin 实现，开源默认权重） |
| 朗读 | 系统 TextToSpeech | 
| 最低 Android | 14 (API 34) |

更详细的架构、数据模型、约定见 [CLAUDE.md](./CLAUDE.md)。

## 构建

  ```bash
  # 需要 JDK 21、Android SDK build-tools 36.1.0、platforms android-36
  ./gradlew :app:assembleDebug
  # 产物：app/build/outputs/apk/debug/app-debug.apk
  
  # 跑单元测试（FSRS + 文本解析器）
  ./gradlew :app:testDebugUnitTest
  
  重新生成预装词库

  app/src/main/assets/preload.json 是从原始 NDJSON 词表清洗来的，源文件通过
  scripts/build_preload.py 处理。若要重新生成：

  # 需要把 scripts/raw/ 下的 zip 词表补回来（已 gitignore）
  python3 scripts/build_preload.py

  数据来源与免责

  词库数据（释义 / 音标 / 例句）来源于开源仓库 kajweb/dict 
  (https://github.com/kajweb/dict)，该仓库声明其数据是从「扇贝单词」app 爬取并结构化的。

  本项目仅将这份数据打包进个人使用的 Android
  app，不单独分发词库、不提供商业服务，视同「个人学习资料备份」。如你 fork
  或修改本项目用于其他目的，请自行评估版权风险，作者不为此承担责任。
  
  如果扇贝或 kajweb/dict 的维护者希望本项目下线相关资源，联系作者即可。

  非目标

  这些不会在可预见的将来做，别提 issue：

  - 多用户 / 账号系统 / 云同步
  - 应用商店上架
  - iOS 端 / Web 端
  - 内置广告或付费功能
  - AI 对话式背单词 / 游戏化打卡

  作者只有一个人（也就是作者本人），只给自己一台 Android 手机用。
