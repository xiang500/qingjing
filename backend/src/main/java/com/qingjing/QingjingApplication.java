package com.qingjing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 《轻境 · AI 实时穿搭与 3D 数字衣橱系统》后端入口。
 *
 * <p>第一阶段聚焦「建库搭架」：衣橱单品的 CRUD 与统计接口，
 * 后续阶段在同一工程内扩展 AI Agent（Function Calling + RAG）、
 * SSE 流式对话与社区能力。</p>
 */
@SpringBootApplication
public class QingjingApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingjingApplication.class, args);
    }
}