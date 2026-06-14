package com.example.permission.controller;

import com.example.permission.common.PageResult;
import com.example.permission.common.Result;
import com.example.permission.entity.Room;
import com.example.permission.entity.RoomStatusLog;
import com.example.permission.service.RoomService;
import com.example.permission.service.RoomStatusLogService;
import com.example.permission.service.SysUserService;
import com.example.permission.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomStatusLogService roomStatusLogService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('hotel:room:list')")
    public Result<PageResult<Room>> pageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false) String orientation,
            @RequestParam(required = false) String viewType) {
        PageResult<Room> result = roomService.pageList(pageNum, pageSize, roomNumber, buildingId, floorId, roomTypeId, status, orientation, viewType);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hotel:room:query')")
    public Result<Room> getById(@PathVariable Long id) {
        Room room = roomService.getById(id);
        return Result.success(room);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('hotel:room:add')")
    public Result<Void> add(@RequestBody Room room) {
        roomService.add(room);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('hotel:room:edit')")
    public Result<Void> update(@RequestBody Room room) {
        roomService.update(room);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hotel:room:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return Result.success();
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('hotel:room:batch:add')")
    public Result<Map<String, Object>> batchCreate(@RequestBody Map<String, Object> params) {
        Long buildingId = Long.valueOf(params.get("buildingId").toString());
        Long floorId = Long.valueOf(params.get("floorId").toString());
        Long roomTypeId = Long.valueOf(params.get("roomTypeId").toString());
        String numberPrefix = params.get("numberPrefix").toString();
        Integer startNum = Integer.valueOf(params.get("startNum").toString());
        Integer endNum = Integer.valueOf(params.get("endNum").toString());
        String orientation = params.get("orientation") != null ? params.get("orientation").toString() : null;
        String viewType = params.get("viewType") != null ? params.get("viewType").toString() : null;
        Map<String, Object> result = roomService.batchCreate(buildingId, floorId, roomTypeId, numberPrefix, startNum, endNum, orientation, viewType);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('hotel:room:status:edit')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestHeader("Authorization") String token,
                                     @RequestBody Map<String, Object> params) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(actualToken);
        String username = sysUserService.getUserDetail(userId).getUsername();
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        roomService.updateStatus(id, status, userId, username, remark);
        return Result.success();
    }

    @GetMapping("/{roomId}/logs")
    @PreAuthorize("hasAuthority('hotel:room:query')")
    public Result<List<RoomStatusLog>> getStatusLogs(@PathVariable Long roomId) {
        List<RoomStatusLog> logs = roomStatusLogService.listByRoomId(roomId);
        return Result.success(logs);
    }
}
