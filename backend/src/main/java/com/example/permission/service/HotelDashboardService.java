package com.example.permission.service;

import com.example.permission.entity.Building;
import com.example.permission.entity.Floor;
import com.example.permission.entity.Room;
import com.example.permission.entity.RoomType;
import com.example.permission.mapper.BuildingMapper;
import com.example.permission.mapper.FloorMapper;
import com.example.permission.mapper.RoomMapper;
import com.example.permission.mapper.RoomTypeMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.permission.entity.table.BuildingTableDef.BUILDING;
import static com.example.permission.entity.table.FloorTableDef.FLOOR;
import static com.example.permission.entity.table.RoomTableDef.ROOM;
import static com.example.permission.entity.table.RoomTypeTableDef.ROOM_TYPE;

@Service
public class HotelDashboardService {

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private FloorMapper floorMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private RoomTypeMapper roomTypeMapper;

    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalBuildings = buildingMapper.selectCountByQuery(
                QueryWrapper.create().from(Building.class).where(BUILDING.DELETED.eq(0)));
        long totalFloors = floorMapper.selectCountByQuery(
                QueryWrapper.create().from(Floor.class).where(FLOOR.DELETED.eq(0)));
        long totalRooms = roomMapper.selectCountByQuery(
                QueryWrapper.create().from(Room.class).where(ROOM.DELETED.eq(0)));
        long activeRoomTypes = roomTypeMapper.selectCountByQuery(
                QueryWrapper.create().from(RoomType.class)
                        .where(ROOM_TYPE.DELETED.eq(0)).and(ROOM_TYPE.SALE_STATUS.eq(1)));
        long availableRooms = roomMapper.selectCountByQuery(
                QueryWrapper.create().from(Room.class)
                        .where(ROOM.DELETED.eq(0)).and(ROOM.STATUS.eq(1)));
        stats.put("totalBuildings", totalBuildings);
        stats.put("totalFloors", totalFloors);
        stats.put("totalRooms", totalRooms);
        stats.put("activeRoomTypes", activeRoomTypes);
        stats.put("availableRooms", availableRooms);
        return stats;
    }

    public List<Map<String, Object>> getRoomStatusStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Room> allRooms = roomMapper.selectListByQuery(
                QueryWrapper.create().from(Room.class).where(ROOM.DELETED.eq(0)));
        Map<Integer, Long> statusCount = new HashMap<>();
        for (Room room : allRooms) {
            statusCount.merge(room.getStatus(), 1L, Long::sum);
        }
        for (Map.Entry<Integer, Long> entry : statusCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getRoomTypeStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<RoomType> types = roomTypeMapper.selectListByQuery(
                QueryWrapper.create().from(RoomType.class).where(ROOM_TYPE.DELETED.eq(0)));
        for (RoomType type : types) {
            long total = roomMapper.selectCountByQuery(
                    QueryWrapper.create().from(Room.class)
                            .where(ROOM.ROOM_TYPE_ID.eq(type.getId()))
                            .and(ROOM.DELETED.eq(0)));
            long available = roomMapper.selectCountByQuery(
                    QueryWrapper.create().from(Room.class)
                            .where(ROOM.ROOM_TYPE_ID.eq(type.getId()))
                            .and(ROOM.DELETED.eq(0))
                            .and(ROOM.STATUS.eq(1)));
            Map<String, Object> item = new HashMap<>();
            item.put("typeId", type.getId());
            item.put("typeName", type.getTypeName());
            item.put("total", total);
            item.put("available", available);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getFloorStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Floor> floors = floorMapper.selectListByQuery(
                QueryWrapper.create().from(Floor.class).where(FLOOR.DELETED.eq(0)));
        for (Floor floor : floors) {
            Building building = buildingMapper.selectOneById(floor.getBuildingId());
            Map<String, Object> item = new HashMap<>();
            item.put("floorId", floor.getId());
            item.put("buildingName", building != null ? building.getBuildingName() : "");
            item.put("floorNumber", floor.getFloorNumber());
            item.put("floorName", floor.getFloorName());
            item.put("roomCount", floor.getRoomCount());
            result.add(item);
        }
        return result;
    }
}
