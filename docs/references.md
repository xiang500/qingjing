# 同赛道项目调研

> 目的：说清「轻境」在同类项目中的位置——哪些是行业通用做法，哪些是我们自己的取舍。
> 调研时间：2026-11；范围：GitHub / Gitee / 掘金 / B 站。

## 一、直接对标（Java + Vue + AI 衣橱）

| 项目 | 技术栈 | 值得注意的做法 | 我们没跟的地方 |
| --- | --- | --- | --- |
| [smart-wardrobe-web](https://gitcode.com/gcw_zA7Uu7s8/smart-wardrobe-web)（华为云生态） | Spring Boot 2.7 + JPA + JWT；Vue3 + Vite + TS + Element Plus + Pinia | 灵感复刻（参考图拆单品→自有衣橱匹配）、旅拍穿搭（识别风景主色）、穿搭四维评分、收纳标签打印 | 依赖华为云 OBS/图像识别/盘古，不可离线演示；我们用本地 LLM 兼容接口 + 降级策略引擎 |
| [Kaleido AI 智能衣柜](https://github.com/yucheng-ou/kaleido-ai) | JDK21 + Spring Boot 3.5 + Spring Cloud + Spring AI | DDD + CQRS + 整洁架构、11 个微服务、RBAC 后台、AI Agent 工作流编排 | 微服务拆分对比赛演示是负担（启动、联调、部署成本）；我们做单体分层 |
| [AttireAgent](http://mcpgrep.com/pt/server/attireagent) | Spring Boot 3.2 + Spring AI + ruoyi-vue-pro + Vue3 | Chat / Agent 双模式、MCP 工具、ReAct | MCP 在演示现场感知弱，工程量 2~3 天；我们用 Function Calling + 采纳率闭环 |
| [智能衣橱穿搭打卡与二手置换](https://juejin.cn/post/7685283059571441674) | Vue3.4 + Pinia + Tailwind + ECharts + Spring Boot 3.2 | **CPW 单次穿着成本**、365 天热力图、三端（小程序/大屏/Admin） | 二手置换闭环超出赛题范围；CPW 与热力图我们**已吸收** |
| [yigui 智能衣柜](https://github.com/yyouyouyouyou114-del/yigui) | React 18 + TS + Vite + Tailwind + Zustand + IndexedDB + TF.js | 试穿两条路：**图层叠加（简化版）+ 云端试穿 API（真实版）**、本地 IndexedDB 存储 | 它也**没有 3D**；我们同样走 2D，但把 2D 做成设计风格而非技术妥协 |

## 二、海外项目的技术启发

| 项目 | 关键做法 | 对我们的意义 |
| --- | --- | --- |
| [loom](https://github.com/anushreeberlia/loom) | Florence-2 视觉打标 → FashionCLIP 2.0（ONNX 512 维）→ pgvector HNSW 检索 → 对装槽位过滤 → 颜色和谐 + 风格打分 | P2「向量 RAG」的参考实现；我们的 `utils/palette.ts` 已在做可解释配色评分，未来可换成向量检索 |
| [wardrowbe](https://github.com/Anyesh/wardrowbe) | FastAPI + Next.js + Postgres + Redis；Open-Meteo 免费天气 + Nominatim 地理编码；穿着追踪；MCP；ntfy/webhook/邮件通知 | 天气驱动与提醒机制；我们用 Open-Meteo 直连 + SSE 推送 |
| [fashion_recomender](https://github.com/gowhith/fashion_recomender) | 图像分类 + visual embedding + 天气/场合生成搭配 + LLM 解释 | 与我们的「工具调用 → 结构化推荐」同构 |
| [demo_agentic_rag](https://github.com/data42lana/demo_agentic_rag) | Agentic RAG：模型自己决定去检索什么 | 我们的 `OutfitAgent` 是同样的思路（模型决定查天气/翻衣橱/给推荐） |
| [OOTDiffusion](https://github.com/levihsu/OOTDiffusion) / [FitDiT](https://www.bilibili.com/video/BV18Lcce1Esj) / [Leffa](https://www.bilibili.com/video/BV1e3kQY1E3q) | 扩散模型虚拟试穿（VITON-HD / Dress Code 底座；FitDiT 4G 显存可跑） | 真试穿的正确做法是**接本地推理服务**，不是浏览器堆 3D；列为可选扩展 |

## 三、B 站参照

| 视频 | 类型 | 参考价值 |
| --- | --- | --- |
| [BV1SVKb6mEvc](https://www.bilibili.com/video/BV1SVKb6mEvc) 爱搭衣橱App | 产品向 | 热评指出真实痛点：「**我懒得在找衣服之前开 App，谁帮我把衣服找出来**」——所以 AI 主动推荐比手动录入更重要；另外提示要接天气、补齐职业/体型/发型等画像 |
| [BV1e1zMYQEc2](https://www.bilibili.com/video/BV1e1zMYQEc2) SpringBoot+Vue 服装搭配系统 | 毕设向（45 P） | 典型「服装购买 + 穿搭 + 档案 + 后台统计」套路，功能清单可对照自查 |
| [BV1yC411V7H1](https://www.bilibili.com/video/BV1yC411V7H1) 基于深度学习的智能衣柜 | 视觉识别向 | 端侧识别路线；我们最终没走（加载成本高） |
| [BV1a45F6REKK](https://www.bilibili.com/video/BV1a45F6REKK) / [BV17DbR6ZEig](https://www.bilibili.com/video/BV17DbR6ZEig) | Vibe Coding 小项目 | UI 与交互灵感 |
| [BV18Lcce1Esj](https://www.bilibili.com/video/BV18Lcce1Esj) FitDiT | 试穿模型 | 试穿效果的现状与门槛 |

## 四、结论：轻境的差异化

同类项目普遍具备「衣橱管理 + AI 推荐 + 后台」三件套，我们的差异化在四点：

1. **视觉系统**：四季光场（四套色盘 + 自动轮播）+ 纯 CSS 液态玻璃，同类项目里没有。属于「设计语言」层面的差异，不是功能堆砌。
2. **效果可度量**：AI 推荐有曝光/采纳/采纳率闭环（`ai_recommendation` 表），同类项目基本只讲「推荐了什么」，不讲「被采纳了多少」。
3. **敢做减法**：从 Three.js + MediaPipe（33MB 资源）收敛到 2D 分层 + 造型板，部署体积 34.8MB → 1.4MB。这是一次有数据支撑的工程取舍，而不是能力不足。
4. **可离线演示**：没有 API Key 时走同一管线的本地策略引擎，评委环境不需要联网也能跑通完整链路。

## 五、还没做、但值得做的（按性价比）

1. **灵感复刻**（对标 smart-wardrobe-web）：上传参考搭配图 → 视觉模型拆单品 → 在自有衣橱里匹配，输出「能复刻到 87%」。这是把「内容不具体」一次拉满的功能。
2. **身形避坑规则**：`user` 表已有 A/H/Y 体型字段，把 12 条知识片段从「解释」变成「拒绝穿什么」。
3. **真试穿**（可选）：本地部署 FitDiT / OOTDiffusion，后端 `/api/tryon` 转发；不要自己训练。
4. **向量 RAG**（对标 loom）：用 FashionCLIP 或中文 embedding 替换现在的二元组检索。

已落地：CPW 单次穿着成本 + 365 天穿着热力图（对标掘金那套）。