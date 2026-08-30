<template>
  <div class="score-apply-page">

    <!-- 页面头部 -->
    <div class="page-header">

      <div>

        <h2>
          部门加减分申报
        </h2>

        <p>
          申报本部门活动、部门工作等加减分
        </p>

      </div>


      <el-button
        :loading="loadingPermission"
        @click="refreshAll"
      >

        <el-icon>
          <Refresh />
        </el-icon>

        刷新

      </el-button>

    </div>


    <!-- =====================================================
         我的部门身份
    ====================================================== -->

    <el-card
      class="apply-card"
      shadow="never"
    >

      <div class="card-header">

        <div>

          <h3>
            我的部门身份
          </h3>

          <p>
            显示你当前加入的部门以及担任的职务。
          </p>

        </div>


        <el-button
          size="small"
          :loading="loadingPermission"
          @click="refreshPermission"
        >

          <el-icon>
            <Refresh />
          </el-icon>

          刷新

        </el-button>

      </div>


      <div
        v-if="permission.departments.length > 0"
        class="department-list"
      >

        <div
          v-for="item in permission.departments"
          :key="item.departmentId"
          class="department-item"
        >

          <div class="department-info">

            <div class="department-name">

              {{ item.departmentName || '未命名部门' }}

            </div>


            <el-tag
              :type="getPositionTagType(item.position)"
            >

              {{ getPositionName(item.position) }}

            </el-tag>

          </div>


          <div class="department-tip">

            <span
              v-if="isDepartmentLeader(item.position)"
            >

              你可以提交本部门加减分申报，
              并审核本部门其他成员提交的申报。

            </span>


            <span v-else>

              你可以提交本部门加减分申报。

            </span>

          </div>

        </div>

      </div>


      <el-empty
        v-else
        description="当前没有部门身份"
      />

    </el-card>


    <!-- =====================================================
         部门申报审核
    ====================================================== -->

    <el-card
      v-if="permission.canDepartmentAudit"
      class="apply-card"
      shadow="never"
    >

      <div class="card-header">

        <div>

          <h3>
            部门申报审核
          </h3>

          <p>
            审核本部门其他成员提交的学生加减分申报。
          </p>

        </div>


        <div class="audit-header-actions">

          <span class="selected-text">

            当前页已选择

            <strong>
              {{ selectedAuditRows.length }}
            </strong>

            条

          </span>


          <!-- 一键通过选中 -->

          <el-button
            type="success"
            :disabled="
              selectedAuditRows.length === 0 ||
              batchAuditing
            "
            :loading="batchAuditing"
            @click="batchAuditPass"
          >

            <el-icon>
              <CircleCheck />
            </el-icon>

            一键通过选中

          </el-button>


          <!-- 一键全部通过 -->

          <el-button
            type="primary"
            :disabled="
              auditList.length === 0 ||
              batchAuditing
            "
            :loading="batchAuditing"
            @click="batchAuditAllPass"
          >

            <el-icon>
              <CircleCheck />
            </el-icon>

            一键全部通过

          </el-button>


          <!-- 刷新 -->

          <el-button
            size="small"
            :loading="auditLoading"
            @click="loadAuditList"
          >

            <el-icon>
              <Refresh />
            </el-icon>

            刷新

          </el-button>

        </div>

      </div>


      <!-- 审核分页提示 -->

      <div
        v-if="auditList.length > 0"
        class="audit-page-tip"
      >

        <span>

          当前显示第

          <strong>
            {{ auditCurrentPage }}
          </strong>

          页，每页

          <strong>
            {{ auditPageSize }}
          </strong>

          条

        </span>


        <span>

          共

          <strong>
            {{ auditList.length }}
          </strong>

          条待审核申请

        </span>


        <span class="tip-divider">
          |
        </span>


        <span>

          共

          <strong>
            {{ auditTotalPages }}
          </strong>

          页

        </span>

      </div>


      <!-- 审核表格 -->

      <el-table
        ref="auditTableRef"
        v-loading="auditLoading"
        :data="pagedAuditList"
        border
        stripe
        row-key="id"
        style="width: 100%"
        @selection-change="handleAuditSelectionChange"
      >

        <!-- 选择 -->

        <el-table-column
          type="selection"
          width="55"
          align="center"
        />


        <!-- 序号 -->

        <el-table-column
          label="#"
          width="60"
          align="center"
        >

          <template #default="{ $index }">

            {{
              (auditCurrentPage - 1) *
              auditPageSize +
              $index +
              1
            }}

          </template>

        </el-table-column>


        <!-- 项目名称 -->

        <el-table-column
          prop="title"
          label="加减分项目"
          min-width="180"
        >

          <template #default="{ row }">

            {{ row.title || '—' }}

          </template>

        </el-table-column>


        <!-- 被加减分学生 -->

        <el-table-column
          label="被加减分学生"
          width="150"
          align="center"
        >

          <template #default="{ row }">

            {{
              row.studentName ||
              row.studentId ||
              '—'
            }}

          </template>

        </el-table-column>


        <!-- 申报人 -->

        <el-table-column
          label="申报人"
          width="130"
          align="center"
        >

          <template #default="{ row }">

            {{
              row.applicantName ||
              row.applicantUsername ||
              '—'
            }}

          </template>

        </el-table-column>


        <!-- 部门 -->

        <el-table-column
          label="部门"
          width="130"
          align="center"
        >

          <template #default="{ row }">

            {{
              row.departmentName ||
              row.departmentId ||
              '—'
            }}

          </template>

        </el-table-column>


        <!-- 类型 -->

        <el-table-column
          label="类型"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="Number(row.scoreType) === 1"
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
          width="110"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                Number(row.scoreType) === 1
                  ? 'bonus'
                  : 'deduct'
              "
            >

              {{
                Number(row.scoreType) === 1
                  ? '+'
                  : '-'
              }}{{ row.score }}

            </span>

          </template>

        </el-table-column>


        <!-- 项目说明 -->

        <el-table-column
          prop="description"
          label="项目说明"
          min-width="240"
          show-overflow-tooltip
        >

          <template #default="{ row }">

            {{ row.description || '—' }}

          </template>

        </el-table-column>


        <!-- 申报时间 -->

        <el-table-column
          prop="createTime"
          label="申报时间"
          width="180"
          align="center"
        />


        <!-- 操作 -->

        <el-table-column
          label="操作"
          width="180"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              type="success"
              size="small"
              :disabled="
                batchAuditing ||
                auditingId !== null
              "
              :loading="auditingId === row.id"
              @click="auditApply(row, 1)"
            >

              <el-icon>
                <CircleCheck />
              </el-icon>

              通过

            </el-button>


            <el-button
              type="danger"
              size="small"
              :disabled="
                batchAuditing ||
                auditingId !== null
              "
              :loading="auditingId === row.id"
              @click="auditApply(row, 2)"
            >

              <el-icon>
                <CircleClose />
              </el-icon>

              驳回

            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- 没有数据 -->

      <el-empty
        v-if="
          !auditLoading &&
          auditList.length === 0
        "
        description="暂无待审核部门申报"
      />


      <!-- 分页 -->

      <div
        v-if="
          !auditLoading &&
          auditList.length > 0
        "
        class="audit-pagination"
      >

        <el-pagination
          v-model:current-page="auditCurrentPage"
          :page-size="auditPageSize"
          :total="auditList.length"
          layout="prev, pager, next"
          background
          @current-change="handleAuditPageChange"
        />

      </div>

    </el-card>


    <!-- =====================================================
         部门活动申报
    ====================================================== -->

    <el-card
      v-if="permission.canDepartmentApply"
      class="apply-card"
      shadow="never"
    >

      <div class="card-header">

        <div>

          <h3>
            部门活动加减分申报
          </h3>

          <p>
            申报本部门活动、部门工作等加减分，
            由本部门副部长或部长审核。
          </p>

        </div>


        <el-tag type="success">

          部门审核 → 辅导员审核

        </el-tag>

      </div>


      <el-form
        ref="departmentFormRef"
        :model="departmentForm"
        :rules="departmentRules"
        label-width="110px"
        class="apply-form"
      >

        <!-- 所属部门 -->

        <el-form-item
          label="所属部门"
          prop="departmentId"
        >

          <el-select
            v-model="departmentForm.departmentId"
            placeholder="请选择申报部门"
            style="width: 100%"
          >

            <el-option
              v-for="item in permission.departments"
              :key="item.departmentId"
              :label="
                `${item.departmentName || '未命名部门'}（${getPositionName(item.position)}）`
              "
              :value="item.departmentId"
            />

          </el-select>

        </el-form-item>


        <!-- 申报类型 -->

        <el-form-item
          label="申报类型"
          prop="scoreType"
        >

          <el-radio-group
            v-model="departmentForm.scoreType"
          >

            <el-radio :value="1">
              加分
            </el-radio>


            <el-radio :value="-1">
              减分
            </el-radio>

          </el-radio-group>

        </el-form-item>


        <!-- 分值 -->

        <el-form-item
          label="分值"
          prop="score"
        >

          <el-input-number
            v-model="departmentForm.score"
            :min="0.01"
            :max="40"
            :precision="2"
            :step="0.5"
          />

        </el-form-item>


        <!-- 项目名称 -->

        <el-form-item
          label="项目名称"
          prop="title"
        >

          <el-input
            v-model="departmentForm.title"
            maxlength="200"
            show-word-limit
            placeholder="例如：迎新活动、志愿服务、部门工作等"
          />

        </el-form-item>


        <!-- 申报说明 -->

        <el-form-item
          label="申报说明"
          prop="description"
        >

          <el-input
            v-model="departmentForm.description"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请详细说明活动内容、本人负责工作等"
          />

        </el-form-item>


        <!-- 活动凭证 -->

        <el-form-item
          label="活动凭证"
        >

          <el-upload
            action="#"
            :auto-upload="false"
            :limit="1"
            :on-change="handleDepartmentFile"
            :on-remove="removeDepartmentFile"
          >

            <el-button type="primary">

              选择凭证

            </el-button>


            <template #tip>

              <div class="upload-tip">

                可上传活动证明、照片、文件等材料

              </div>

            </template>

          </el-upload>

        </el-form-item>


        <!-- 提交 -->

        <el-form-item>

          <el-button
            type="success"
            :loading="departmentSubmitting"
            @click="submitDepartment"
          >

            提交部门申报

          </el-button>


          <el-button
            @click="resetDepartment"
          >

            重置

          </el-button>

        </el-form-item>

      </el-form>

    </el-card>


    <!-- =====================================================
         我的部门申报记录
    ====================================================== -->

    <el-card
      class="apply-card"
      shadow="never"
    >

      <div class="card-header">

        <div>

          <h3>
            我的部门申报记录
          </h3>

          <p>
            查看自己提交的部门加减分申报及审核进度。
          </p>

        </div>


        <el-button
          size="small"
          :loading="listLoading"
          @click="loadMyApply"
        >

          <el-icon>
            <Refresh />
          </el-icon>

          刷新

        </el-button>

      </div>


      <el-table
        v-loading="listLoading"
        :data="myApplyList"
        border
        stripe
      >

        <!-- 序号 -->

        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
        />


        <!-- 项目 -->

        <el-table-column
          label="申报项目"
          min-width="180"
        >

          <template #default="{ row }">

            {{ row.title || '—' }}

          </template>

        </el-table-column>


        <!-- 部门 -->

        <el-table-column
          label="部门"
          min-width="130"
          align="center"
        >

          <template #default="{ row }">

            {{ row.departmentName || '-' }}

          </template>

        </el-table-column>


        <!-- 类型 -->

        <el-table-column
          label="类型"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="Number(row.scoreType) === 1"
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
          width="110"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                Number(row.scoreType) === 1
                  ? 'bonus'
                  : 'deduct'
              "
            >

              {{
                Number(row.scoreType) === 1
                  ? '+'
                  : '-'
              }}{{ row.score }}

            </span>

          </template>

        </el-table-column>


        <!-- 审核状态 -->

        <el-table-column
          label="审核状态"
          min-width="180"
          align="center"
        >

          <template #default="{ row }">

            <!-- 待部门审核 -->

            <el-tag
              v-if="Number(row.status) === 0"
              type="warning"
            >

              待部门审核

            </el-tag>


            <!-- 部门驳回 -->

            <el-tag
              v-else-if="Number(row.status) === 2"
              type="danger"
            >

              部门审核驳回

            </el-tag>


            <!-- 部门通过，等待辅导员 -->

            <el-tag
              v-else-if="
                Number(row.status) === 1 &&
                Number(row.finalStatus) === 0
              "
              type="warning"
            >

              待辅导员终审

            </el-tag>


            <!-- 终审通过 -->

            <el-tag
              v-else-if="
                Number(row.status) === 1 &&
                Number(row.finalStatus) === 1
              "
              type="success"
            >

              终审通过

            </el-tag>


            <!-- 终审驳回 -->

            <el-tag
              v-else-if="
                Number(row.status) === 1 &&
                Number(row.finalStatus) === 2
              "
              type="danger"
            >

              终审驳回

            </el-tag>


            <!-- 其他 -->

            <el-tag
              v-else
              type="warning"
            >

              审核处理中

            </el-tag>

          </template>

        </el-table-column>


        <!-- 审核意见 -->

        <el-table-column
          label="审核意见"
          min-width="200"
          show-overflow-tooltip
        >

          <template #default="{ row }">

            <!-- 终审驳回 -->

            <template
              v-if="Number(row.finalStatus) === 2"
            >

              {{
                row.finalReviewRemark ||
                '终审驳回'
              }}

            </template>


            <!-- 部门驳回 -->

            <template
              v-else-if="Number(row.status) === 2"
            >

              {{
                row.reviewRemark ||
                '部门审核驳回'
              }}

            </template>


            <!-- 终审通过 -->

            <template
              v-else-if="Number(row.finalStatus) === 1"
            >

              {{
                row.finalReviewRemark ||
                '审核通过'
              }}

            </template>


            <!-- 部门初审通过 -->

            <template
              v-else-if="Number(row.status) === 1"
            >

              {{
                row.reviewRemark ||
                '部门初审通过'
              }}

            </template>


            <template v-else>

              -

            </template>

          </template>

        </el-table-column>


        <!-- 申报时间 -->

        <el-table-column
          prop="createTime"
          label="申报时间"
          width="180"
          align="center"
        />

      </el-table>


      <el-empty
        v-if="
          !listLoading &&
          myApplyList.length === 0
        "
        description="暂无部门申报记录"
      />

    </el-card>

  </div>
</template>


<script setup>

import {
  ref,
  reactive,
  computed,
  onMounted
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  CircleCheck,
  CircleClose,
  Refresh
} from '@element-plus/icons-vue'

import request from '@/utils/request'


/* =========================================================
   权限
========================================================= */

const loadingPermission =
  ref(false)

const permission =
  reactive({

    canDepartmentApply:
      false,

    canDepartmentAudit:
      false,

    departments:
      []

  })


/* =========================================================
   权限辅助函数
========================================================= */

function isDepartmentLeader(position) {

  const value =
    String(position ?? '')
      .trim()

  return (
    value === '部长' ||
    value === '副部长' ||
    value === '1' ||
    value === '2'
  )

}


function getPositionName(position) {

  const value =
    String(position ?? '')
      .trim()

  if (
    value === '部长'
  ) {

    return '部长'

  }


  if (
    value === '副部长'
  ) {

    return '副部长'

  }


  return value || '成员'

}


function getPositionTagType(position) {

  const name =
    getPositionName(position)

  if (
    name === '部长'
  ) {

    return 'danger'

  }


  if (
    name === '副部长'
  ) {

    return 'warning'

  }


  return 'primary'

}


/* =========================================================
   部门申报
========================================================= */

const departmentFormRef =
  ref(null)

const departmentSubmitting =
  ref(false)

const departmentFile =
  ref(null)

const departmentForm =
  reactive({

    departmentId:
      null,

    scoreType:
      1,

    score:
      null,

    title:
      '',

    description:
      ''

  })


const departmentRules = {

  departmentId: [

    {
      required: true,
      message: '请选择部门',
      trigger: 'change'
    }

  ],

  scoreType: [

    {
      required: true,
      message: '请选择加减分',
      trigger: 'change'
    }

  ],

  score: [

    {
      required: true,
      message: '请输入分值',
      trigger: 'blur'
    }

  ],

  title: [

    {
      required: true,
      message: '请输入项目名称',
      trigger: 'blur'
    }

  ],

  description: [

    {
      required: true,
      message: '请输入申报说明',
      trigger: 'blur'
    }

  ]

}


/* =========================================================
   部门审核
========================================================= */

const auditLoading =
  ref(false)

const auditList =
  ref([])

const auditTableRef =
  ref(null)

const selectedAuditRows =
  ref([])

const batchAuditing =
  ref(false)

const auditingId =
  ref(null)


/* =========================================================
   审核分页
========================================================= */

const auditPageSize =
  ref(5)

const auditCurrentPage =
  ref(1)


const auditTotalPages =
  computed(() => {

    if (
      auditList.value.length === 0
    ) {

      return 0

    }


    return Math.ceil(
      auditList.value.length /
      auditPageSize.value
    )

  })


const pagedAuditList =
  computed(() => {

    const start =
      (
        auditCurrentPage.value -
        1
      ) *
      auditPageSize.value

    const end =
      start +
      auditPageSize.value

    return auditList.value.slice(
      start,
      end
    )

  })


/* =========================================================
   我的部门申报
========================================================= */

const listLoading =
  ref(false)

const myApplyList =
  ref([])


/* =========================================================
   获取权限
========================================================= */

async function loadPermission() {

  loadingPermission.value =
    true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/my-permissions'
      )


    const data =
      res.data?.data || {}


    permission.departments =
      Array.isArray(data.departments)
        ? data.departments
        : []


    permission.canDepartmentApply =
      Boolean(
        data.canDepartmentApply === true ||
        Number(data.canDepartmentApply) === 1 ||
        permission.departments.length > 0
      )


    permission.canDepartmentAudit =
      Boolean(
        data.canDepartmentAudit === true ||
        Number(data.canDepartmentAudit) === 1 ||
        permission.departments.some(
          item =>
            isDepartmentLeader(
              item.position
            )
        )
      )

  } catch (error) {

    console.error(
      '获取部门权限失败：',
      error
    )


    permission.departments =
      []

    permission.canDepartmentApply =
      false

    permission.canDepartmentAudit =
      false

    auditList.value =
      []

    ElMessage.error(
      getErrorMessage(
        error,
        '获取部门权限失败'
      )
    )

  } finally {

    loadingPermission.value =
      false

  }

}


/* =========================================================
   刷新权限
========================================================= */

async function refreshPermission() {

  await loadPermission()


  if (
    permission.canDepartmentAudit
  ) {

    await loadAuditList()

  } else {

    auditList.value =
      []

    clearAuditSelection()

  }

}


/* =========================================================
   刷新全部
========================================================= */

async function refreshAll() {

  await refreshPermission()

  await loadMyApply()

}


/* =========================================================
   获取部门审核列表
========================================================= */

async function loadAuditList() {

  if (
    !permission.canDepartmentAudit
  ) {

    auditList.value =
      []

    clearAuditSelection()

    return

  }


  auditLoading.value =
    true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/audit/list'
      )


    if (
      Number(res.data?.code) !== 200
    ) {

      throw new Error(
        res.data?.message ||
        res.data?.msg ||
        '获取待审核申请失败'
      )

    }


    const data =
      res.data?.data


    let list =
      []


    if (
      Array.isArray(data)
    ) {

      list =
        data

    } else if (
      data &&
      Array.isArray(data.records)
    ) {

      list =
        data.records

    } else if (
      data &&
      Array.isArray(data.list)
    ) {

      list =
        data.list

    }


    auditList.value =
      list.filter(
        item =>
          Number(item.status) === 0
      )


    const totalPages =
      auditTotalPages.value


    if (
      totalPages === 0
    ) {

      auditCurrentPage.value =
        1

    } else if (
      auditCurrentPage.value >
      totalPages
    ) {

      auditCurrentPage.value =
        totalPages

    }


    clearAuditSelection()

  } catch (error) {

    console.error(
      '获取部门待审核申请失败：',
      error
    )


    auditList.value =
      []

    clearAuditSelection()


    ElMessage.error(
      getErrorMessage(
        error,
        '获取待审核申请失败'
      )
    )

  } finally {

    auditLoading.value =
      false

  }

}


/* =========================================================
   审核分页
========================================================= */

function handleAuditPageChange(page) {

  auditCurrentPage.value =
    page

  clearAuditSelection()

}


/* =========================================================
   选择审核记录
========================================================= */

function handleAuditSelectionChange(rows) {

  selectedAuditRows.value =
    Array.isArray(rows)
      ? rows
      : []

}


/* =========================================================
   清空审核选择
========================================================= */

function clearAuditSelection() {

  selectedAuditRows.value =
    []


  auditTableRef.value
    ?.clearSelection()

}


/* =========================================================
   单条审核
========================================================= */

async function auditApply(
  row,
  status
) {

  if (
    !row ||
    row.id === null ||
    row.id === undefined
  ) {

    ElMessage.error(
      '申请数据异常，缺少申请 ID'
    )

    return

  }


  if (
    batchAuditing.value ||
    auditingId.value !== null
  ) {

    return

  }


  const actionText =
    Number(status) === 1
      ? '通过'
      : '驳回'


  let reviewRemark =
    ''


  /* -------------------------------------------------------
     输入审核意见
  ------------------------------------------------------- */

  try {

    const result =
      await ElMessageBox.prompt(

        Number(status) === 1
          ? '请输入审核意见（可不填）'
          : '请输入驳回原因（必填）',

        `部门申报${actionText}`,

        {

          confirmButtonText:
            '确定',

          cancelButtonText:
            '取消',

          inputType:
            'textarea',

          inputPlaceholder:
            Number(status) === 1
              ? '请输入审核意见（可不填）'
              : '请输入驳回原因',

          inputValidator:
            Number(status) === 2
              ? value => {

                return String(
                  value || ''
                ).trim()
                  ? true
                  : '驳回时必须填写原因'

              }
              : undefined

        }

      )


    reviewRemark =
      String(
        result.value || ''
      ).trim()

  } catch {

    return

  }


  /* -------------------------------------------------------
     二次确认
  ------------------------------------------------------- */

  try {

    await ElMessageBox.confirm(

      Number(status) === 1
        ? '确定通过这条部门加减分申报吗？通过后将进入辅导员终审。'
        : '确定驳回这条部门加减分申报吗？',

      '确认审核',

      {

        confirmButtonText:
          Number(status) === 1
            ? '确定通过'
            : '确定驳回',

        cancelButtonText:
          '取消',

        type:
          Number(status) === 1
            ? 'success'
            : 'warning'

      }

    )

  } catch {

    return

  }


  auditingId.value =
    row.id


  try {

    const res =
      await request.put(

        `/departmentScoreApply/audit/${row.id}`,

        {

          status:
            Number(status),

          reviewRemark:
          reviewRemark

        }

      )


    if (
      Number(res.data?.code) !== 200
    ) {

      throw new Error(
        res.data?.message ||
        res.data?.msg ||
        `部门申报${actionText}失败`
      )

    }


    ElMessage.success(

      Number(status) === 1
        ? '部门审核通过，已进入辅导员终审'
        : '部门申报已驳回'

    )


    await loadAuditList()

    await loadMyApply()

  } catch (error) {

    console.error(
      '部门审核失败：',
      error
    )


    ElMessage.error(
      getErrorMessage(
        error,
        `部门申报${actionText}失败`
      )
    )

  } finally {

    auditingId.value =
      null

  }

}


/* =========================================================
   批量通过选中
========================================================= */

async function batchAuditPass() {

  const rows =
    auditTableRef.value
      ?.getSelectionRows?.() ||
    []


  if (
    rows.length === 0
  ) {

    ElMessage.warning(
      '请先勾选要审核通过的申请'
    )

    return

  }


  if (
    batchAuditing.value ||
    auditingId.value !== null
  ) {

    return

  }


  const validRows =
    rows.filter(
      row =>
        row &&
        row.id !== null &&
        row.id !== undefined
    )


  if (
    validRows.length === 0
  ) {

    ElMessage.error(
      '选中的申请数据无效'
    )

    return

  }


  try {

    await ElMessageBox.confirm(

      `确定审核通过当前页选中的 ${validRows.length} 条部门申报吗？通过后将进入辅导员终审。`,

      '一键通过选中',

      {

        confirmButtonText:
          '全部通过',

        cancelButtonText:
          '取消',

        type:
          'success'

      }

    )

  } catch {

    return

  }


  batchAuditing.value =
    true


  let successCount =
    0

  let failCount =
    0


  try {

    for (
      const row of validRows
      ) {

      try {

        const res =
          await request.put(

            `/departmentScoreApply/audit/${row.id}`,

            {

              status:
                1,

              reviewRemark:
                ''

            }

          )


        if (
          Number(res.data?.code) === 200
        ) {

          successCount++

        } else {

          failCount++

        }

      } catch (error) {

        failCount++

        console.error(
          `申请 ${row.id} 审核失败：`,
          error
        )

      }

    }


    if (
      successCount > 0 &&
      failCount === 0
    ) {

      ElMessage.success(
        `成功审核通过 ${successCount} 条申请`
      )

    } else if (
      successCount > 0
    ) {

      ElMessage.warning(
        `成功通过 ${successCount} 条，失败 ${failCount} 条`
      )

    } else {

      ElMessage.error(
        `一键审核失败，共 ${failCount} 条`
      )

    }


    await loadAuditList()

    await loadMyApply()

  } finally {

    batchAuditing.value =
      false

  }

}


/* =========================================================
   一键全部通过
========================================================= */

async function batchAuditAllPass() {

  const rows =
    auditList.value.filter(
      row =>
        row &&
        row.id !== null &&
        row.id !== undefined &&
        Number(row.status) === 0
    )


  if (
    rows.length === 0
  ) {

    ElMessage.warning(
      '当前没有待审核的部门申报'
    )

    return

  }


  if (
    batchAuditing.value ||
    auditingId.value !== null
  ) {

    return

  }


  try {

    await ElMessageBox.confirm(

      `当前共有 ${rows.length} 条待审核部门申报，确定全部审核通过吗？通过后所有申请都将进入辅导员终审。`,

      '一键全部通过',

      {

        confirmButtonText:
          '全部通过',

        cancelButtonText:
          '取消',

        type:
          'warning'

      }

    )

  } catch {

    return

  }


  batchAuditing.value =
    true


  let successCount =
    0

  let failCount =
    0


  try {

    for (
      const row of rows
      ) {

      try {

        const res =
          await request.put(

            `/departmentScoreApply/audit/${row.id}`,

            {

              status:
                1,

              reviewRemark:
                ''

            }

          )


        if (
          Number(res.data?.code) === 200
        ) {

          successCount++

        } else {

          failCount++

        }

      } catch (error) {

        failCount++

        console.error(
          `申请 ${row.id} 请求失败：`,
          error
        )

      }

    }


    if (
      successCount > 0 &&
      failCount === 0
    ) {

      ElMessage.success(
        `已全部通过，共 ${successCount} 条申请`
      )

    } else if (
      successCount > 0
    ) {

      ElMessage.warning(
        `成功通过 ${successCount} 条，失败 ${failCount} 条`
      )

    } else {

      ElMessage.error(
        `一键全部通过失败，共 ${failCount} 条`
      )

    }


    auditCurrentPage.value =
      1


    await loadAuditList()

    await loadMyApply()

  } finally {

    batchAuditing.value =
      false

  }

}


/* =========================================================
   部门凭证
========================================================= */

function handleDepartmentFile(file) {

  departmentFile.value =
    file?.raw || null

}


function removeDepartmentFile() {

  departmentFile.value =
    null

}


/* =========================================================
   提交部门申报
========================================================= */

async function submitDepartment() {

  if (
    !departmentFormRef.value
  ) {

    return

  }


  const valid =
    await departmentFormRef.value
      .validate()
      .catch(
        () => false
      )


  if (!valid) {

    return

  }


  departmentSubmitting.value =
    true


  try {

    const data = {

      departmentId:
      departmentForm.departmentId,

      scoreType:
      departmentForm.scoreType,

      score:
      departmentForm.score,

      title:
      departmentForm.title,

      description:
      departmentForm.description,

      evidenceUrl:
        departmentFile.value
          ? departmentFile.value.name
          : null

    }


    const res =
      await request.post(
        '/departmentScoreApply/add',
        data
      )


    if (
      Number(res.data?.code) !== 200
    ) {

      throw new Error(
        res.data?.message ||
        res.data?.msg ||
        '部门申报提交失败'
      )

    }


    ElMessage.success(
      '部门申报提交成功，等待本部门副部或部长审核'
    )


    resetDepartment()

    await loadMyApply()


    if (
      permission.canDepartmentAudit
    ) {

      await loadAuditList()

    }

  } catch (error) {

    console.error(
      '部门申报提交失败：',
      error
    )


    ElMessage.error(
      getErrorMessage(
        error,
        '部门申报提交失败'
      )
    )

  } finally {

    departmentSubmitting.value =
      false

  }

}


/* =========================================================
   重置部门申报
========================================================= */

function resetDepartment() {

  departmentForm.departmentId =
    null

  departmentForm.scoreType =
    1

  departmentForm.score =
    null

  departmentForm.title =
    ''

  departmentForm.description =
    ''

  departmentFile.value =
    null


  departmentFormRef.value
    ?.clearValidate()

}


/* =========================================================
   我的部门申报记录
========================================================= */

async function loadMyApply() {

  listLoading.value =
    true


  try {

    const res =
      await request.get(
        '/departmentScoreApply/my'
      )


    if (
      Number(res.data?.code) !== 200
    ) {

      throw new Error(
        res.data?.message ||
        res.data?.msg ||
        '获取部门申报记录失败'
      )

    }


    const data =
      res.data?.data


    let list =
      []


    if (
      Array.isArray(data)
    ) {

      list =
        data

    } else if (
      data &&
      Array.isArray(data.records)
    ) {

      list =
        data.records

    } else if (
      data &&
      Array.isArray(data.list)
    ) {

      list =
        data.list

    }


    myApplyList.value =
      list
        .map(
          item => ({

            ...item,

            title:
              item.title ||
              '部门加减分申报',

            departmentName:
              item.departmentName ||
              null,

            reviewRemark:
              item.reviewRemark ||
              null,

            finalReviewRemark:
              item.finalReviewRemark ||
              null,

            finalStatus:
              item.finalStatus ?? 0

          })
        )
        .sort(
          (a, b) => {

            const timeA =
              new Date(
                a.createTime || 0
              ).getTime()

            const timeB =
              new Date(
                b.createTime || 0
              ).getTime()

            return (
              timeB -
              timeA
            )

          }
        )

  } catch (error) {

    console.error(
      '获取部门申报记录失败：',
      error
    )


    myApplyList.value =
      []


    ElMessage.error(
      getErrorMessage(
        error,
        '获取部门申报记录失败'
      )
    )

  } finally {

    listLoading.value =
      false

  }

}


/* =========================================================
   错误信息
========================================================= */

function getErrorMessage(
  error,
  defaultMessage
) {

  return (
    error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    defaultMessage
  )

}


/* =========================================================
   初始化
========================================================= */

onMounted(
  async () => {

    await refreshAll()

  }
)

</script>


<style scoped>

.score-apply-page {

  padding:
    24px;

  min-height:
    calc(100vh - 60px);

  background:
    #f5f7fa;

}


.page-header {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    center;

  margin-bottom:
    20px;

}


.page-header h2 {

  margin:
    0 0 6px;

  font-size:
    24px;

  color:
    #303133;

}


.page-header p {

  margin:
    0;

  color:
    #909399;

}


.apply-card {

  margin-bottom:
    20px;

}


.card-header {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    flex-start;

  margin-bottom:
    20px;

}


.card-header h3 {

  margin:
    0 0 8px;

  font-size:
    18px;

  color:
    #303133;

}


.card-header p {

  margin:
    0;

  color:
    #909399;

  font-size:
    14px;

}


.audit-header-actions {

  display:
    flex;

  align-items:
    center;

  gap:
    10px;

  flex-wrap:
    wrap;

}


.selected-text {

  color:
    #909399;

  font-size:
    14px;

  white-space:
    nowrap;

}


.selected-text strong {

  color:
    #409eff;

  font-size:
    16px;

}


.audit-page-tip {

  display:
    flex;

  align-items:
    center;

  gap:
    18px;

  margin-bottom:
    15px;

  padding:
    10px 14px;

  border-radius:
    6px;

  background:
    #f5f7fa;

  color:
    #606266;

  font-size:
    13px;

}


.audit-page-tip strong {

  color:
    #409eff;

}


.tip-divider {

  color:
    #dcdfe6;

}


.audit-pagination {

  display:
    flex;

  justify-content:
    flex-end;

  margin-top:
    20px;

}


.apply-form {

  max-width:
    800px;

}


.upload-tip {

  color:
    #909399;

  font-size:
    12px;

}


.bonus {

  color:
    #67c23a;

  font-weight:
    700;

}


.deduct {

  color:
    #f56c6c;

  font-weight:
    700;

}


.department-list {

  display:
    flex;

  flex-direction:
    column;

  gap:
    12px;

}


.department-item {

  padding:
    16px;

  border:
    1px solid #ebeef5;

  border-radius:
    8px;

  background:
    #fafafa;

}


.department-info {

  display:
    flex;

  align-items:
    center;

  justify-content:
    space-between;

  gap:
    12px;

}


.department-name {

  font-size:
    16px;

  font-weight:
    600;

  color:
    #303133;

}


.department-tip {

  margin-top:
    8px;

  font-size:
    13px;

  color:
    #909399;

}


@media (max-width: 1100px) {

  .card-header {

    flex-direction:
      column;

    gap:
      15px;

  }


  .audit-header-actions {

    width:
      100%;

    justify-content:
      flex-start;

  }

}


@media (max-width: 700px) {

  .score-apply-page {

    padding:
      12px;

  }


  .audit-page-tip {

    flex-wrap:
      wrap;

  }

}

</style>
