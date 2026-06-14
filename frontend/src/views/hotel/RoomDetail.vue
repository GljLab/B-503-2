<template>
  <div class="room-detail-container">
    <div class="detail-header">
      <div class="header-left">
        <el-button @click="router.push('/hotel/room')">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <span class="room-number">{{ roomData?.roomNumber || '' }}</span>
        <el-tag v-if="roomData" :type="statusTagType(roomData.status)" size="large">
          {{ statusLabel(roomData?.status) }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button v-if="hasPermission('hotel:room:edit')" type="primary" @click="openEditDialog">
          <el-icon><Edit /></el-icon>编辑房间
        </el-button>
        <el-button v-if="hasPermission('hotel:room:status:edit')" type="warning" @click="openStatusDialog">
          <el-icon><Switch /></el-icon>修改状态
        </el-button>
        <el-button v-if="hasPermission('hotel:room:delete')" type="danger" @click="handleDelete">
          <el-icon><Delete /></el-icon>删除房间
        </el-button>
        <el-button v-if="hasPermission('hotel:room:copy')" type="success" @click="openCopyDialog">
          <el-icon><CopyDocument /></el-icon>复制房间
        </el-button>
      </div>
    </div>

    <el-card v-if="roomData" shadow="never" class="info-card">
      <template #header><span class="card-title">基本信息</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="房号">
          <span class="field-readonly">{{ roomData.roomNumber }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="楼栋">
          <span class="field-readonly">{{ roomData.buildingName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="楼层">
          <span class="field-readonly">{{ roomData.floorName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="房型">
          <span class="field-readonly">{{ roomData.roomTypeName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="朝向">{{ roomData.orientation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="景观">{{ roomData.viewType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="位置特点" :span="2">
          <div v-if="parsedLocationFeatures.length" class="tag-list">
            <el-tag v-for="f in parsedLocationFeatures" :key="f" size="small" type="info" class="round-tag">{{ f }}</el-tag>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="特殊标识" :span="2">
          <div v-if="parsedSpecialTags.length" class="tag-list">
            <el-tag v-for="t in parsedSpecialTags" :key="t" size="small" type="warning" class="round-tag">{{ t }}</el-tag>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="roomTypeInfo" shadow="never" class="info-card">
      <template #header><span class="card-title">房型信息</span></template>
      <div v-if="roomTypeImages.length" class="image-gallery">
        <el-carousel v-if="roomTypeImages.length > 1" height="200px" indicator-position="outside">
          <el-carousel-item v-for="(img, idx) in roomTypeImages" :key="idx">
            <img :src="img" class="gallery-image" />
          </el-carousel-item>
        </el-carousel>
        <img v-else :src="roomTypeImages[0]" class="gallery-image single" />
      </div>
      <el-descriptions :column="2" border class="type-desc">
        <el-descriptions-item label="面积">{{ roomTypeInfo.area }}m²</el-descriptions-item>
        <el-descriptions-item label="床型">{{ bedTypeLabel(roomTypeInfo.bedType) }}</el-descriptions-item>
        <el-descriptions-item label="最大入住">{{ roomTypeInfo.maxOccupancy }}人</el-descriptions-item>
        <el-descriptions-item label="加床政策">{{ roomTypeInfo.extraBedPolicy || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="parsedFacilities.length" class="facilities-section">
        <span class="facilities-label">设施：</span>
        <div class="tag-list">
          <el-tag v-for="f in parsedFacilities" :key="f" size="small" class="round-tag">{{ f }}</el-tag>
        </div>
      </div>
      <el-descriptions :column="2" border class="price-desc">
        <el-descriptions-item label="基础价格">
          <span class="price-text">¥{{ roomTypeInfo.basePrice }}/晚</span>
        </el-descriptions-item>
        <el-descriptions-item label="周末价格">
          <span class="price-text">¥{{ roomTypeInfo.weekendPrice }}/晚</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="hasPermission('hotel:roomType:cost:view')" label="成本价格">
          <span class="price-text cost">¥{{ roomTypeInfo.costPrice }}/晚</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="hasPermission('hotel:room:remark:view') || hasPermission('hotel:room:remark:edit')" shadow="never" class="info-card">
      <template #header>
        <div class="card-header-with-action">
          <span class="card-title">房间备注</span>
          <el-button v-if="hasPermission('hotel:room:remark:edit')" type="primary" link @click="openRemarkDialog">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
        </div>
      </template>
      <p v-if="roomData?.remark" class="remark-text">{{ roomData.remark }}</p>
      <span v-else class="remark-empty">暂无备注</span>
    </el-card>

    <el-card shadow="never" class="info-card">
      <template #header><span class="card-title">状态变更记录</span></template>
      <el-timeline v-if="statusLogs.length">
        <el-timeline-item
          v-for="log in statusLogs"
          :key="log.id"
          :timestamp="log.createTime"
          placement="top"
        >
          <div class="log-content">
            <span class="log-operator">{{ log.operatorName || '系统' }}</span>
            <el-tag :type="statusTagType(log.oldStatus)" size="small">{{ statusLabel(log.oldStatus) }}</el-tag>
            <span class="log-arrow">→</span>
            <el-tag :type="statusTagType(log.newStatus)" size="small">{{ statusLabel(log.newStatus) }}</el-tag>
            <span v-if="log.remark" class="log-remark">{{ log.remark }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无状态变更记录" :image-size="80" />
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑房间"
      width="600px"
      destroy-on-close
    >
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="房间号">
          <el-input :model-value="roomData?.roomNumber" disabled />
        </el-form-item>
        <el-form-item label="楼栋">
          <el-input :model-value="roomData?.buildingName" disabled />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input :model-value="roomData?.floorName" disabled />
        </el-form-item>
        <el-form-item label="房型">
          <el-input :model-value="roomData?.roomTypeName" disabled />
        </el-form-item>
        <el-divider content-position="left">可编辑属性</el-divider>
        <el-form-item label="朝向">
          <el-select v-model="editForm.orientation" placeholder="请选择朝向" clearable style="width: 100%">
            <el-option v-for="o in orientationOptions" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="景观">
          <el-select v-model="editForm.viewType" placeholder="请选择景观" clearable style="width: 100%">
            <el-option v-for="v in viewTypeOptions" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置特征">
          <el-select v-model="editForm.locationFeatures" multiple placeholder="请选择位置特征" style="width: 100%">
            <el-option v-for="f in locationFeatureOptions" :key="f" :label="f" :value="f" />
          </el-select>
        </el-form-item>
        <el-form-item label="特殊标签">
          <el-select v-model="editForm.specialTags" multiple placeholder="请选择特殊标签" style="width: 100%">
            <el-option v-for="t in specialTagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveEdit">保存</el-button>
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
          <el-tag :type="statusTagType(roomData?.status)">{{ statusLabel(roomData?.status) }}</el-tag>
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

    <el-dialog
      v-model="remarkDialogVisible"
      title="编辑备注"
      width="500px"
      destroy-on-close
    >
      <el-input v-model="remarkForm.remark" type="textarea" :rows="5" placeholder="请输入备注" />
      <template #footer>
        <el-button @click="remarkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="remarkSaving" @click="handleSaveRemark">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="copyDialogVisible"
      title="复制房间"
      width="800px"
      destroy-on-close
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="never" class="copy-source-card">
            <template #header><span class="card-title">源房间信息</span></template>
            <el-descriptions :column="1" border size="small" v-if="roomData">
              <el-descriptions-item label="房间号">{{ roomData.roomNumber }}</el-descriptions-item>
              <el-descriptions-item label="楼栋">{{ roomData.buildingName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="楼层">{{ roomData.floorName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="房型">{{ roomData.roomTypeName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="朝向">{{ roomData.orientation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="景观">{{ roomData.viewType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="位置特征">{{ parsedLocationFeatures.join('、') || '-' }}</el-descriptions-item>
              <el-descriptions-item label="特殊标签">{{ parsedSpecialTags.join('、') || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span class="card-title">新房间信息</span></template>
            <el-form ref="copyFormRef" :model="copyForm" :rules="copyRules" label-width="90px">
              <el-form-item label="新房号" prop="roomNumber">
                <el-input v-model="copyForm.roomNumber" placeholder="请输入新房号" />
              </el-form-item>
              <el-form-item label="楼栋" prop="buildingId">
                <el-select v-model="copyForm.buildingId" placeholder="请选择楼栋" style="width: 100%" @change="handleCopyBuildingChange">
                  <el-option v-for="b in copyBuildings" :key="b.id" :label="b.buildingName" :value="b.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="楼层" prop="floorId">
                <el-select v-model="copyForm.floorId" placeholder="请选择楼层" style="width: 100%">
                  <el-option v-for="f in copyFloors" :key="f.id" :label="f.floorName" :value="f.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="朝向">
                <el-select v-model="copyForm.orientation" placeholder="请选择朝向" clearable style="width: 100%">
                  <el-option v-for="o in orientationOptions" :key="o" :label="o" :value="o" />
                </el-select>
              </el-form-item>
              <el-form-item label="景观">
                <el-select v-model="copyForm.viewType" placeholder="请选择景观" clearable style="width: 100%">
                  <el-option v-for="v in viewTypeOptions" :key="v" :label="v" :value="v" />
                </el-select>
              </el-form-item>
              <el-form-item label="位置特征">
                <el-select v-model="copyForm.locationFeatures" multiple placeholder="请选择位置特征" style="width: 100%">
                  <el-option v-for="f in locationFeatureOptions" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="特殊标签">
                <el-select v-model="copyForm.specialTags" multiple placeholder="请选择特殊标签" style="width: 100%">
                  <el-option v-for="t in specialTagOptions" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="copySaving" @click="handleCopyRoom">确认复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Delete, Switch, CopyDocument } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const route = useRoute()
const router = useRouter()
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

const statusTagType = (status) => {
  const map = { 1: 'success', 2: 'warning', 3: 'primary', 4: 'info', 5: 'info', 6: 'danger', 7: 'info' }
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

const roomData = ref(null)
const statusLogs = ref([])
const pageLoading = ref(false)

const roomTypeInfo = computed(() => roomData.value?.roomType || null)

const parsedLocationFeatures = computed(() => parseJsonArray(roomData.value?.locationFeatures))
const parsedSpecialTags = computed(() => parseJsonArray(roomData.value?.specialTags))
const parsedFacilities = computed(() => parseJsonArray(roomTypeInfo.value?.facilities))
const roomTypeImages = computed(() => {
  if (!roomTypeInfo.value) return []
  if (roomTypeInfo.value.images && roomTypeInfo.value.images.length) {
    return roomTypeInfo.value.images.map(img => img.url || img.imageUrl || img)
  }
  if (roomTypeInfo.value.mainImageUrl) return [roomTypeInfo.value.mainImageUrl]
  return []
})

const loadRoom = async () => {
  pageLoading.value = true
  try {
    const id = route.params.id
    const res = await api.hotel.getRoom(id)
    if (res.code === 200 && res.data) {
      roomData.value = res.data
    } else {
      ElMessage.error(res.message || '获取房间详情失败')
    }
  } catch {
    ElMessage.error('获取房间详情失败')
  } finally {
    pageLoading.value = false
  }
}

const loadStatusLogs = async () => {
  try {
    const id = route.params.id
    const res = await api.hotel.getRoomStatusLogs(id)
    if (res.code === 200) {
      statusLogs.value = res.data || []
    }
  } catch {
    statusLogs.value = []
  }
}

const editDialogVisible = ref(false)
const editSaving = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  orientation: '',
  viewType: '',
  locationFeatures: [],
  specialTags: []
})

const openEditDialog = () => {
  editForm.orientation = roomData.value?.orientation || ''
  editForm.viewType = roomData.value?.viewType || ''
  editForm.locationFeatures = parseJsonArray(roomData.value?.locationFeatures)
  editForm.specialTags = parseJsonArray(roomData.value?.specialTags)
  editDialogVisible.value = true
}

const handleSaveEdit = async () => {
  editSaving.value = true
  try {
    const payload = {
      id: roomData.value.id,
      orientation: editForm.orientation || undefined,
      viewType: editForm.viewType || undefined,
      locationFeatures: editForm.locationFeatures.length ? JSON.stringify(editForm.locationFeatures) : undefined,
      specialTags: editForm.specialTags.length ? JSON.stringify(editForm.specialTags) : undefined
    }
    const res = await api.hotel.updateRoom(payload)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      await loadRoom()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch {
    ElMessage.error('更新失败')
  } finally {
    editSaving.value = false
  }
}

const statusDialogVisible = ref(false)
const statusSaving = ref(false)
const statusForm = reactive({
  newStatus: null,
  remark: ''
})

const openStatusDialog = () => {
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
    const res = await api.hotel.updateRoomStatus(roomData.value.id, {
      status: statusForm.newStatus,
      remark: statusForm.remark
    })
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      statusDialogVisible.value = false
      await loadRoom()
      await loadStatusLogs()
    } else {
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch {
    ElMessage.error('状态更新失败')
  } finally {
    statusSaving.value = false
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除房间「${roomData.value.roomNumber}」？删除后不可恢复！`, '警告', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await api.hotel.deleteRoom(roomData.value.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      router.push('/hotel/room')
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {}
}

const remarkDialogVisible = ref(false)
const remarkSaving = ref(false)
const remarkForm = reactive({ remark: '' })

const openRemarkDialog = () => {
  remarkForm.remark = roomData.value?.remark || ''
  remarkDialogVisible.value = true
}

const handleSaveRemark = async () => {
  remarkSaving.value = true
  try {
    const res = await api.hotel.updateRoom({
      id: roomData.value.id,
      remark: remarkForm.remark || undefined
    })
    if (res.code === 200) {
      ElMessage.success('备注更新成功')
      remarkDialogVisible.value = false
      await loadRoom()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch {
    ElMessage.error('更新失败')
  } finally {
    remarkSaving.value = false
  }
}

const copyDialogVisible = ref(false)
const copySaving = ref(false)
const copyFormRef = ref(null)
const copyBuildings = ref([])
const copyFloors = ref([])

const copyForm = reactive({
  roomNumber: '',
  buildingId: null,
  floorId: null,
  orientation: '',
  viewType: '',
  locationFeatures: [],
  specialTags: []
})

const copyRules = {
  roomNumber: [{ required: true, message: '请输入新房号', trigger: 'blur' }],
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  floorId: [{ required: true, message: '请选择楼层', trigger: 'change' }]
}

const openCopyDialog = async () => {
  copyForm.roomNumber = ''
  copyForm.buildingId = roomData.value?.buildingId || null
  copyForm.floorId = roomData.value?.floorId || null
  copyForm.orientation = roomData.value?.orientation || ''
  copyForm.viewType = roomData.value?.viewType || ''
  copyForm.locationFeatures = parseJsonArray(roomData.value?.locationFeatures)
  copyForm.specialTags = parseJsonArray(roomData.value?.specialTags)
  try {
    const res = await api.hotel.getBuildings()
    if (res.code === 200) {
      copyBuildings.value = res.data || []
    }
  } catch {
    copyBuildings.value = []
  }
  if (copyForm.buildingId) {
    try {
      const res = await api.hotel.getFloors(copyForm.buildingId)
      if (res.code === 200) {
        copyFloors.value = res.data || []
      }
    } catch {
      copyFloors.value = []
    }
  } else {
    copyFloors.value = []
  }
  copyDialogVisible.value = true
}

const handleCopyBuildingChange = async () => {
  copyForm.floorId = null
  if (!copyForm.buildingId) {
    copyFloors.value = []
    return
  }
  try {
    const res = await api.hotel.getFloors(copyForm.buildingId)
    if (res.code === 200) {
      copyFloors.value = res.data || []
    }
  } catch {
    copyFloors.value = []
  }
}

const handleCopyRoom = async () => {
  const valid = await copyFormRef.value.validate().catch(() => false)
  if (!valid) return

  copySaving.value = true
  try {
    const payload = {
      roomNumber: copyForm.roomNumber,
      buildingId: copyForm.buildingId,
      floorId: copyForm.floorId,
      orientation: copyForm.orientation || undefined,
      viewType: copyForm.viewType || undefined,
      locationFeatures: copyForm.locationFeatures.length ? JSON.stringify(copyForm.locationFeatures) : undefined,
      specialTags: copyForm.specialTags.length ? JSON.stringify(copyForm.specialTags) : undefined
    }
    const res = await api.hotel.copyRoom(roomData.value.id, payload)
    if (res.code === 200) {
      ElMessage.success('复制成功')
      copyDialogVisible.value = false
      router.push(`/hotel/room/${res.data.id}`)
    } else {
      ElMessage.error(res.message || '复制失败')
    }
  } catch {
    ElMessage.error('复制失败')
  } finally {
    copySaving.value = false
  }
}

onMounted(() => {
  loadRoom()
  loadStatusLogs()
})
</script>

<style scoped>
.room-detail-container {
  padding: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.room-number {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.info-card {
  border-radius: 12px;
  border: none;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.card-header-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-readonly {
  font-weight: 600;
  color: #2d3748;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.round-tag {
  border-radius: 14px;
}

.image-gallery {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
}

.gallery-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.gallery-image.single {
  border-radius: 8px;
}

.type-desc {
  margin-bottom: 12px;
}

.facilities-section {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 8px;
}

.facilities-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
  line-height: 28px;
}

.price-desc {
  margin-top: 4px;
}

.price-text {
  font-size: 15px;
  font-weight: 600;
  color: #e6a23c;
}

.price-text.cost {
  color: #f56c6c;
}

.remark-text {
  margin: 0;
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  white-space: pre-wrap;
}

.remark-empty {
  color: #c0c4cc;
  font-size: 14px;
}

.log-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.log-operator {
  font-weight: 600;
  color: #2d3748;
}

.log-arrow {
  color: #a0aec0;
  font-size: 16px;
}

.log-remark {
  color: #718096;
  font-size: 13px;
}

.copy-source-card {
  border-radius: 12px;
  border: none;
}
</style>
