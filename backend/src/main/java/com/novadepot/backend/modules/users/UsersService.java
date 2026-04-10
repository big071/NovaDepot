package com.novadepot.backend.modules.users;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersService {
    public List<UserVO> list() {
        return List.of(
                new UserVO(1L, "admin", "系统管理员", 1L),
                new UserVO(2L, "operator", "仓库操作员", 1L)
        );
    }

    public UserVO detail(Long id) {
        return new UserVO(id, "user" + id, "用户" + id, 1L);
    }

    public UserVO create(UserCreateRequest request) {
        return new UserVO(100L, request.getUsername(), request.getRealName(), 1L);
    }
}
