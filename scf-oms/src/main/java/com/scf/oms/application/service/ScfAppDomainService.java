package com.scf.oms.application.service;

import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

class ScfAppDomainService {

    private final ScfFacadeSupport support;

    ScfAppDomainService(ScfFacadeSupport support) {
        this.support = support;
    }

    Map<String, Object> login(Map<String, Object> req) {
        String username = support.s(req.get("username"));
        String password = support.s(req.get("password"));
        Map<String, Object> user = support.one(
                "select * from scf_user where username = ? and password = ? and status = 1",
                username,
                password
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "username or password is invalid");
        }
        return authPayload(user);
    }

    Map<String, Object> register(Map<String, Object> req) {
        String username = support.require(support.s(req.get("username")), "username is required");
        String password = support.require(support.s(req.get("password")), "password is required");
        String confirmPassword = support.require(support.s(req.get("confirmPassword")), "confirmPassword is required");
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "confirmPassword does not match password");
        }
        if (support.exists("select count(1) from scf_user where username = ?", username)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "username already exists: " + username);
        }
        long id = support.nextId("scf_user", 1000L);
        Timestamp now = support.now();
        String roleCode = support.blankTo(support.s(req.get("roleCode")), "operations");
        String roleName = support.blankTo(support.s(req.get("role")), support.blankTo(support.s(req.get("roleName")), roleCode));
        String displayName = support.blankTo(support.s(req.get("name")), support.blankTo(support.s(req.get("displayName")), username));
        support.jdbc().update(
                "insert into scf_user (id, user_code, username, password, display_name, role_code, role_name, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)",
                id,
                "user-" + id,
                username,
                password,
                displayName,
                roleCode,
                roleName,
                now,
                now
        );
        return currentUserById(id);
    }

    Map<String, Object> currentUser() {
        Map<String, Object> user = support.one("select * from scf_user where status = 1 order by id limit 1");
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "active user not found");
        }
        return authPayload(user);
    }

    List<Map<String, Object>> users() {
        return support.jdbc().query(
                "select * from scf_user where status = 1 order by id",
                (rs, rowNum) -> support.map(
                        "id", rs.getString("user_code"),
                        "username", rs.getString("username"),
                        "displayName", rs.getString("display_name"),
                        "roleCode", rs.getString("role_code"),
                        "roleName", rs.getString("role_name"),
                        "status", rs.getInt("status")
                )
        );
    }

    Map<String, Object> navigation() {
        return support.map(
                "items", List.of(
                        navGroup("upstream", "订单接入", "上游订单与履约入口", List.of(
                                navItem("upstream-orders", "订单接入", "UP")
                        )),
                        navGroup("oms", "OMS", "履约工作台与规则中心", List.of(
                                navItem("oms-workspace", "履约工作台", "OW"),
                                navItem("oms-rules", "路由规则", "OR"),
                                navItem("oms-exceptions", "异常中心", "OE"),
                                navItem("oms-split-merge", "拆单合单", "SM"),
                                navItem("oms-dashboard", "OMS 看板", "OD")
                        )),
                        navGroup("isc", "ISC", "库存台账与预警", List.of(
                                navItem("isc-ledger", "库存台账", "IL"),
                                navItem("isc-adjustments", "库存调整", "IA"),
                                navItem("isc-alerts", "安全库存", "SA")
                        )),
                        navGroup("wms", "WMS", "波次、拣货、打包、出库", List.of(
                                navItem("wms-taskhall", "波次大厅", "TH"),
                                navItem("wms-picking", "拣货任务", "PK"),
                                navItem("wms-packing", "打包复核", "PA"),
                                navItem("wms-shipment", "出库交接", "SH")
                        )),
                        navGroup("lgs", "LGS", "物流商、在途交付与回传", List.of(
                                navItem("lgs-providers", "物流商管理", "LP"),
                                navItem("lgs-delivery", "在途交付", "LD"),
                                navItem("lgs-callback", "回传记录", "LC")
                        ))
                )
        );
    }

    private Map<String, Object> currentUserById(long id) {
        Map<String, Object> user = support.requiredOne("select * from scf_user where id = ?", "user not found: " + id, id);
        return authPayload(user);
    }

    private Map<String, Object> authPayload(Map<String, Object> user) {
        return support.map(
                "token", "token-" + user.get("user_code"),
                "id", support.s(user.get("user_code")),
                "name", support.s(user.get("display_name")),
                "username", support.s(user.get("username")),
                "role", support.s(user.get("role_name")),
                "roleCode", support.s(user.get("role_code")),
                "user", support.map(
                        "id", support.s(user.get("user_code")),
                        "username", support.s(user.get("username")),
                        "name", support.s(user.get("display_name")),
                        "displayName", support.s(user.get("display_name")),
                        "roleCode", support.s(user.get("role_code")),
                        "role", support.s(user.get("role_name")),
                        "roleName", support.s(user.get("role_name"))
                )
        );
    }

    private Map<String, Object> navGroup(String id, String label, String title, List<Map<String, Object>> items) {
        return support.map("id", id, "type", "group", "label", label, "title", title, "items", items);
    }

    private Map<String, Object> navItem(String id, String label, String shortCode) {
        return support.map("id", id, "label", label, "short", shortCode);
    }
}
