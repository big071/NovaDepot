# NovaDepot Ops Scripts (Docker)

## 1) 备份
```powershell
./scripts/ops/backup.ps1
./scripts/ops/backup.ps1 -OutputDir ./backups
```

## 2) 恢复
```powershell
./scripts/ops/restore.ps1 -BackupFile ./backups/novadepot-backup-20260422-220000.sql
```

## 3) 重置基线
```powershell
# RC 演示基线
./scripts/ops/reset-rc-baseline.ps1

# 商用样本基线（Phase 4）
./scripts/ops/reset-commercial-baseline.ps1
```

## 4) 发布 / 回滚检查
```powershell
./scripts/ops/release-checklist.ps1
./scripts/ops/data-quality-check.ps1
./scripts/ops/rollback-checklist.ps1 -BackupFile ./backups/novadepot-backup-20260422-220000.sql
```

## 5) 说明与风险
1. 必须在仓库根目录执行：`D:\新建文件夹\NovaDepot`。
2. 运行前请确认 Docker Daemon 已启动。
3. 重置/恢复会改写数据库数据，建议先备份。
4. 所有 SQL 脚本按 UTF-8 + `utf8mb4` 约束执行。
5. 发布后建议固定执行：
   1. `./scripts/ops/release-checklist.ps1`
   2. `./scripts/ops/data-quality-check.ps1`
   3. 场景截图/录屏基线核对（见 `docs/46-phase4-scenario-screenshot-baseline-2026-04-22.md`）。
