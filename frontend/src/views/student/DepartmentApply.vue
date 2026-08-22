<template>
  <div class="score-apply-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>综合测评申报</h2>
        <p>个人证书申请、部门学生加减分申报及审核</p>
      </div>

      <el-button :loading="loadingPermission" @click="loadPermission"> 刷新 </el-button>
    </div>

    <!-- ====================================================== -->
    <!-- 我的部门身份 -->
    <!-- ====================================================== -->

    <el-card class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>我的部门身份</h3>
          <p>显示你当前加入的部门以及担任的职务。</p>
        </div>

        <el-button size="small" :loading="loadingPermission" @click="loadPermission">
          刷新
        </el-button>
      </div>

      <div v-if="permission.departments.length > 0" class="department-list">
        <div
          v-for="item in permission.departments"
          :key="item.departmentId"
          class="department-item"
        >
          <div class="department-info">
            <div class="department-name">
              {{ item.departmentName }}
            </div>

            <el-tag
              :type="
                item.position === '部长'
                  ? 'danger'
                  : item.position === '副部长'
                    ? 'warning'
                    : 'primary'
              "
            >
              {{ item.position }}
            </el-tag>
          </div>

          <div class="department-tip">
            <span v-if="item.position === '干事'"> 你可以提交本部门学生加减分申报 </span>

            <span v-else> 你可以提交本部门学生加减分申报，并审核本部门其他成员提交的申报 </span>
          </div>
        </div>
      </div>

      <el-empty v-else description="当前没有部门干部身份" />
    </el-card>

    <!-- ====================================================== -->
    <!-- 部门申报审核 -->
    <!-- ====================================================== -->

    <el-card v-if="permission.canDepartmentAudit" class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>部门申报审核</h3>
          <p>你是副部长或部长，可以审核本部门其他干部提交的学生加减分申报。</p>
        </div>

        <el-button size="small" :loading="auditLoading" @click="loadAuditList"> 刷新 </el-button>
      </div>

      <el-table v-loading="auditLoading" :data="auditList" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="title" label="加减分项目" min-width="180" />

        <el-table-column label="被加减分学生" width="130" align="center">
          <template #default="{ row }">
            {{ row.studentName || row.studentId }}
          </template>
        </el-table-column>

        <el-table-column label="部门" width="140" align="center">
          <template #default="{ row }">
            {{ row.departmentName || row.departmentId }}
          </template>
        </el-table-column>

        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="Number(row.scoreType) === 1" type="success"> 加分 </el-tag>
            <el-tag v-else type="danger"> 减分 </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="分值" width="90" align="center">
          <template #default="{ row }">
            <span :class="Number(row.scoreType) === 1 ? 'bonus' : 'deduct'">
              {{ Number(row.scoreType) === 1 ? '+' : '-' }}{{ row.score }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          prop="description"
          label="项目说明"
          min-width="240"
          show-overflow-tooltip
        />

        <el-table-column prop="createTime" label="申报时间" width="170" />

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="auditApply(row, 1)"> 通过 </el-button>
            <el-button type="danger" size="small" @click="auditApply(row, 2)"> 驳回 </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!auditLoading && auditList.length === 0" description="暂无待审核部门申报" />
    </el-card>

    <!-- ====================================================== -->
    <!-- 部门学生加减分申报 【保留唯一一份部门表单，带v-if权限判断】 -->
    <!-- ====================================================== -->

    <el-card v-if="permission.canDepartmentApply" class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>部门学生加减分申报</h3>
          <p>部门干部可以根据本部门加减分模板，对学生进行加分或扣分申报。</p>
        </div>
        <el-tag type="success"> 部门审核 → 辅导员审核 </el-tag>
      </div>

      <el-form
        ref="departmentFormRef"
        :model="departmentForm"
        :rules="departmentRules"
        label-width="110px"
        class="apply-form"
      >
        <!-- 所属部门 -->
        <el-form-item label="所属部门" prop="departmentId">
          <el-select
            v-model="departmentForm.departmentId"
            placeholder="请选择申报部门"
            style="width: 100%"
          >
            <el-option
              v-for="item in permission.departments"
              :key="item.departmentId"
              :label="`${item.departmentName}（${item.position}）`"
              :value="item.departmentId"
            />
          </el-select>
        </el-form-item>

        <!-- 被加减分学生 -->
        <el-form-item label="被加减分学生" prop="studentId">
          <el-select
            v-model="departmentForm.studentId"
            filterable
            clearable
            placeholder="请选择需要加分或扣分的学生"
            style="width: 100%"
          >
            <el-option
              v-for="item in studentList"
              :key="item.id"
              :label="`${item.realName || item.username}（${item.studentNo || item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 部门模板 -->
        <el-form-item label="加减分项目" prop="templateId">
          <el-select
            v-model="departmentForm.templateId"
            filterable
            clearable
            :disabled="!departmentForm.departmentId"
            placeholder="请先选择部门，再选择加减分项目"
            style="width: 100%"
          >
            <el-option
              v-for="item in departmentTemplateList"
              :key="item.id"
              :label="`${item.name}（${Number(item.scoreType) === 1 ? '+' : '-'}${item.score}分）`"
              :value="item.id"
            />
          </el-select>
          <div class="form-tip">这里只显示当前申报部门自己的加减分模板。</div>
        </el-form-item>

        <!-- 模板详情 -->
        <el-form-item label="项目类型">
          <el-tag
            v-if="selectedTemplate"
            :type="Number(selectedTemplate.scoreType) === 1 ? 'success' : 'danger'"
          >
            {{ Number(selectedTemplate.scoreType) === 1 ? '加分' : '减分' }}
          </el-tag>
          <span v-if="selectedTemplate" class="template-score">
            {{ Number(selectedTemplate.scoreType) === 1 ? '+' : '-'
            }}{{ selectedTemplate.score }} 分
          </span>
          <span v-if="!selectedTemplate" class="empty-template"> 请先选择加减分项目 </span>
        </el-form-item>

        <!-- 模板项目说明 -->
        <el-form-item label="项目说明">
          <el-input
            :model-value="selectedTemplate?.description || ''"
            type="textarea"
            :rows="5"
            readonly
            placeholder="选择加减分项目后自动显示项目说明"
          />
        </el-form-item>

        <!-- 证明材料 -->
        <el-form-item label="活动凭证">
          <el-upload
            action=""
            :auto-upload="false"
            :limit="1"
            :on-change="handleDepartmentFile"
            :on-remove="removeDepartmentFile"
          >
            <el-button type="primary"> 选择凭证 </el-button>
            <template #tip>
              <div class="upload-tip">可上传活动证明、照片、文件等材料</div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 提交 -->
        <el-form-item>
          <el-button type="success" :loading="departmentSubmitting" @click="submitDepartment">
            提交部门加减分申报
          </el-button>
          <el-button @click="resetDepartment"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ====================================================== -->
    <!-- 我的申请 -->
    <!-- ====================================================== -->

    <el-card class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>我的申报记录</h3>
          <p>分别查看个人证书申请和部门学生加减分申报。</p>
        </div>

        <el-button size="small" :loading="listLoading" @click="loadMyApply"> 刷新 </el-button>
      </div>

      <el-tabs v-model="activeApplyTab">
        <!-- ================= 个人证书 ================= -->
        <el-tab-pane label="个人证书申请" name="certificate">
          <el-table v-loading="listLoading" :data="certificateApplyList" border stripe>
            <el-table-column type="index" label="#" width="60" align="center" />

            <el-table-column prop="title" label="证书/加分项目" min-width="200" />

            <el-table-column
              prop="description"
              label="申报说明"
              min-width="240"
              show-overflow-tooltip
            />

            <el-table-column prop="applyScore" label="申请分值" width="100" align="center">
              <template #default="{ row }">
                <span class="bonus"> +{{ row.applyScore }} </span>
              </template>
            </el-table-column>

            <el-table-column prop="createTime" label="申报时间" width="170" />

            <el-table-column label="审核状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="Number(row.status) === 0" type="warning"> 待审核 </el-tag>

                <el-tag v-else-if="Number(row.status) === 1" type="success"> 审核通过 </el-tag>

                <el-tag v-else type="danger"> 已驳回 </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              prop="reviewRemark"
              label="审核意见"
              min-width="200"
              show-overflow-tooltip
            />
          </el-table>

          <el-empty
            v-if="!listLoading && certificateApplyList.length === 0"
            description="暂无个人证书申请"
          />
        </el-tab-pane>

        <!-- ================= 部门加减分 ================= -->
        <el-tab-pane label="部门学生加减分申报" name="department">
          <el-table v-loading="listLoading" :data="departmentApplyList" border stripe>
            <el-table-column type="index" label="#" width="60" align="center" />

            <el-table-column prop="title" label="加减分项目" min-width="200" />

            <el-table-column label="被加减分学生" width="140" align="center">
              <template #default="{ row }">
                {{ row.studentName || row.studentId }}
              </template>
            </el-table-column>

            <el-table-column label="所属部门" width="140" align="center">
              <template #default="{ row }">
                {{ row.departmentName || row.departmentId }}
              </template>
            </el-table-column>

            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="Number(row.scoreType) === 1" type="success"> 加分 </el-tag>

                <el-tag v-else type="danger"> 减分 </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="分值" width="90" align="center">
              <template #default="{ row }">
                <span :class="Number(row.scoreType) === 1 ? 'bonus' : 'deduct'">
                  {{ Number(row.scoreType) === 1 ? '+' : '-' }}{{ row.score }}
                </span>
              </template>
            </el-table-column>

            <el-table-column
              prop="description"
              label="项目说明"
              min-width="240"
              show-overflow-tooltip
            />

            <el-table-column prop="createTime" label="申报时间" width="170" />

            <el-table-column label="审核状态" width="150" align="center">
              <template #default="{ row }">
                <el-tag v-if="Number(row.status) === 0" type="warning"> 待部门审核 </el-tag>

                <el-tag v-else-if="Number(row.status) === 2" type="danger"> 部门已驳回 </el-tag>

                <el-tag
                  v-else-if="Number(row.status) === 1 && Number(row.finalStatus) === 0"
                  type="warning"
                >
                  待辅导员审核
                </el-tag>

                <el-tag
                  v-else-if="Number(row.status) === 1 && Number(row.finalStatus) === 1"
                  type="success"
                >
                  审核通过
                </el-tag>

                <el-tag
                  v-else-if="Number(row.status) === 1 && Number(row.finalStatus) === 2"
                  type="danger"
                >
                  辅导员已驳回
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              prop="reviewRemark"
              label="部门审核意见"
              min-width="180"
              show-overflow-tooltip
            />

            <el-table-column
              prop="finalReviewRemark"
              label="辅导员审核意见"
              min-width="180"
              show-overflow-tooltip
            />
          </el-table>

          <el-empty
            v-if="!listLoading && departmentApplyList.length === 0"
            description="暂无部门学生加减分申报"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

/* ========== 权限 ========== */
const loadingPermission = ref(false)
const permission = reactive({
  canDepartmentApply: false,
  canDepartmentAudit: false,
  departments: [],
})

/* ========== 学生列表 ========== */
const studentList = ref([])

/* ========== 个人证书 ========== */
const certificateFormRef = ref(null)
const certificateSubmitting = ref(false)
const certificateFile = ref(null)
const certificateForm = reactive({
  ruleId: null,
  applyScore: null,
  description: '',
})
const certificateRules = {
  ruleId: [{ required: true, message: '请选择加分项目', trigger: 'change' }],
  applyScore: [{ required: true, message: '请输入申请分值', trigger: 'blur' }],
  description: [{ required: true, message: '请输入申报说明', trigger: 'blur' }],
}
const scoreRules = ref([])

/* ========== 部门加减分 ========== */
const departmentFormRef = ref(null)
const departmentSubmitting = ref(false)
const departmentFile = ref(null)
const departmentTemplateList = ref([])
const templateLoading = ref(false)

const departmentForm = reactive({
  departmentId: null,
  templateId: null,
  studentId: null,
  evidenceUrl: '',
})

const departmentRules = {
  departmentId: [
    {
      required: true,
      message: '请选择部门',
      trigger: 'change',
    },
  ],

  studentId: [
    {
      required: true,
      message: '请选择需要加减分的学生',
      trigger: 'change',
    },
  ],

  templateId: [
    {
      required: true,
      message: '请选择加减分项目',
      trigger: 'change',
    },
  ],
}

const selectedTemplate = computed(() => {
  return departmentTemplateList.value.find((item) => item.id === departmentForm.templateId)
})

/* ========== 我的申请 ========== */
const listLoading = ref(false)
const myApplyList = ref([])
const activeApplyTab = ref('certificate')

const certificateApplyList = ref([])
const departmentApplyList = ref([])
/* ========== 部门审核 ========== */
const auditLoading = ref(false)
const auditList = ref([])

/* 获取权限 */
async function loadPermission() {
  loadingPermission.value = true
  try {
    const res = await request.get('/departmentScoreApply/my-permissions')
    const data = res.data?.data || {}
    permission.departments = data.departments || []
    permission.canDepartmentApply = data.canDepartmentApply === true
    permission.canDepartmentAudit = data.canDepartmentAudit === true

    const validDepartmentIds = permission.departments.map((item) => item.departmentId)
    if (departmentForm.departmentId && !validDepartmentIds.includes(departmentForm.departmentId)) {
      departmentForm.departmentId = null
      departmentForm.templateId = null
    }
  } catch (error) {
    console.error(error)
    permission.departments = []
    permission.canDepartmentApply = false
    permission.canDepartmentAudit = false
    ElMessage.error(error?.response?.data?.message || '获取部门权限失败')
  } finally {
    loadingPermission.value = false
  }
}

/* 加载全部学生 */
async function loadStudentList() {
  try {
    const res = await request.get('/user/student-list')
    studentList.value = res.data?.data || []
  } catch (error) {
    console.error(error)
    studentList.value = []
    ElMessage.error('获取学生列表失败')
  }
}

/* 根据部门加载模板 */
async function loadDepartmentTemplates(departmentId) {
  departmentTemplateList.value = []
  departmentForm.templateId = null
  if (!departmentId) return
  templateLoading.value = true
  try {
    const res = await request.get('/departmentScoreTemplate/list', {
      params: { departmentId },
    })
    departmentTemplateList.value = res.data?.data || []
  } catch (error) {
    console.error(error)
    departmentTemplateList.value = []
    ElMessage.error(error?.response?.data?.message || '获取部门加减分项目失败')
  } finally {
    templateLoading.value = false
  }
}

watch(
  () => departmentForm.departmentId,
  async (val) => {
    await loadDepartmentTemplates(val)
  },
)

/* 部门审核列表 */
async function loadAuditList() {
  if (!permission.canDepartmentAudit) {
    auditList.value = []
    return
  }
  auditLoading.value = true
  try {
    const res = await request.get('/departmentScoreApply/audit/list')
    auditList.value = res.data?.code === 200 ? res.data.data || [] : []
  } catch (error) {
    console.error(error)
    auditList.value = []
    ElMessage.error(error?.response?.data?.message || '获取待审核申请失败')
  } finally {
    auditLoading.value = false
  }
}

/* 审核操作 */
async function auditApply(row, status) {
  const actionText = status === 1 ? '通过' : '驳回'
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入${actionText}意见`,
      `部门申报${actionText}`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入审核意见（可选）',
        inputType: 'textarea',
      },
    )
    await request.put(`/departmentScoreApply/audit/${row.id}`, {
      status,
      reviewRemark: value || '',
    })
    ElMessage.success(`部门申报已${actionText}`)
    await loadAuditList()
    await loadMyApply()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    console.error(error)
    ElMessage.error(error?.response?.data?.message || `部门申报${actionText}失败`)
  }
}

/* 加载加分规则 */
async function loadScoreRules() {
  try {
    const res = await request.get('/scoreRule/list')
    scoreRules.value = res.data?.data || []
  } catch (error) {
    console.error(error)
    scoreRules.value = []
  }
}

/* 证书文件 */
function handleCertificateFile(file) {
  certificateFile.value = file.raw
}
function removeCertificateFile() {
  certificateFile.value = null
}

/* 部门文件 */
function handleDepartmentFile(file) {
  departmentFile.value = file.raw
}
function removeDepartmentFile() {
  departmentFile.value = null
}

/* 提交个人证书 */
async function submitCertificate() {
  if (!certificateFormRef.value) return
  const valid = await certificateFormRef.value.validate()
  if (!valid) return

  certificateSubmitting.value = true
  try {
    const data = {
      ruleId: certificateForm.ruleId,
      applyScore: certificateForm.applyScore,
      description: certificateForm.description,
      materialFile: certificateFile.value ? certificateFile.value.name : null,
    }
    await request.post('/scoreApply/add', data)
    ElMessage.success('个人证书申请提交成功，等待档案部审核')
    resetCertificate()
    await loadMyApply()
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    certificateSubmitting.value = false
  }
}

//新增证书表单重置（原有变量不动）
function resetDepartment() {
  departmentForm.departmentId = null
  departmentForm.studentId = null
  departmentForm.templateId = null
  departmentTemplateList.value = []
  departmentFile.value = null
  departmentFormRef.value?.resetFields()
}

/* 提交部门加减分 */
async function submitDepartment() {
  if (!departmentFormRef.value) {
    return
  }

  const valid = await departmentFormRef.value.validate()

  if (!valid) {
    return
  }

  departmentSubmitting.value = true

  try {
    const data = {
      departmentId: departmentForm.departmentId,
      studentId: departmentForm.studentId,
      templateId: departmentForm.templateId,
      evidenceUrl: departmentFile.value ? departmentFile.value.name : null,
    }

    await request.post('/departmentScoreApply/add', data)

    ElMessage.success('部门加减分申报提交成功，等待本部门副部长或部长审核')

    resetDepartment()

    await loadMyApply()
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    departmentSubmitting.value = false
  }
}

/* 重置 */
function resetDepartment() {
  departmentForm.departmentId = null
  departmentForm.studentId = null
  departmentForm.templateId = null
  departmentTemplateList.value = []
  departmentFile.value = null
  departmentFormRef.value?.resetFields()
}

/* 我的申报记录 */
async function loadMyApply() {
  listLoading.value = true

  try {
    const [certificateRes, departmentRes] = await Promise.all([
      request.get('/scoreApply/my').catch(() => ({ data: { data: [] } })),

      request.get('/departmentScoreApply/my').catch(() => ({ data: { data: [] } })),
    ])

    certificateApplyList.value = (certificateRes.data?.data || []).map((item) => ({
      ...item,
      title: item.ruleName || item.rule?.name || item.description || '证书申报',
    }))

    departmentApplyList.value = (departmentRes.data?.data || []).map((item) => ({
      ...item,
      title: item.title || '部门加减分',
    }))

    // 兼容旧代码
    myApplyList.value = [...certificateApplyList.value, ...departmentApplyList.value]
  } catch (error) {
    console.error(error)

    certificateApplyList.value = []
    departmentApplyList.value = []
    myApplyList.value = []
  } finally {
    listLoading.value = false
  }
}

/* 页面初始化 */
onMounted(async () => {
  await loadPermission()
  await loadStudentList()
  await loadScoreRules()
  await loadMyApply()
  if (permission.canDepartmentAudit) {
    await loadAuditList()
  }
})
</script>

<style scoped>
.score-apply-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
}

.apply-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 25px;
}

.card-header h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #303133;
}

.card-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.apply-form {
  max-width: 800px;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
}

.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.template-score {
  margin-left: 12px;
  font-weight: 700;
  color: #606266;
}

.empty-template {
  color: #c0c4cc;
}

.bonus {
  color: #67c23a;
  font-weight: 700;
}

.deduct {
  color: #f56c6c;
  font-weight: 700;
}

.department-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.department-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.department-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.department-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.department-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
</style>
