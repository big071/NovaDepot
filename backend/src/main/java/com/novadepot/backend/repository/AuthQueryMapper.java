package com.novadepot.backend.repository;

import com.novadepot.backend.modules.auth.AuthUserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AuthQueryMapper {

    @Select("""
            SELECT u.id AS userId,
                   u.tenant_id AS tenantId,
                   u.username AS username,
                   u.real_name AS realName,
                   u.password_hash AS passwordHash,
                   u.status AS userStatus,
                   t.status AS tenantStatus,
                   u.force_password_change AS forcePasswordChange,
                   u.failed_login_count AS failedLoginCount,
                   u.lock_until AS lockUntil
            FROM users u
            JOIN tenants t ON t.id = u.tenant_id AND t.deleted = 0
            WHERE t.tenant_code = #{tenantCode}
              AND u.username = #{username}
              AND u.deleted = 0
            LIMIT 1
            """)
    AuthUserRow findAuthUser(@Param("tenantCode") String tenantCode,
                             @Param("username") String username);

    @Select("""
            SELECT u.id AS userId,
                   u.tenant_id AS tenantId,
                   u.username AS username,
                   u.real_name AS realName,
                   u.password_hash AS passwordHash,
                   u.status AS userStatus,
                   t.status AS tenantStatus,
                   u.force_password_change AS forcePasswordChange,
                   u.failed_login_count AS failedLoginCount,
                   u.lock_until AS lockUntil
            FROM users u
            JOIN tenants t ON t.id = u.tenant_id AND t.deleted = 0
            WHERE u.tenant_id = #{tenantId}
              AND u.id = #{userId}
              AND u.deleted = 0
            LIMIT 1
            """)
    AuthUserRow findAuthUserById(@Param("tenantId") Long tenantId,
                                 @Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.perm_code
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
                        AND r.tenant_id = ur.tenant_id
                        AND r.deleted = 0
                        AND r.status = 'ACTIVE'
            JOIN role_permissions rp ON rp.role_id = r.id
                                   AND rp.tenant_id = ur.tenant_id
                                   AND rp.deleted = 0
            JOIN permissions p ON p.id = rp.permission_id
                              AND p.deleted = 0
                              AND p.status = 'ACTIVE'
            WHERE ur.tenant_id = #{tenantId}
              AND ur.user_id = #{userId}
              AND ur.deleted = 0
            ORDER BY p.perm_code
            """)
    List<String> findPermissions(@Param("tenantId") Long tenantId,
                                 @Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT r.role_code
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
                        AND r.tenant_id = ur.tenant_id
                        AND r.deleted = 0
                        AND r.status = 'ACTIVE'
            WHERE ur.tenant_id = #{tenantId}
              AND ur.user_id = #{userId}
              AND ur.deleted = 0
            ORDER BY r.role_code
            """)
    List<String> findRoleCodes(@Param("tenantId") Long tenantId,
                               @Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT ur.user_id
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
                        AND r.tenant_id = ur.tenant_id
                        AND r.deleted = 0
                        AND r.status = 'ACTIVE'
            JOIN role_permissions rp ON rp.role_id = r.id
                                   AND rp.tenant_id = ur.tenant_id
                                   AND rp.deleted = 0
            JOIN permissions p ON p.id = rp.permission_id
                              AND p.deleted = 0
                              AND p.status = 'ACTIVE'
                              AND p.perm_code = #{permCode}
            JOIN users u ON u.id = ur.user_id
                        AND u.tenant_id = ur.tenant_id
                        AND u.deleted = 0
                        AND u.status = 'ACTIVE'
            WHERE ur.tenant_id = #{tenantId}
              AND ur.deleted = 0
            ORDER BY ur.user_id
            """)
    List<Long> findUserIdsByPermission(@Param("tenantId") Long tenantId,
                                       @Param("permCode") String permCode);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash},
                updated_at = NOW(3)
            WHERE id = #{userId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            """)
    int updatePasswordHash(@Param("tenantId") Long tenantId,
                           @Param("userId") Long userId,
                           @Param("passwordHash") String passwordHash);

    @Update("""
            UPDATE users
            SET failed_login_count = #{failedLoginCount},
                lock_until = #{lockUntil},
                updated_at = NOW(3)
            WHERE id = #{userId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            """)
    int updateLoginSecurity(@Param("tenantId") Long tenantId,
                            @Param("userId") Long userId,
                            @Param("failedLoginCount") Integer failedLoginCount,
                            @Param("lockUntil") java.time.LocalDateTime lockUntil);

    @Update("""
            UPDATE users
            SET failed_login_count = 0,
                lock_until = NULL,
                last_login_at = NOW(3),
                updated_at = NOW(3)
            WHERE id = #{userId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            """)
    int markLoginSuccess(@Param("tenantId") Long tenantId,
                         @Param("userId") Long userId);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash},
                force_password_change = 0,
                failed_login_count = 0,
                lock_until = NULL,
                pwd_updated_at = NOW(3),
                updated_at = NOW(3)
            WHERE id = #{userId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            """)
    int changePassword(@Param("tenantId") Long tenantId,
                       @Param("userId") Long userId,
                       @Param("passwordHash") String passwordHash);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash},
                force_password_change = 1,
                failed_login_count = 0,
                lock_until = NULL,
                pwd_updated_at = NOW(3),
                updated_at = NOW(3)
            WHERE id = #{userId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            """)
    int resetPasswordByAdmin(@Param("tenantId") Long tenantId,
                             @Param("userId") Long userId,
                             @Param("passwordHash") String passwordHash);

    @Select("""
            SELECT force_password_change
            FROM users
            WHERE tenant_id = #{tenantId}
              AND id = #{userId}
              AND deleted = 0
            LIMIT 1
            """)
    Integer findForcePasswordChange(@Param("tenantId") Long tenantId,
                                    @Param("userId") Long userId);
}
