SET NAMES utf8mb4;

UPDATE faq_knowledge
SET review_status = 'APPROVED'
WHERE tenant_id = 1
  AND (review_status IS NULL OR review_status = '');

INSERT INTO faq_knowledge (id, tenant_id, faq_code, question, answer, tags, scene, priority, enabled, review_status, version_no, source_type, source_ref_id, created_at, created_by, updated_at, updated_by, deleted) VALUES
(300501,1,'FAQ-SP3-LOGISTICS-001','客户催促发货时客服应该如何回复？','先确认订单和出库状态，再告知客户已加急核查，并承诺在30分钟内同步物流进展。','物流催发,发货,客服','customer-service',100,1,'APPROVED',1,'SPRINT3_BASELINE',NULL,NOW(3),1,NOW(3),1,0),
(300502,1,'FAQ-SP3-INVENTORY-001','客户反馈缺货或库存异常时怎么处理？','先核对库存、在途入库和待出库单据；如存在风险，创建工单并同步仓储运营复核。','库存异常,缺货,工单','inventory',90,1,'APPROVED',1,'SPRINT3_BASELINE',NULL,NOW(3),1,NOW(3),1,0),
(300503,1,'FAQ-SP3-AFTERSALE-001','退换货问题如何收集信息？','请客户提供订单号、问题商品照片和诉求类型，再创建售后工单并记录处理备注。','退换货,售后,客服','customer-service',80,1,'APPROVED',1,'SPRINT3_BASELINE',NULL,NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE question=VALUES(question), answer=VALUES(answer), tags=VALUES(tags), scene=VALUES(scene), priority=VALUES(priority), enabled=VALUES(enabled), review_status=VALUES(review_status), version_no=VALUES(version_no), source_type=VALUES(source_type), source_ref_id=VALUES(source_ref_id), deleted=0, updated_at=NOW(3);

INSERT INTO sop_knowledge (id, tenant_id, sop_code, title, scene, steps, risks, review_checks, tags, priority, enabled, review_status, source_type, source_ref_id, created_at, created_by, updated_at, updated_by, deleted) VALUES
(300601,1,'SOP-SP3-LOGISTICS-001','物流催发处理 SOP','customer-service','1. 核对客户订单与出库单状态；2. 确认是否已发运及物流单号；3. 未发运时联系仓储运营加急；4. 向客户承诺下一次反馈时间；5. 工单备注处理结果并回访。','出库状态未核实就承诺时效；未记录责任人；客户二次追问无依据。','是否确认出库状态；是否记录责任人；是否设置下一次反馈时间。','物流催发,客服,出库',100,1,'APPROVED','SPRINT3_BASELINE',NULL,NOW(3),1,NOW(3),1,0),
(300602,1,'SOP-SP3-LOW-STOCK-001','低库存处理 SOP','inventory','1. 确认商品安全库存阈值；2. 核对当前可用库存、在途入库和待出库；3. 判断是否需要补货或调拨；4. 更新处理备注；5. 复盘阈值是否需要调整。','安全库存阈值过低；忽略在途库存；补货建议无事实依据。','是否读取最新库存；是否说明阈值来源；是否给出下一步补货或调拨动作。','低库存,补货,仓储',90,1,'APPROVED','SPRINT3_BASELINE',NULL,NOW(3),1,NOW(3),1,0),
(300603,1,'SOP-SP3-ANOMALY-001','异常巡检处理 SOP','warehouse','1. 扫描负库存、低库存和异常状态单据；2. 按影响范围排序；3. 指派责任人复核；4. 记录处理结论；5. 在审计中心回查相关业务编号。','只看结果不看业务编号；未区分责任人；异常关闭无备注。','是否列出业务编号；是否有责任人；是否能在审计中心回查。','异常巡检,审计,仓储',80,1,'APPROVED','SPRINT3_BASELINE',NULL,NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE title=VALUES(title), scene=VALUES(scene), steps=VALUES(steps), risks=VALUES(risks), review_checks=VALUES(review_checks), tags=VALUES(tags), priority=VALUES(priority), enabled=VALUES(enabled), review_status=VALUES(review_status), source_type=VALUES(source_type), source_ref_id=VALUES(source_ref_id), deleted=0, updated_at=NOW(3);

INSERT INTO rule_configs (id, tenant_id, config_key, config_name, config_value, value_type, scene, remark, enabled, created_at, created_by, updated_at, updated_by, deleted) VALUES
(300701,1,'LOW_STOCK_DEFAULT_THRESHOLD','低库存默认阈值','10','NUMBER','inventory','商品规格未配置安全库存时使用。',1,NOW(3),1,NOW(3),1,0),
(300702,1,'AUTO_REPLY_PRIORITY','自动回复优先级','FAQ,SOP,RULE_PROVIDER','TEXT','customer-service','客服 AI 自动回复优先引用 FAQ，再引用 SOP，最后回退规则提供者。',1,NOW(3),1,NOW(3),1,0),
(300703,1,'TICKET_CATEGORY_KEYWORDS','工单分类关键词','物流催发=物流,发货,催;库存异常=库存,缺货,少货;退换货=退,换,售后;质量反馈=质量,损坏','TEXT','customer-service','用于客服工单分类建议。',1,NOW(3),1,NOW(3),1,0),
(300704,1,'CS_REPLY_CANDIDATE_PRIORITY','客服候选回复优先级','FAQ答案,SOP下一步,标准安抚话术','TEXT','customer-service','用于候选回复排序。',1,NOW(3),1,NOW(3),1,0),
(300705,1,'AGENT_RESULT_DISPLAY_THRESHOLD','Agent 任务结果展示阈值','5','NUMBER','agent','Agent 默认展示重点结果条数。',1,NOW(3),1,NOW(3),1,0)
ON DUPLICATE KEY UPDATE config_name=VALUES(config_name), config_value=VALUES(config_value), value_type=VALUES(value_type), scene=VALUES(scene), remark=VALUES(remark), enabled=VALUES(enabled), deleted=0, updated_at=NOW(3);
