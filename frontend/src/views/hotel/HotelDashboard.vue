<template>
  <div class="hotel-dashboard-container">
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="28"><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalBuildings }}</div>
              <div class="stat-label">总楼栋数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon :size="28"><CopyDocument /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalFloors }}</div>
              <div class="stat-label">总楼层数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
              <el-icon :size="28"><Key /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalRooms }}</div>
              <div class="stat-label">总房间数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="28"><Ticket /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.activeRoomTypes }}</div>
              <div class="stat-label">在售房型数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="section-row">
      <el-col :xs="24" :sm="12">
        <el-card class="status-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>房间状态分布</span>
              <el-button
                v-if="hasPermission('hotel:dashboard:export')"
                type="primary"
                size="small"
                plain
                @click="handleExport"
              >
                <el-icon><Download /></el-icon>导出价格表
              </el-button>
            </div>
          </template>
          <div class="status-list">
            <div v-for="item in statusStats" :key="item.status" class="status-item">
              <div class="status-label-row">
                <span class="status-dot" :style="{ background: statusColors[item.status] }"></span>
                <span class="status-name">{{ statusLabels[item.status] }}</span>
                <span class="status-count">{{ item.count }} 间</span>
                <span class="status-percent">{{ getStatusPercent(item) }}%</span>
              </div>
              <el-progress
                :percentage="getStatusPercent(item)"
                :color="statusColors[item.status]"
                :show-text="false"
                :stroke-width="10"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12">
        <el-card class="type-card" shadow="hover">
          <template #header>
            <span>房型统计</span>
          </template>
          <el-table :data="roomTypeStats" stripe style="width: 100%">
            <el-table-column prop="typeName" label="房型" min-width="100" />
            <el-table-column prop="total" label="总数" width="70" align="center" />
            <el-table-column prop="available" label="空闲数" width="70" align="center" />
            <el-table-column label="占用率" min-width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="getOccupancy(row)"
                  :color="getOccupancyColor(getOccupancy(row))"
                  :stroke-width="10"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="section-row">
      <el-col :span="24">
        <el-card class="floor-card" shadow="hover">
          <template #header>
            <span>楼层房间分布</span>
          </template>
          <el-table :data="floorStats" stripe style="width: 100%">
            <el-table-column prop="buildingName" label="楼栋" width="140" />
            <el-table-column prop="floorNumber" label="楼层" width="80" align="center" />
            <el-table-column prop="floorName" label="楼层名" width="140" />
            <el-table-column label="房间数" min-width="200">
              <template #default="{ row }">
                <div class="room-bar-wrapper">
                  <div
                    class="room-bar"
                    :style="{ width: getBarWidth(row.roomCount) + '%' }"
                  ></div>
                  <span class="room-bar-count">{{ row.roomCount }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, CopyDocument, Key, Ticket, Download } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const userStore = useUserStore()
const hasPermission = (p) => userStore.hasPermission(p)

const statusLabels = { 1: '空闲', 2: '已预订', 3: '已入住', 4: '待清洁', 5: '清洁中', 6: '维修中', 7: '停用' }
const statusColors = { 1: '#67C23A', 2: '#E6A23C', 3: '#409EFF', 4: '#909399', 5: '#909399', 6: '#F56C6C', 7: '#C0C4CC' }

const overview = ref({
  totalBuildings: 0,
  totalFloors: 0,
  totalRooms: 0,
  activeRoomTypes: 0
})

const statusStats = ref([])
const roomTypeStats = ref([])
const floorStats = ref([])

const totalStatusCount = computed(() => {
  return statusStats.value.reduce((sum, item) => sum + item.count, 0)
})

const getStatusPercent = (item) => {
  if (totalStatusCount.value === 0) return 0
  return Math.round((item.count / totalStatusCount.value) * 100)
}

const getOccupancy = (row) => {
  if (!row.total || row.total === 0) return 0
  return Math.round(((row.total - row.available) / row.total) * 100)
}

const getOccupancyColor = (percentage) => {
  if (percentage < 50) return '#67C23A'
  if (percentage < 80) return '#E6A23C'
  return '#F56C6C'
}

const maxFloorRooms = computed(() => {
  if (floorStats.value.length === 0) return 1
  return Math.max(...floorStats.value.map(f => f.roomCount), 1)
})

const getBarWidth = (count) => {
  return Math.round((count / maxFloorRooms.value) * 100)
}

const loadOverview = async () => {
  try {
    const res = await api.hotel.getDashboardOverview()
    if (res.code === 200 && res.data) {
      overview.value = res.data
    }
  } catch {}
}

const loadStatusStats = async () => {
  try {
    const res = await api.hotel.getRoomStatusStats()
    if (res.code === 200) {
      statusStats.value = res.data || []
    }
  } catch {}
}

const loadRoomTypeStats = async () => {
  try {
    const res = await api.hotel.getRoomTypeStats()
    if (res.code === 200) {
      roomTypeStats.value = res.data || []
    }
  } catch {}
}

const loadFloorStats = async () => {
  try {
    const res = await api.hotel.getFloorStats()
    if (res.code === 200) {
      floorStats.value = res.data || []
    }
  } catch {}
}

const handleExport = () => {
  ElMessage.info('价格表导出功能')
}

onMounted(() => {
  Promise.all([loadOverview(), loadStatusStats(), loadRoomTypeStats(), loadFloorStats()])
})
</script>

<style scoped>
.hotel-dashboard-container {
  padding: 10px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
}

.stat-label {
  font-size: 14px;
  color: #718096;
}

.section-row {
  margin-bottom: 20px;
}

.status-card,
.type-card,
.floor-card {
  border-radius: 12px;
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-name {
  font-size: 14px;
  color: #2d3748;
  min-width: 56px;
}

.status-count {
  font-size: 14px;
  color: #718096;
  flex: 1;
}

.status-percent {
  font-size: 13px;
  color: #a0aec0;
  min-width: 36px;
  text-align: right;
}

.room-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.room-bar {
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #4facfe 0%, #00f2fe 100%);
  transition: width 0.4s ease;
  min-width: 2px;
}

.room-bar-count {
  font-size: 13px;
  color: #4a5568;
  min-width: 28px;
}
</style>
