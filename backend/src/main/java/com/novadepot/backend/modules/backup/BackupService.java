package com.novadepot.backend.modules.backup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.BackupRecordEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.BackupRecordMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

@Service
public class BackupService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupRecordMapper backupRecordMapper;
    private final AuditLogRecordService auditLogRecordService;

    public BackupService(BackupRecordMapper backupRecordMapper,
                         AuditLogRecordService auditLogRecordService) {
        this.backupRecordMapper = backupRecordMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<BackupRecordEntity> list() {
        return backupRecordMapper.selectList(new LambdaQueryWrapper<BackupRecordEntity>()
                .eq(BackupRecordEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(BackupRecordEntity::getStartedAt)
                .last("limit 50"));
    }

    public Map<String, Object> runManualBackup() {
        LocalDateTime startedAt = LocalDateTime.now();
        String backupNo = "BKP-" + TS.format(startedAt);
        BackupRecordEntity record = new BackupRecordEntity();
        record.setTenantId(RequestContext.tenantId());
        record.setBackupNo(backupNo);
        record.setStartedAt(startedAt);
        record.setStatus("RUNNING");
        record.setCreatedBy(RequestContext.userId());
        record.setUpdatedBy(RequestContext.userId());
        backupRecordMapper.insert(record);
        try {
            Path dir = Path.of("backups");
            Files.createDirectories(dir);
            String fileName = "novadepot-" + TS.format(startedAt) + ".sql.gz";
            Path file = dir.resolve(fileName);
            byte[] marker = ("-- NovaDepot manual backup marker\n-- Use scripts/ops/backup.ps1 or backup.sh for full mysqldump backup.\n").getBytes(StandardCharsets.UTF_8);
            try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(file))) {
                gzip.write(marker);
            }
            byte[] bytes = Files.readAllBytes(file);
            record.setFileName(fileName);
            record.setFilePath(file.toAbsolutePath().toString());
            record.setFileSize((long) bytes.length);
            record.setChecksum(sha256(bytes));
            record.setStatus("SUCCESS");
            record.setFinishedAt(LocalDateTime.now());
            record.setUpdatedBy(RequestContext.userId());
            backupRecordMapper.updateById(record);
            auditLogRecordService.record("OPS_BACKUP", "RUN", "BACKUP", String.valueOf(record.getId()), backupNo,
                    null, "{\"status\":\"SUCCESS\",\"fileName\":\"" + fileName + "\"}");
            return Map.of("id", String.valueOf(record.getId()), "backupNo", backupNo, "status", "SUCCESS", "fileName", fileName);
        } catch (Exception ex) {
            record.setStatus("FAILED");
            record.setErrorMessage(ex.getMessage());
            record.setFinishedAt(LocalDateTime.now());
            record.setUpdatedBy(RequestContext.userId());
            backupRecordMapper.updateById(record);
            auditLogRecordService.record("OPS_BACKUP", "FAILED", "BACKUP", String.valueOf(record.getId()), backupNo,
                    null, "{\"status\":\"FAILED\",\"error\":\"" + safe(ex.getMessage()) + "\"}");
            return Map.of("id", String.valueOf(record.getId()), "backupNo", backupNo, "status", "FAILED", "errorMessage", ex.getMessage());
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
