<template>
  <div class="room-manage-container">
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-total">
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalRooms }}</div>
            <div class="stat-label">总房间数</div>
          </div>
          <el-icon :size="40" class="stat-icon"><House /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-free">
          <div class="stat-content">
            <div class="stat-value">{{ stats.freeRooms }}</div>
            <div class="stat-label">空闲数</div>
          </div>
          <el-icon :size="40" class="stat-icon"><CircleCheck /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-occupied">
          <div class="stat-content">
            <div class="stat-value">{{ stats.occupiedRooms }}</div>
            <div class="stat-label">已入住数</div>
          </div>
          <el-icon :size="40" class="stat-icon"><User /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-maintenance">
          <div class="stat-content">
            <div class="stat-value">{{ stats.maintenanceRooms }}</div>
            <div class="stat-label">维修中</div>
          </div>
          <el-icon :size="40" class="stat-icon"><WarnTriangleFilled /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input v-model="queryParams.roomNumber" placeholder="房间号" clearable style="width: 130px" @keyup.enter="handleSearch" />
        <el-select v-model="queryParams.buildingId" placeholder="楼栋" clearable style="width: 140px" @change="handleBuildingChange">
          <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
        </el-select>
        <el-select v-model="queryParams.floorId" placeholder="楼层" clearable style="width: 120px">
          <el-option v-for="f in filteredFloors" :key="f.id" :label="f.floorName" :value="f.id" />
        </el-select>
        <el-select v-model="queryParams.roomTypeId" placeholder="房型" clearable style="width: 140px">
          <el-option v-for="rt in roomTypes" :key="rt.id" :label="rt.typeName" :value="rt.id" />
        </el-select>
        <el-select v-model="queryParams.statusList" placeholder="状态" multiple clearable collapse-tags style="width: 200px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="queryParams.orientation" placeholder="朝向" clearable style="width: 110px">
          <el-option v-for="o in orientationOptions" :key="o" :label="o" :value="o" />
        </el-select>
        <el-select v-model="queryParams.viewType" placeholder="景观" clearable style="width: 110px">
          <el-option v-for="v in viewTypeOptions" :key="v" :label="v" :value="v" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <el-button v-if="hasPermission('hotel:room:add')" type="primary" @click="openRoomDialog(null)">
            <el-icon><Plus /></el-icon>新增房间
          </el-button>
          <el-button v-if="hasPermission('hotel:room:batch:add')" @click="openBatchDialog">
            <el-icon><CopyDocument /></el-icon>批量创建
          </el-button>
        </div>
      </div>
      <el-table :data="tableData" stripe border v-loading="tableLoading" style="width: 100%">
        <el-table-column prop="roomNumber" label="房号" width="110" align="center" />
        <el-table-column prop="buildingName" label="楼栋" width="120" align="center" />
        <el-table-column prop="floorName" label="楼层" width="100" align="center" />
        <el-table-column prop="roomTypeName" label="房型" min-width="140" />
        <el-table-column prop="orientation" label="朝向" width="90" align="center">
          <template #default="{ row }">{{ row.orientation || '-' }}</template>
        </el-table-column>
        <el-table-column prop="viewType" label="景观" width="90" align="center">
          <template #default="{ row }">{{ row.viewType || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">查看详情</el-button>
            <el-button v-if="hasPermission('hotel:room:edit')" type="primary" link size="small" @click="openRoomDialog(row)">编辑</el-button>
            <el-button v-if="hasPermission('hotel:room:status:edit')" type="warning" link size="small" @click="openStatusDialog(row)">修改状态</el-button>
            <el-button v-if="hasPermission('hotel:room:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadRooms"
          @current-change="loadRooms"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="roomDialogVisible"
      :title="roomForm.id ? '编辑房间' : '新增房间'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="100px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="房间号" prop="roomNumber">
          <el-input v-model="roomForm.roomNumber" placeholder="请输入房间号" :disabled="!!roomForm.id" />
        </el-form-item>
        <el-form-item label="楼栋" prop="buildingId">
          <el-select v-model="roomForm.buildingId" placeholder="请选择楼栋" style="width: 100%" :disabled="!!roomForm.id" @change="handleFormBuildingChange">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层" prop="floorId">
          <el-select v-model="roomForm.floorId" placeholder="请选择楼层" style="width: 100%" :disabled="!!roomForm.id">
            <el-option v-for="f in formFloors" :key="f.id" :label="f.floorName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房型" prop="roomTypeId">
          <el-select v-model="roomForm.roomTypeId" placeholder="请选择房型" style="width: 100%" :disabled="!!roomForm.id" @change="handleFormRoomTypeChange">
            <el-option v-for="rt in roomTypes" :key="rt.id" :label="rt.typeName" :value="rt.id" />
          </el-select>
        </el-form-item>
        <div v-if="selectedRoomTypeInfo" class="room-type-info-box">
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="面积">{{ selectedRoomTypeInfo.area }}m²</el-descriptions-item>
            <el-descriptions-item label="床型">{{ bedTypeLabel(selectedRoomTypeInfo.bedType) }}</el-descriptions-item>
            <el-descriptions-item label="最大入住">{{ selectedRoomTypeInfo.maxOccupancy }}人</el-descriptions-item>
          </el-descriptions>
        </div>

        <el-divider content-position="left">房间属性</el-divider>
        <el-form-item label="朝向" prop="orientation">
          <el-select v-model="roomForm.orientation" placeholder="请选择朝向" clearable style="width: 100%">
            <el-option v-for="o in orientationOptions" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="景观" prop="viewType">
          <el-select v-model="roomForm.viewType" placeholder="请选择景观" clearable style="width: 100%">
            <el-option v-for="v in viewTypeOptions" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置特征" prop="locationFeatures">
          <el-select v-model="roomForm.locationFeatures" multiple placeholder="请选择位置特征" style="width: 100%">
            <el-option v-for="f in locationFeatureOptions" :key="f" :label="f" :value="f" />
          </el-select>
        </el-form-item>
        <el-form-item label="特殊标签" prop="specialTags">
          <el-select v-model="roomForm.specialTags" multiple placeholder="请选择特殊标签" style="width: 100%">
            <el-option v-for="t in specialTagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <template v-if="hasPermission('hotel:room:remark:view') || hasPermission('hotel:room:remark:edit')">
          <el-divider content-position="left">备注</el-divider>
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="roomForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
              :disabled="!hasPermission('hotel:room:remark:edit')"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roomSaving" @click="handleSaveRoom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchDialogVisible"
      title="批量创建房间"
      width="700px"
      destroy-on-close
    >
      <el-steps :active="batchStep" finish-status="success" class="batch-steps">
        <el-step title="选择位置与房型" />
        <el-step title="设置房号规则" />
        <el-step title="默认属性" />
      </el-steps>

      <div v-if="batchStep === 0" class="batch-section">
        <el-form label-width="100px">
          <el-form-item label="楼栋" required>
            <el-select v-model="batchForm.buildingId" placeholder="请选择楼栋" style="width: 100%" @change="handleBatchBuildingChange">
              <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层" required>
            <el-select v-model="batchForm.floorId" placeholder="请选择楼层" style="width: 100%">
              <el-option v-for="f in batchFloors" :key="f.id" :label="f.floorName" :value="f.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="房型" required>
            <el-select v-model="batchForm.roomTypeId" placeholder="请选择房型" style="width: 100%">
              <el-option v-for="rt in roomTypes" :key="rt.id" :label="rt.typeName" :value="rt.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="batchStep === 1" class="batch-section">
        <el-form label-width="100px">
          <el-form-item label="号码前缀">
            <el-input v-model="batchForm.numberPrefix" placeholder="例如: 2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="起始号" required>
            <el-input v-model="batchForm.startNum" placeholder="例如: 01" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束号" required>
            <el-input v-model="batchForm.endNum" placeholder="例如: 30" style="width: 100%" />
          </el-form-item>
        </el-form>
        <div v-if="batchPreviewNumbers.length > 0" class="batch-preview">
          <div class="batch-preview-title">预览房号 ({{ batchPreviewNumbers.length }}间)</div>
          <div class="batch-preview-list">
            <el-tag v-for="num in batchPreviewNumbers" :key="num" size="small" class="preview-tag">{{ num }}</el-tag>
          </div>
        </div>
      </div>

      <div v-if="batchStep === 2" class="batch-section">
        <el-form label-width="100px">
          <el-form-item label="朝向">
            <el-select v-model="batchForm.orientation" placeholder="请选择朝向" clearable style="width: 100%">
              <el-option v-for="o in orientationOptions" :key="o" :label="o" :value="o" />
            </el-select>
          </el-form-item>
          <el-form-item label="景观">
            <el-select v-model="batchForm.viewType" placeholder="请选择景观" clearable style="width: 100%">
              <el-option v-for="v in viewTypeOptions" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="batchResult" class="batch-result">
        <el-alert
          :title="`成功创建 ${batchResult.successCount} 间房间`"
          type="success"
          show-icon
          :closable="false"
        />
        <div v-if="batchResult.failures && batchResult.failures.length > 0" class="batch-failures">
          <div class="batch-failures-title">失败列表：</div>
          <el-table :data="batchResult.failures" size="small" border>
            <el-table-column prop="roomNumber" label="房号" width="140" />
            <el-table-column prop="reason" label="原因" />
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="batchDialogVisible = false">关闭</el-button>
        <el-button v-if="batchStep > 0 && !batchResult" @click="batchStep--">上一步</el-button>
        <el-button v-if="batchStep < 2" type="primary" @click="batchStep++">下一步</el-button>
        <el-button v-if="batchStep === 2 && !batchResult" type="primary" :loading="batchSaving" @click="handleBatchCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="statusDialogVisible"
      title="修改房间状态"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="当前状态">
          <el-tag :type="statusTagType(statusForm.currentStatus)">{{ statusLabel(statusForm.currentStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态" required>
          <el-select v-model="statusForm.newStatus" placeholder="请选择新状态" style="width: 100%">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="statusForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusSaving" @click="handleUpdateStatus">确认</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      :title="detailData ? `房间详情 - ${detailData.roomNumber}` : '房间详情'"
      size="520px"
      destroy-on-close
    >
      <div v-if="detailData" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="房间号">{{ detailData.roomNumber }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)" size="small">{{ statusLabel(detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="楼栋">{{ detailData.buildingName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="楼层">{{ detailData.floorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房型">{{ detailData.roomTypeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="朝向">{{ detailData.orientation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="景观">{{ detailData.viewType || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="parsedLocationFeatures.length > 0" class="detail-section">
          <h4 class="section-title">位置特征</h4>
          <div class="tag-list">
            <el-tag v-for="f in parsedLocationFeatures" :key="f" size="small" type="info" class="info-tag">{{ f }}</el-tag>
          </div>
        </div>

        <div v-if="parsedSpecialTags.length > 0" class="detail-section">
          <h4 class="section-title">特殊标签</h4>
          <div class="tag-list">
            <el-tag v-for="t in parsedSpecialTags" :key="t" size="small" type="warning" class="info-tag">{{ t }}</el-tag>
          </div>
        </div>

        <div v-if="detailData.remark && (hasPermission('hotel:room:remark:view') || hasPermission('hotel:room:remark:edit'))" class="detail-section">
          <h4 class="section-title">备注</h4>
          <p class="detail-remark">{{ detailData.remark }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, House, User, CircleCheck, WarnTriangleFilled,
  CopyDocument
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const userStore = useUserStore()
const hasPermission = (p) => userStore.hasPermission(p)

const statusOptions = [
  { value: 1, label: '空闲' },
  { value: 2, label: '已预订' },
  { value: 3, label: '已入住' },
  { value: 4, label: '待清洁' },
  { value: 5, label: '清洁中' },
  { value: 6, label: '维修中' },
  { value: 7, label: '停用' }
]

const orientationOptions = ['东', '南', '西', '北', '东南', '东北', '西南', '西北']
const viewTypeOptions = ['江景', '海景', '山景', '园景', '城景']
const locationFeatureOptions = ['靠近电梯', '转角房', '安静区域', '靠近楼梯', '高楼层', '低楼层']
const specialTagOptions = ['残疾人房', '连通房', '禁烟房', '允许宠物', '行政房', 'VIP房']

const stats = reactive({
  totalRooms: 0,
  freeRooms: 0,
  occupiedRooms: 0,
  maintenanceRooms: 0
})

const buildings = ref([])
const allFloors = ref([])
const roomTypes = ref([])

const tableData = ref([])
const total = ref(0)
const tableLoading = ref(false)

const queryParams = reactive({
  roomNumber: '',
  buildingId: null,
  floorId: null,
  roomTypeId: null,
  statusList: [],
  orientation: '',
  viewType: '',
  pageNum: 1,
  pageSize: 10
})

const filteredFloors = computed(() => {
  if (!queryParams.buildingId) return allFloors.value
  return allFloors.value.filter(f => f.buildingId === queryParams.buildingId)
})

const statusTagType = (status) => {
  const map = { 1: 'success', 2: 'warning', 3: '', 4: 'info', 5: 'info', 6: 'danger', 7: 'info' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { 1: '空闲', 2: '已预订', 3: '已入住', 4: '待清洁', 5: '清洁中', 6: '维修中', 7: '停用' }
  return map[status] || '未知'
}

const bedTypeLabel = (type) => {
  const map = { single: '单人床', king: '大床', twin: '双床' }
  return map[type] || type || '-'
}

const parseJsonArray = (val) => {
  if (!val) return []
  if (Array.isArray(val)) return val
  try {
    const parsed = JSON.parse(val)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const parsedLocationFeatures = computed(() => parseJsonArray(detailData.value?.locationFeatures))
const parsedSpecialTags = computed(() => parseJsonArray(detailData.value?.specialTags))

const loadStats = async () => {
  try {
    const res = await api.hotel.getDashboardOverview()
    if (res.code === 200 && res.data) {
      const d = res.data
      stats.totalRooms = d.totalRooms || 0
      stats.freeRooms = d.freeRooms || 0
      stats.occupiedRooms = d.occupiedRooms || 0
      stats.maintenanceRooms = d.maintenanceRooms || 0
    }
  } catch {}
}

const loadBuildings = async () => {
  try {
    const res = await api.hotel.getBuildings()
    if (res.code === 200) {
      buildings.value = res.data || []
      const floors = []
      for (const b of buildings.value) {
        if (b.floors && b.floors.length) {
          for (const f of b.floors) {
            floors.push({ ...f, buildingId: b.id })
          }
        }
      }
      allFloors.value = floors
    }
  } catch {
    buildings.value = []
    allFloors.value = []
  }
}

const loadRoomTypes = async () => {
  try {
    const res = await api.hotel.getRoomTypes()
    if (res.code === 200) {
      roomTypes.value = res.data || []
    }
  } catch {
    roomTypes.value = []
  }
}

const loadRooms = async () => {
  tableLoading.value = true
  try {
    const params = { ...queryParams }
    if (params.statusList && params.statusList.length > 0) {
      params.statusList = params.statusList.join(',')
    } else {
      delete params.statusList
    }
    const res = await api.hotel.getRoomPage(params)
    if (res.code === 200) {
      tableData.value = res.data?.records || res.data?.list || []
      total.value = res.data?.total || 0
    }
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    tableLoading.value = false
  }
}

const handleBuildingChange = () => {
  queryParams.floorId = null
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadRooms()
}

const handleReset = () => {
  queryParams.roomNumber = ''
  queryParams.buildingId = null
  queryParams.floorId = null
  queryParams.roomTypeId = null
  queryParams.statusList = []
  queryParams.orientation = ''
  queryParams.viewType = ''
  queryParams.pageNum = 1
  loadRooms()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除房间「${row.roomNumber}」？`, '提示', { type: 'warning' })
    const res = await api.hotel.deleteRoom(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadRooms()
      await loadStats()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {}
}

const detailVisible = ref(false)
const detailData = ref(null)

const handleViewDetail = async (row) => {
  try {
    const res = await api.hotel.getRoom(row.id)
    if (res.code === 200 && res.data) {
      detailData.value = res.data
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('获取房间详情失败')
  }
}

const roomDialogVisible = ref(false)
const roomSaving = ref(false)
const roomFormRef = ref(null)
const formFloors = ref([])

const roomForm = reactive({
  id: null,
  roomNumber: '',
  buildingId: null,
  floorId: null,
  roomTypeId: null,
  orientation: '',
  viewType: '',
  locationFeatures: [],
  specialTags: [],
  remark: ''
})

const roomRules = {
  roomNumber: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  floorId: [{ required: true, message: '请选择楼层', trigger: 'change' }],
  roomTypeId: [{ required: true, message: '请选择房型', trigger: 'change' }]
}

const selectedRoomTypeInfo = computed(() => {
  if (!roomForm.roomTypeId) return null
  return roomTypes.value.find(rt => rt.id === roomForm.roomTypeId) || null
})

const handleFormBuildingChange = () => {
  roomForm.floorId = null
  loadFormFloors(roomForm.buildingId)
}

const loadFormFloors = async (buildingId) => {
  if (!buildingId) {
    formFloors.value = []
    return
  }
  try {
    const res = await api.hotel.getFloors(buildingId)
    if (res.code === 200) {
      formFloors.value = res.data || []
    }
  } catch {
    formFloors.value = []
  }
}

const handleFormRoomTypeChange = () => {}

const openRoomDialog = async (row) => {
  if (row) {
    Object.assign(roomForm, {
      id: row.id,
      roomNumber: row.roomNumber || '',
      buildingId: row.buildingId || null,
      floorId: row.floorId || null,
      roomTypeId: row.roomTypeId || null,
      orientation: row.orientation || '',
      viewType: row.viewType || '',
      locationFeatures: parseJsonArray(row.locationFeatures),
      specialTags: parseJsonArray(row.specialTags),
      remark: row.remark || ''
    })
    if (row.buildingId) {
      await loadFormFloors(row.buildingId)
    }
  } else {
    Object.assign(roomForm, {
      id: null,
      roomNumber: '',
      buildingId: null,
      floorId: null,
      roomTypeId: null,
      orientation: '',
      viewType: '',
      locationFeatures: [],
      specialTags: [],
      remark: ''
    })
    formFloors.value = []
  }
  roomDialogVisible.value = true
}

const handleSaveRoom = async () => {
  const valid = await roomFormRef.value.validate().catch(() => false)
  if (!valid) return

  roomSaving.value = true
  try {
    const payload = {
      roomNumber: roomForm.roomNumber,
      buildingId: roomForm.buildingId,
      floorId: roomForm.floorId,
      roomTypeId: roomForm.roomTypeId,
      orientation: roomForm.orientation || undefined,
      viewType: roomForm.viewType || undefined,
      locationFeatures: roomForm.locationFeatures.length ? JSON.stringify(roomForm.locationFeatures) : undefined,
      specialTags: roomForm.specialTags.length ? JSON.stringify(roomForm.specialTags) : undefined,
      remark: roomForm.remark || undefined
    }
    let res
    if (roomForm.id) {
      res = await api.hotel.updateRoom({ id: roomForm.id, ...payload })
    } else {
      res = await api.hotel.addRoom(payload)
    }
    if (res.code === 200) {
      ElMessage.success(roomForm.id ? '更新成功' : '新增成功')
      roomDialogVisible.value = false
      detailVisible.value = false
      await loadRooms()
      await loadStats()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    roomSaving.value = false
  }
}

const batchDialogVisible = ref(false)
const batchStep = ref(0)
const batchSaving = ref(false)
const batchResult = ref(null)
const batchFloors = ref([])

const batchForm = reactive({
  buildingId: null,
  floorId: null,
  roomTypeId: null,
  numberPrefix: '',
  startNum: '',
  endNum: '',
  orientation: '',
  viewType: ''
})

const batchPreviewNumbers = computed(() => {
  const start = parseInt(batchForm.startNum, 10)
  const end = parseInt(batchForm.endNum, 10)
  if (isNaN(start) || isNaN(end) || start > end) return []
  const prefix = batchForm.numberPrefix || ''
  const numbers = []
  for (let i = start; i <= end; i++) {
    numbers.push(prefix + String(i).padStart(batchForm.startNum.length, '0'))
  }
  return numbers
})

const openBatchDialog = () => {
  batchStep.value = 0
  batchResult.value = null
  Object.assign(batchForm, {
    buildingId: null,
    floorId: null,
    roomTypeId: null,
    numberPrefix: '',
    startNum: '',
    endNum: '',
    orientation: '',
    viewType: ''
  })
  batchFloors.value = []
  batchDialogVisible.value = true
}

const handleBatchBuildingChange = async () => {
  batchForm.floorId = null
  if (!batchForm.buildingId) {
    batchFloors.value = []
    return
  }
  try {
    const res = await api.hotel.getFloors(batchForm.buildingId)
    if (res.code === 200) {
      batchFloors.value = res.data || []
    }
  } catch {
    batchFloors.value = []
  }
}

const handleBatchCreate = async () => {
  if (!batchForm.buildingId || !batchForm.floorId || !batchForm.roomTypeId) {
    ElMessage.warning('请先选择楼栋、楼层和房型')
    return
  }
  if (!batchForm.startNum || !batchForm.endNum) {
    ElMessage.warning('请输入起始号和结束号')
    return
  }

  batchSaving.value = true
  try {
    const payload = {
      buildingId: batchForm.buildingId,
      floorId: batchForm.floorId,
      roomTypeId: batchForm.roomTypeId,
      numberPrefix: batchForm.numberPrefix || '',
      startNum: parseInt(batchForm.startNum, 10),
      endNum: parseInt(batchForm.endNum, 10),
      orientation: batchForm.orientation || undefined,
      viewType: batchForm.viewType || undefined
    }
    const res = await api.hotel.batchCreateRooms(payload)
    if (res.code === 200) {
      batchResult.value = res.data || { successCount: batchPreviewNumbers.value.length, failures: [] }
      ElMessage.success('批量创建完成')
      await loadRooms()
      await loadStats()
    } else {
      ElMessage.error(res.message || '批量创建失败')
    }
  } catch {
    ElMessage.error('批量创建失败')
  } finally {
    batchSaving.value = false
  }
}

const statusDialogVisible = ref(false)
const statusSaving = ref(false)
const statusForm = reactive({
  roomId: null,
  currentStatus: null,
  newStatus: null,
  remark: ''
})

const openStatusDialog = (row) => {
  statusForm.roomId = row.id
  statusForm.currentStatus = row.status
  statusForm.newStatus = null
  statusForm.remark = ''
  statusDialogVisible.value = true
}

const handleUpdateStatus = async () => {
  if (!statusForm.newStatus) {
    ElMessage.warning('请选择新状态')
    return
  }
  statusSaving.value = true
  try {
    const res = await api.hotel.updateRoomStatus(statusForm.roomId, {
      status: statusForm.newStatus,
      remark: statusForm.remark
    })
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      statusDialogVisible.value = false
      await loadRooms()
      await loadStats()
    } else {
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch {
    ElMessage.error('状态更新失败')
  } finally {
    statusSaving.value = false
  }
}

onMounted(() => {
  loadBuildings()
  loadRoomTypes()
  loadRooms()
  loadStats()
})
</script>

<style scoped>
.room-manage-container {
  padding: 10px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  cursor: default;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #718096;
  margin-top: 4px;
}

.stat-icon {
  opacity: 0.15;
}

.stat-total .stat-value {
  color: #667eea;
}
.stat-total .stat-icon {
  color: #667eea;
}

.stat-free .stat-value {
  color: #67c23a;
}
.stat-free .stat-icon {
  color: #67c23a;
}

.stat-occupied .stat-value {
  color: #409eff;
}
.stat-occupied .stat-icon {
  color: #409eff;
}

.stat-maintenance .stat-value {
  color: #f56c6c;
}
.stat-maintenance .stat-icon {
  color: #f56c6c;
}

.filter-card {
  border-radius: 12px;
  border: none;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-card {
  border-radius: 12px;
  border: none;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-toolbar-left {
  display: flex;
  gap: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.room-type-info-box {
  margin: -10px 0 10px 100px;
  padding: 10px;
  background: #f4f6fb;
  border-radius: 8px;
}

.batch-steps {
  margin-bottom: 24px;
}

.batch-section {
  min-height: 200px;
}

.batch-preview {
  margin-top: 16px;
}

.batch-preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 8px;
}

.batch-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 160px;
  overflow-y: auto;
}

.preview-tag {
  border-radius: 4px;
}

.batch-result {
  margin-top: 20px;
}

.batch-failures {
  margin-top: 12px;
}

.batch-failures-title {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 8px;
}

.detail-content {
  padding: 0 4px;
}

.detail-section {
  margin-top: 20px;
}

.section-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.info-tag {
  border-radius: 14px;
}

.detail-remark {
  margin: 0;
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
