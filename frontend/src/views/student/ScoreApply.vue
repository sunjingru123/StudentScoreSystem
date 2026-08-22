<template>
  <div class="score-apply-page">
    <div class="page-header">
      <div>
        <h2>综合测评申报</h2>
        <p>个人证书和部门活动加减分申报</p>
      </div>

      <el-button :loading="loadingPermission" @click="loadPermission"> 刷新 </el-button>
    </div>
    <!-- ========================= -->
    <!-- 我的部门身份 -->
    <!-- ========================= -->

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
            <span v-if="item.position === '干事'"> 你可以提交本部门加减分申报 </span>

            <span v-else> 你可以提交部门申报，并审核本部门其他成员的申报 </span>
          </div>
        </div>
      </div>

      <el-empty v-else description="当前没有部门干部身份" />
    </el-card>
    <!-- ========================= -->
    <!-- 部门申报审核 -->
    <!-- ========================= -->

    <el-card v-if="permission.canDepartmentAudit" class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>部门申报审核</h3>
          <p>你是副部长或部长，可以审核本部门其他成员提交的加减分申请。</p>
        </div>

        <el-button size="small" :loading="auditLoading" @click="loadAuditList"> 刷新 </el-button>
      </div>

      <el-table v-loading="auditLoading" :data="auditList" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="title" label="申报项目" min-width="180" />

        <el-table-column label="申报学生" width="120" align="center">
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
          label="申报说明"
          min-width="220"
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
    <!-- 个人证书 -->
    <el-card class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>个人证书申报</h3>
          <p>上传个人获奖证书、竞赛证书等材料， 提交后由档案部审核。</p>
        </div>

        <el-tag type="primary"> 档案部审核 </el-tag>
      </div>

      <el-form
        ref="certificateFormRef"
        :model="certificateForm"
        :rules="certificateRules"
        label-width="110px"
        class="apply-form"
      >
        <el-form-item label="加分项目" prop="ruleId">
          <el-select
            v-model="certificateForm.ruleId"
            placeholder="请选择加分项目"
            style="width: 100%"
          >
            <el-option
              v-for="item in scoreRules"
              :key="item.id"
              :label="`${item.name}（${item.score}分）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="申请分值" prop="applyScore">
          <el-input-number
            v-model="certificateForm.applyScore"
            :min="0.01"
            :max="40"
            :precision="2"
            :step="0.5"
          />
        </el-form-item>

        <el-form-item label="申报说明" prop="description">
          <el-input
            v-model="certificateForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="请输入证书、获奖情况等说明"
          />
        </el-form-item>

        <el-form-item label="获奖凭证">
          <el-upload
            action=""
            :auto-upload="false"
            :limit="1"
            :on-change="handleCertificateFile"
            :on-remove="removeCertificateFile"
          >
            <el-button type="primary"> 选择凭证 </el-button>

            <template #tip>
              <div class="upload-tip">请上传获奖证书、证明材料等</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="certificateSubmitting" @click="submitCertificate">
            提交个人证书
          </el-button>

          <el-button @click="resetCertificate"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 部门活动 -->
    <el-card v-if="permission.canDepartmentApply" class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>部门活动加减分申报</h3>
          <p>申报本部门活动、部门工作等加减分， 由本部门副部长或部长审核。</p>
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
        <!-- 部门 -->
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

        <!-- 类型 -->
        <el-form-item label="申报类型" prop="scoreType">
          <el-radio-group v-model="departmentForm.scoreType">
            <el-radio :value="1"> 加分 </el-radio>

            <el-radio :value="-1"> 减分 </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 分值 -->
        <el-form-item label="分值" prop="score">
          <el-input-number
            v-model="departmentForm.score"
            :min="0.01"
            :max="40"
            :precision="2"
            :step="0.5"
          />
        </el-form-item>

        <!-- 项目 -->
        <el-form-item label="项目名称" prop="title">
          <el-input
            v-model="departmentForm.title"
            maxlength="200"
            show-word-limit
            placeholder="例如：迎新活动、志愿服务、部门工作等"
          />
        </el-form-item>

        <!-- 说明 -->
        <el-form-item label="申报说明" prop="description">
          <el-input
            v-model="departmentForm.description"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请详细说明活动内容、本人负责工作等"
          />
        </el-form-item>

        <!-- 凭证 -->
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

        <el-form-item>
          <el-button type="success" :loading="departmentSubmitting" @click="submitDepartment">
            提交部门申报
          </el-button>

          <el-button @click="resetDepartment"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 我的申请 -->
    <el-card class="apply-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>我的申报记录</h3>
          <p>查看个人证书和部门加减分申报的审核进度。</p>
        </div>

        <el-button size="small" :loading="listLoading" @click="loadMyApply">
          刷新
        </el-button>
      </div>

      <el-table
        v-loading="listLoading"
        :data="myApplyList"
        border
        stripe
      >
        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
        />

        <!-- 申报项目 -->
        <el-table-column
          label="申报项目"
          min-width="180"
        >
          <template #default="{ row }">
            {{ row.title }}
          </template>
        </el-table-column>

        <!-- 申报类型 -->
        <el-table-column
          label="申报类型"
          width="120"
          align="center"
        >
          <template #default="{ row }">

            <el-tag
              v-if="row.applyType === 'CERTIFICATE'"
              type="primary"
            >
              个人证书
            </el-tag>

            <el-tag
              v-else
              type="success"
            >
              部门加减分
            </el-tag>

          </template>
        </el-table-column>

        <!-- 部门 -->
        <el-table-column
          label="部门"
          min-width="130"
          align="center"
        >
          <template #default="{ row }">
        <span v-if="row.applyType === 'DEPARTMENT'">
          {{ row.departmentName || '-' }}
        </span>

            <span v-else>
          -
        </span>
          </template>
        </el-table-column>

        <!-- 加减分 -->
        <el-table-column
          label="类型"
          width="90"
          align="center"
        >
          <template #default="{ row }">

            <el-tag
              v-if="row.applyType === 'CERTIFICATE'"
              type="success"
            >
              加分
            </el-tag>

            <el-tag
              v-else-if="Number(row.scoreType) === 1"
              type="success"
            >
              加分
            </el-tag>

            <el-tag
              v-else
              type="danger"
            >
              减分
            </el-tag>

          </template>
        </el-table-column>

        <!-- 分值 -->
        <el-table-column
          label="分值"
          width="90"
          align="center"
        >
          <template #default="{ row }">

        <span
          v-if="row.applyType === 'CERTIFICATE'"
          class="bonus"
        >
          +{{ row.score }}
        </span>

            <span
              v-else
              :class="Number(row.scoreType) === 1 ? 'bonus' : 'deduct'"
            >
          {{ Number(row.scoreType) === 1 ? '+' : '-' }}{{ row.score }}
        </span>

          </template>
        </el-table-column>

        <!-- 审核状态 -->
        <el-table-column
          label="审核状态"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">

            <!-- 个人证书 -->
            <template v-if="row.applyType === 'CERTIFICATE'">

              <el-tag
                v-if="Number(row.status) === 0"
                type="warning"
              >
                待档案部审核
              </el-tag>

              <el-tag
                v-else-if="Number(row.status) === 1"
                type="success"
              >
                审核通过
              </el-tag>

              <el-tag
                v-else
                type="danger"
              >
                已驳回
              </el-tag>

            </template>

            <!-- 部门申报 -->
            <template v-else>

              <!-- 初审待审核 -->
              <el-tag
                v-if="Number(row.status) === 0"
                type="warning"
              >
                待部门审核
              </el-tag>

              <!-- 初审驳回 -->
              <el-tag
                v-else-if="Number(row.status) === 2"
                type="danger"
              >
                部门审核驳回
              </el-tag>

              <!-- 初审通过，看终审 -->
              <template v-else>

                <el-tag
                  v-if="Number(row.finalStatus) === 0"
                  type="warning"
                >
                  待辅导员终审
                </el-tag>

                <el-tag
                  v-else-if="Number(row.finalStatus) === 1"
                  type="success"
                >
                  终审通过
                </el-tag>

                <el-tag
                  v-else
                  type="danger"
                >
                  终审驳回
                </el-tag>

              </template>

            </template>

          </template>
        </el-table-column>

        <!-- 审核意见 -->
        <el-table-column
          label="审核意见"
          min-width="200"
          show-overflow-tooltip
        >
          <template #default="{ row }">

            <!-- 个人证书 -->
            <span v-if="row.applyType === 'CERTIFICATE'">
          {{ row.description || '-' }}
        </span>

            <!-- 部门申报 -->
            <span v-else>

          <template v-if="row.finalStatus === 2">
            {{ row.finalReviewRemark || '-' }}
          </template>

          <template v-else-if="row.status === 2">
            {{ row.reviewRemark || '-' }}
          </template>

          <template v-else-if="row.finalStatus === 1">
            {{ row.finalReviewRemark || '审核通过' }}
          </template>

          <template v-else-if="row.status === 1">
            {{ row.reviewRemark || '部门初审通过' }}
          </template>

          <template v-else>
            -
          </template>

        </span>

          </template>
        </el-table-column>

        <!-- 时间 -->
        <el-table-column
          prop="createTime"
          label="申报时间"
          width="180"
          align="center"
        />

      </el-table>

      <el-empty
        v-if="!listLoading && myApplyList.length === 0"
        description="暂无申报记录"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import request from '@/utils/request'

/*
 * ============================
 * 权限
 * ============================
 */

const loadingPermission = ref(false)

const permission = reactive({
  canDepartmentApply: false,

  // 副部长 / 部长才能审核
  canDepartmentAudit: false,

  departments: [],
})
/*
 * ============================
 * 个人证书
 * ============================
 */

const certificateFormRef = ref(null)

const certificateSubmitting = ref(false)

const certificateFile = ref(null)

const certificateForm = reactive({
  ruleId: null,
  applyScore: null,
  description: '',
})

const certificateRules = {
  ruleId: [
    {
      required: true,
      message: '请选择加分项目',
      trigger: 'change',
    },
  ],

  applyScore: [
    {
      required: true,
      message: '请输入申请分值',
      trigger: 'blur',
    },
  ],

  description: [
    {
      required: true,
      message: '请输入申报说明',
      trigger: 'blur',
    },
  ],
}

const scoreRules = ref([])

/*
 * ============================
 * 部门申报
 * ============================
 */

const departmentFormRef = ref(null)

const departmentSubmitting = ref(false)

const departmentFile = ref(null)

const departmentForm = reactive({
  departmentId: null,
  scoreType: 1,
  score: null,
  title: '',
  description: '',
})

const departmentRules = {
  departmentId: [
    {
      required: true,
      message: '请选择部门',
      trigger: 'change',
    },
  ],

  scoreType: [
    {
      required: true,
      message: '请选择加减分',
      trigger: 'change',
    },
  ],

  score: [
    {
      required: true,
      message: '请输入分值',
      trigger: 'blur',
    },
  ],

  title: [
    {
      required: true,
      message: '请输入项目名称',
      trigger: 'blur',
    },
  ],

  description: [
    {
      required: true,
      message: '请输入申报说明',
      trigger: 'blur',
    },
  ],
}

/*
 * ============================
 * 我的申请
 * ============================
 */

const listLoading = ref(false)

const myApplyList = ref([])
/*
 * ============================
 * 部门审核
 * ============================
 */

const auditLoading = ref(false)

const auditList = ref([])
/*
 * ============================
 * 加载权限
 * ============================
 */

async function loadPermission() {
  loadingPermission.value = true

  try {
    const res = await request.get('/departmentScoreApply/my-permissions')

    const data = res.data?.data || {}

    permission.departments = data.departments || []

    permission.canDepartmentApply = data.canDepartmentApply === true

    /*
     * 副部长 / 部长拥有审核权限
     */
    permission.canDepartmentAudit = permission.departments.some(
      (item) => item.position === '副部长' || item.position === '部长',
    )
  } catch (error) {
    console.error(error)

    permission.canDepartmentApply = false

    permission.canDepartmentAudit = false

    permission.departments = []

    ElMessage.error('获取部门权限失败')
  } finally {
    loadingPermission.value = false
  }
}
/*
 * ============================
 * 加载部门待审核申请
 * ============================
 */

async function loadAuditList() {
  if (!permission.canDepartmentAudit) {
    auditList.value = []
    return
  }

  auditLoading.value = true

  try {
    const res = await request.get('/departmentScoreApply/audit/list')

    if (res.data?.code === 200) {
      auditList.value = res.data.data || []
    } else {
      auditList.value = []
    }
  } catch (error) {
    console.error(error)

    auditList.value = []

    ElMessage.error(error?.response?.data?.message || '获取待审核申请失败')
  } finally {
    auditLoading.value = false
  }
}
/*
 * ============================
 * 审核部门申报
 * ============================
 */

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
  } catch (error) {
    /*
     * 用户点取消不提示错误
     */
    if (error === 'cancel' || error === 'close') {
      return
    }

    console.error(error)

    ElMessage.error(error?.response?.data?.message || `部门申报${actionText}失败`)
  }
}
/*
 * ============================
 * 加载加分规则
 * ============================
 */

async function loadScoreRules() {
  try {
    const res = await request.get('/scoreRule/list')

    scoreRules.value = res.data?.data || []
  } catch (error) {
    console.error(error)
  }
}

/*
 * ============================
 * 个人证书文件
 * ============================
 */

function handleCertificateFile(file) {
  certificateFile.value = file.raw
}

function removeCertificateFile() {
  certificateFile.value = null
}

/*
 * ============================
 * 部门文件
 * ============================
 */

function handleDepartmentFile(file) {
  departmentFile.value = file.raw
}

function removeDepartmentFile() {
  departmentFile.value = null
}

/*
 * ============================
 * 提交个人证书
 * ============================
 */

async function submitCertificate() {
  if (!certificateFormRef.value) {
    return
  }

  const valid = await certificateFormRef.value.validate()

  if (!valid) {
    return
  }

  certificateSubmitting.value = true

  try {
    const data = {
      ruleId: certificateForm.ruleId,

      applyScore: certificateForm.applyScore,

      description: certificateForm.description,

      materialFile: certificateFile.value ? certificateFile.value.name : null,
    }

    await request.post('/scoreApply/add', data)

    ElMessage.success('个人证书申报提交成功，等待档案部审核')

    resetCertificate()

    await loadMyApply()
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    certificateSubmitting.value = false
  }
}

/*
 * ============================
 * 提交部门申报
 * ============================
 */

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

      scoreType: departmentForm.scoreType,

      score: departmentForm.score,

      title: departmentForm.title,

      description: departmentForm.description,

      evidenceUrl: departmentFile.value ? departmentFile.value.name : null,
    }

    await request.post('/departmentScoreApply/add', data)

    ElMessage.success('部门申报提交成功，等待本部门副部或部长审核')

    resetDepartment()

    await loadMyApply()
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    departmentSubmitting.value = false
  }
}

/*
 * ============================
 * 重置
 * ============================
 */

function resetCertificate() {
  certificateForm.ruleId = null

  certificateForm.applyScore = null

  certificateForm.description = ''

  certificateFile.value = null

  certificateFormRef.value?.resetFields()
}

function resetDepartment() {
  departmentForm.departmentId = null

  departmentForm.scoreType = 1

  departmentForm.score = null

  departmentForm.title = ''

  departmentForm.description = ''

  departmentFile.value = null

  departmentFormRef.value?.resetFields()
}

/*
 * ============================
 * 我的申请
 * ============================
 */

async function loadMyApply() {
  listLoading.value = true

  try {
    const [certificateRes, departmentRes] = await Promise.all([
      request
        .get('/scoreApply/my')
        .catch(() => ({ data: { data: [] } })),

      request
        .get('/departmentScoreApply/my')
        .catch(() => ({ data: { data: [] } })),
    ])

    // ============================
    // 个人证书申报
    // ============================

    const certificateList = (
      certificateRes.data?.data || []
    ).map((item) => ({
      ...item,

      applyType: 'CERTIFICATE',

      title:
        item.ruleName ||
        item.description ||
        '个人证书申报',

      scoreType: 1,

      score: item.applyScore,

      departmentName: null,

      finalStatus: null,

      reviewRemark: null,

      finalReviewRemark: null,
    }))

    // ============================
    // 部门申报
    // ============================

    const departmentList = (
      departmentRes.data?.data || []
    ).map((item) => ({
      ...item,

      applyType: 'DEPARTMENT',

      title:
        item.title ||
        '部门加减分申报',

      scoreType: item.scoreType,

      score: item.score,

      departmentName:
        item.departmentName || null,

      reviewRemark:
        item.reviewRemark || null,

      finalReviewRemark:
        item.finalReviewRemark || null,

      finalStatus:
        item.finalStatus ?? 0,
    }))

    // ============================
    // 合并
    // ============================

    myApplyList.value = [
      ...certificateList,
      ...departmentList,
    ].sort((a, b) => {
      const timeA = new Date(
        a.createTime || 0
      ).getTime()

      const timeB = new Date(
        b.createTime || 0
      ).getTime()

      return timeB - timeA
    })

  } catch (error) {
    console.error(error)

    myApplyList.value = []

    ElMessage.error('获取申报记录失败')
  } finally {
    listLoading.value = false
  }
}

/*
 * ============================
 * 初始化
 * ============================
 */

onMounted(async () => {
  await loadPermission()

  await loadScoreRules()

  await loadMyApply()

  /*
   * 只有副部长 / 部长才加载审核列表
   */
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
