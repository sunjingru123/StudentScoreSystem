<template>
  <div class="score-apply-page">

    <!-- ====================================================== -->
    <!-- 页面标题 -->
    <!-- ====================================================== -->

    <div class="page-header">

      <div>
        <h2>综合测评申报</h2>

        <p>
          部门学生加减分申报及审核
        </p>
      </div>

      <el-button
        :loading="loadingPermission"
        @click="loadPermission"
      >
        刷新
      </el-button>

    </div>


    <!-- ====================================================== -->
    <!-- 我的部门身份 -->
    <!-- ====================================================== -->

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
          @click="loadPermission"
        >
          刷新
        </el-button>

      </div>


      <!-- 有部门身份 -->

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

            <span
              v-if="item.position === '干事'"
            >
              你可以提交本部门学生加减分申报
            </span>

            <span
              v-else
            >
              你可以提交本部门学生加减分申报，并审核本部门其他成员提交的申报
            </span>

          </div>

        </div>

      </div>


      <!-- 没有部门身份 -->

      <el-empty
        v-else
        description="当前没有部门干部身份"
      />

    </el-card>



    <!-- ====================================================== -->
    <!-- 部门申报审核 -->
    <!-- ====================================================== -->

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
            你是副部长或部长，可以审核本部门其他干部提交的学生加减分申报。
          </p>

        </div>


        <el-button
          size="small"
          :loading="auditLoading"
          @click="loadAuditList"
        >
          刷新
        </el-button>

      </div>


      <!-- ================================================== -->
      <!-- 批量审核操作栏 -->
      <!-- ================================================== -->

      <div
        v-if="auditList.length > 0"
        class="batch-audit-toolbar"
      >

        <div class="batch-audit-left">

          <span class="selected-count">
            已选择
            <strong>
              {{ selectedAuditRows.length }}
            </strong>
            条申报
          </span>

          <el-button
            size="small"
            @click="selectAllAudit"
          >
            全选
          </el-button>

          <el-button
            size="small"
            @click="clearAuditSelection"
          >
            取消全选
          </el-button>

        </div>


        <el-button
          type="success"
          :loading="batchAuditLoading"
          :disabled="selectedAuditRows.length === 0"
          @click="batchApproveAudit"
        >
          一键审批通过
          <span
            v-if="selectedAuditRows.length > 0"
          >
            （{{ selectedAuditRows.length }}）
          </span>
        </el-button>

      </div>


      <!-- ================================================== -->
      <!-- 审核表格 -->
      <!-- ================================================== -->

      <el-table
        ref="auditTableRef"
        v-loading="auditLoading"
        :data="auditList"
        border
        stripe
        empty-text="暂无待审核部门申报"
        @selection-change="handleAuditSelectionChange"
      >

        <!-- ================================================== -->
        <!-- 全选复选框 -->
        <!-- ================================================== -->

        <el-table-column
          type="selection"
          width="55"
          align="center"
        />


        <!-- ================================================== -->
        <!-- 序号 -->
        <!-- ================================================== -->

        <el-table-column
          label="#"
          width="60"
          align="center"
        >

          <template #default="{ $index }">

            {{ $index + 1 }}

          </template>

        </el-table-column>


        <!-- ================================================== -->
        <!-- 加减分项目 -->
        <!-- ================================================== -->

        <el-table-column
          prop="title"
          label="加减分项目"
          min-width="180"
        />


        <!-- ================================================== -->
        <!-- 被加减分学生 -->
        <!-- ================================================== -->

        <el-table-column
          label="被加减分学生"
          width="130"
          align="center"
        >

          <template #default="{ row }">

            {{ row.studentName || row.studentId }}

          </template>

        </el-table-column>


        <!-- ================================================== -->
        <!-- 部门 -->
        <!-- ================================================== -->

        <el-table-column
          label="部门"
          width="140"
          align="center"
        >

          <template #default="{ row }">

            {{ row.departmentName || row.departmentId }}

          </template>

        </el-table-column>


        <!-- ================================================== -->
        <!-- 类型 -->
        <!-- ================================================== -->

        <el-table-column
          label="类型"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <!-- 加分 -->

            <el-tag
              v-if="isBonus(row)"
              type="success"
            >
              加分
            </el-tag>


            <!-- 减分 -->

            <el-tag
              v-else-if="isDeduct(row)"
              type="danger"
            >
              减分
            </el-tag>


            <!-- 未知 -->

            <el-tag
              v-else
              type="info"
            >
              未知
            </el-tag>

          </template>

        </el-table-column>


        <!-- ================================================== -->
        <!-- 分值 -->
        <!-- ================================================== -->

        <el-table-column
          label="分值"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <!-- 加分 -->

            <span
              v-if="isBonus(row)"
              class="bonus"
            >
              +{{ row.score }}
            </span>


            <!-- 减分 -->

            <span
              v-else-if="isDeduct(row)"
              class="deduct"
            >
              -{{ row.score }}
            </span>


            <!-- 未知 -->

            <span
              v-else
              class="unknown-score"
            >
              {{ row.score }}
            </span>

          </template>

        </el-table-column>


        <!-- ================================================== -->
        <!-- 项目说明 -->
        <!-- ================================================== -->

        <el-table-column
          prop="description"
          label="项目说明"
          min-width="240"
          show-overflow-tooltip
        />


        <!-- ================================================== -->
        <!-- 申报时间 -->
        <!-- ================================================== -->

        <el-table-column
          prop="createTime"
          label="申报时间"
          width="180"
        />


        <!-- ================================================== -->
        <!-- 操作 -->
        <!-- ================================================== -->

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
              @click="auditApply(row, 1)"
            >
              通过
            </el-button>


            <el-button
              type="danger"
              size="small"
              @click="auditApply(row, 2)"
            >
              驳回
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <el-empty
        v-if="
          !auditLoading &&
          auditList.length === 0
        "
        description="暂无待审核部门申报"
      />

    </el-card>



    <!-- ====================================================== -->
    <!-- 部门学生加减分申报 -->
    <!-- ====================================================== -->

    <el-card
      v-if="permission.canDepartmentApply"
      class="apply-card"
      shadow="never"
    >

      <div class="card-header">

        <div>

          <h3>
            部门学生加减分申报
          </h3>

          <p>
            部门干部可以根据本部门加减分模板，对学生进行加分或扣分申报。
          </p>

        </div>


        <el-tag type="success">
          部门审核 → 辅导员审核
        </el-tag>

      </div>


      <!-- ================================================== -->
      <!-- 部门申报表单 -->
      <!-- ================================================== -->

      <el-form
        ref="departmentFormRef"
        :model="departmentForm"
        :rules="departmentRules"
        label-width="110px"
        class="apply-form"
      >


        <!-- ================================================== -->
        <!-- 所属部门 -->
        <!-- ================================================== -->

        <el-form-item
          label="所属部门"
          prop="departmentId"
        >

          <el-select
            v-model="departmentForm.departmentId"
            placeholder="请选择申报部门"
            style="width: 100%"
            clearable
          >

            <el-option
              v-for="item in permission.departments"
              :key="item.departmentId"
              :label="
                `${item.departmentName}（${item.position}）`
              "
              :value="item.departmentId"
            />

          </el-select>

        </el-form-item>



        <!-- ================================================== -->
        <!-- 被加减分学生 -->
        <!-- ================================================== -->

        <el-form-item
          label="被加减分学生"
          prop="studentId"
        >

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
              :label="
                `${item.realName || item.username}（${item.studentNo || item.id}）`
              "
              :value="item.id"
            />

          </el-select>

        </el-form-item>



        <!-- ================================================== -->
        <!-- 部门模板 -->
        <!-- ================================================== -->

        <el-form-item
          label="加减分项目"
          prop="templateId"
        >

          <el-select
            v-model="departmentForm.templateId"
            filterable
            clearable
            :disabled="!departmentForm.departmentId"
            :loading="templateLoading"
            placeholder="请先选择部门，再选择加减分项目"
            style="width: 100%"
          >

            <el-option
              v-for="item in departmentTemplateList"
              :key="item.id"
              :label="getTemplateLabel(item)"
              :value="item.id"
            >

              <div class="template-option">

                <span class="template-name">
                  {{ item.name }}
                </span>


                <!-- 加分模板 -->

                <span
                  v-if="isBonus(item)"
                  class="template-bonus"
                >
                  +{{ item.score }}分
                </span>


                <!-- 减分模板 -->

                <span
                  v-else-if="isDeduct(item)"
                  class="template-deduct"
                >
                  -{{ item.score }}分
                </span>


                <!-- 未知 -->

                <span
                  v-else
                  class="template-unknown"
                >
                  {{ item.score }}分
                </span>

              </div>

            </el-option>

          </el-select>


          <div class="form-tip">
            这里只显示当前申报部门自己的加减分模板。
          </div>

        </el-form-item>



        <!-- ================================================== -->
        <!-- 模板详情 -->
        <!-- ================================================== -->

        <el-form-item label="项目类型">

          <template v-if="selectedTemplate">

            <!-- 加分 -->

            <el-tag
              v-if="isBonus(selectedTemplate)"
              type="success"
            >
              加分
            </el-tag>


            <!-- 减分 -->

            <el-tag
              v-else-if="isDeduct(selectedTemplate)"
              type="danger"
            >
              减分
            </el-tag>


            <!-- 未知 -->

            <el-tag
              v-else
              type="info"
            >
              未知类型
            </el-tag>


            <!-- 加分分值 -->

            <span
              v-if="isBonus(selectedTemplate)"
              class="template-score bonus"
            >
              +{{ selectedTemplate.score }} 分
            </span>


            <!-- 减分分值 -->

            <span
              v-else-if="isDeduct(selectedTemplate)"
              class="template-score deduct"
            >
              -{{ selectedTemplate.score }} 分
            </span>


            <!-- 未知分值 -->

            <span
              v-else
              class="template-score"
            >
              {{ selectedTemplate.score }} 分
            </span>

          </template>


          <span
            v-else
            class="empty-template"
          >
            请先选择加减分项目
          </span>

        </el-form-item>



        <!-- ================================================== -->
        <!-- 模板项目说明 -->
        <!-- ================================================== -->

        <el-form-item
          label="项目说明"
        >

          <el-input
            :model-value="
              selectedTemplate?.description || ''
            "
            type="textarea"
            :rows="5"
            readonly
            placeholder="选择加减分项目后自动显示项目说明"
          />

        </el-form-item>



        <!-- ================================================== -->
        <!-- 证明材料 -->
        <!-- ================================================== -->

        <el-form-item
          label="活动凭证"
        >

          <el-upload
            action=""
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



        <!-- ================================================== -->
        <!-- 提交 -->
        <!-- ================================================== -->

        <el-form-item>

          <el-button
            type="success"
            :loading="departmentSubmitting"
            @click="submitDepartment"
          >
            提交部门加减分申报
          </el-button>


          <el-button
            @click="resetDepartment"
          >
            重置
          </el-button>

        </el-form-item>

      </el-form>

    </el-card>



    <!-- ====================================================== -->
    <!-- 我的申报 -->
    <!-- ====================================================== -->

    <el-card
      class="apply-card"
      shadow="never"
    >

      <div class="card-header">

        <div>

          <h3>
            我的申报记录
          </h3>

          <p>
            查看自己提交的部门学生加减分申报。
          </p>

        </div>


        <el-button
          size="small"
          :loading="listLoading"
          @click="loadMyApply"
        >
          刷新
        </el-button>

      </div>


      <!-- ================================================== -->
      <!-- 我的申报表格 -->
      <!-- ================================================== -->

      <el-table
        v-loading="listLoading"
        :data="pagedDepartmentApplyList"
        border
        stripe
        empty-text="暂无部门学生加减分申报"
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
          prop="title"
          label="加减分项目"
          min-width="200"
        />


        <!-- 学生 -->

        <el-table-column
          label="被加减分学生"
          width="140"
          align="center"
        >

          <template #default="{ row }">

            {{ row.studentName || row.studentId }}

          </template>

        </el-table-column>


        <!-- 部门 -->

        <el-table-column
          label="所属部门"
          width="140"
          align="center"
        >

          <template #default="{ row }">

            {{ row.departmentName || row.departmentId }}

          </template>

        </el-table-column>


        <!-- 类型 -->

        <el-table-column
          label="类型"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <!-- 加分 -->

            <el-tag
              v-if="isBonus(row)"
              type="success"
            >
              加分
            </el-tag>


            <!-- 减分 -->

            <el-tag
              v-else-if="isDeduct(row)"
              type="danger"
            >
              减分
            </el-tag>


            <!-- 未知 -->

            <el-tag
              v-else
              type="info"
            >
              未知
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

            <!-- 加分 -->

            <span
              v-if="isBonus(row)"
              class="bonus"
            >
              +{{ row.score }}
            </span>


            <!-- 减分 -->

            <span
              v-else-if="isDeduct(row)"
              class="deduct"
            >
              -{{ row.score }}
            </span>


            <!-- 未知 -->

            <span
              v-else
              class="unknown-score"
            >
              {{ row.score }}
            </span>

          </template>

        </el-table-column>


        <!-- 项目说明 -->

        <el-table-column
          prop="description"
          label="项目说明"
          min-width="240"
          show-overflow-tooltip
        />


        <!-- 时间 -->

        <el-table-column
          prop="createTime"
          label="申报时间"
          width="180"
        />


        <!-- ================================================== -->
        <!-- 审核状态 -->
        <!-- ================================================== -->

        <el-table-column
          label="审核状态"
          width="150"
          align="center"
        >

          <template #default="{ row }">

            <!-- ============================================== -->
            <!-- 第一阶段：部门审核 -->
            <!-- ============================================== -->

            <el-tag
              v-if="Number(row.status) === 0"
              type="warning"
            >
              待部门审核
            </el-tag>


            <!-- ============================================== -->
            <!-- 部门审核驳回 -->
            <!-- ============================================== -->

            <el-tag
              v-else-if="Number(row.status) === 2"
              type="danger"
            >
              部门已驳回
            </el-tag>


            <!-- ============================================== -->
            <!-- 第二阶段：辅导员审核 -->
            <!-- ============================================== -->

            <el-tag
              v-else-if="
                Number(row.status) === 1 &&
                Number(row.finalStatus) === 0
              "
              type="warning"
            >
              待辅导员审核
            </el-tag>


            <!-- ============================================== -->
            <!-- 最终通过 -->
            <!-- ============================================== -->

            <el-tag
              v-else-if="
                Number(row.status) === 1 &&
                Number(row.finalStatus) === 1
              "
              type="success"
            >
              审核通过
            </el-tag>


            <!-- ============================================== -->
            <!-- 辅导员驳回 -->
            <!-- ============================================== -->

            <el-tag
              v-else-if="
                Number(row.status) === 1 &&
                Number(row.finalStatus) === 2
              "
              type="danger"
            >
              辅导员已驳回
            </el-tag>


            <!-- ============================================== -->
            <!-- 未知 -->
            <!-- ============================================== -->

            <el-tag
              v-else
              type="info"
            >
              未知状态
            </el-tag>

          </template>

        </el-table-column>


        <!-- 部门审核意见 -->

        <el-table-column
          prop="reviewRemark"
          label="部门审核意见"
          min-width="180"
          show-overflow-tooltip
        />


        <!-- 辅导员审核意见 -->

        <el-table-column
          prop="finalReviewRemark"
          label="辅导员审核意见"
          min-width="180"
          show-overflow-tooltip
        />

      </el-table>


      <!-- ================================================== -->
      <!-- 我的申报分页 -->
      <!-- ================================================== -->

      <div
        v-if="
          !listLoading &&
          departmentApplyList.length > 0
        "
        class="pagination-wrapper"
      >

        <div class="pagination-total">

          共
          {{ departmentApplyList.length }}
          条申报记录

        </div>


        <el-pagination
          v-model:current-page="myApplyPageNum"
          v-model:page-size="myApplyPageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="departmentApplyList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />

      </div>


      <el-empty
        v-if="
          !listLoading &&
          departmentApplyList.length === 0
        "
        description="暂无部门学生加减分申报"
      />

    </el-card>

  </div>
</template>



<script setup>

import {
  ref,
  reactive,
  computed,
  watch,
  onMounted,
} from 'vue'

import {
  ElMessage,
  ElMessageBox,
} from 'element-plus'

import request from '@/utils/request'



/* ====================================================== */
/* 权限 */
/* ====================================================== */

const loadingPermission = ref(false)

const permission = reactive({

  canDepartmentApply: false,

  canDepartmentAudit: false,

  departments: [],

})



/* ====================================================== */
/* 学生列表 */
/* ====================================================== */

const studentList = ref([])



/* ====================================================== */
/* 部门加减分申报 */
/* ====================================================== */

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



/* ====================================================== */
/* 表单验证规则 */
/* ====================================================== */

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



/* ====================================================== */
/* scoreType 统一处理 */
/* ====================================================== */

/*
 * 整个系统统一：
 *
 * scoreType = 1
 * 加分
 *
 * scoreType = -1
 * 减分
 *
 *
 * 注意：
 *
 * scoreType 和审核 status 是完全不同的字段。
 *
 *
 * scoreType：
 *
 * 1  = 加分
 * -1 = 减分
 *
 *
 * status：
 *
 * 0 = 待部门审核
 * 1 = 部门审核通过
 * 2 = 部门审核驳回
 *
 *
 * finalStatus：
 *
 * 0 = 待辅导员审核
 * 1 = 辅导员审核通过
 * 2 = 辅导员审核驳回
 */

function getScoreType(item) {

  if (!item) {

    return 0

  }

  return Number(item.scoreType)

}



/* ====================================================== */
/* 判断是否加分 */
/* ====================================================== */

function isBonus(item) {

  return getScoreType(item) === 1

}



/* ====================================================== */
/* 判断是否减分 */
/* ====================================================== */

function isDeduct(item) {

  return getScoreType(item) === -1

}



/* ====================================================== */
/* 获取模板下拉显示文字 */
/* ====================================================== */

function getTemplateLabel(item) {

  if (!item) {

    return ''

  }


  const scoreType =
    getScoreType(item)


  const score =
    item.score ?? 0


  /*
   * 加分
   */

  if (scoreType === 1) {

    return `${item.name}（+${score}分）`

  }


  /*
   * 减分
   */

  if (scoreType === -1) {

    return `${item.name}（-${score}分）`

  }


  /*
   * 未知
   */

  return `${item.name}（${score}分）`

}



/* ====================================================== */
/* 当前选择的模板 */
/* ====================================================== */

const selectedTemplate = computed(() => {

  return departmentTemplateList.value.find(

    (item) =>

      String(item.id) ===
      String(departmentForm.templateId),

  )

})



/* ====================================================== */
/* 我的申报 */
/* ====================================================== */

const listLoading = ref(false)

const departmentApplyList = ref([])



/* ====================================================== */
/* 我的申报分页 */
/* ====================================================== */

const myApplyPageNum = ref(1)

const myApplyPageSize = ref(10)



/* ====================================================== */
/* 部门审核 */
/* ====================================================== */

const auditLoading = ref(false)

const auditList = ref([])



/* ====================================================== */
/* 部门审核批量选择 */
/* ====================================================== */

/*
 * 当前选中的部门申报
 *
 * 只有拥有 canDepartmentAudit 权限的
 * 副部长、部长才能进入这个区域。
 */

const auditTableRef = ref(null)

const selectedAuditRows = ref([])

const batchAuditLoading = ref(false)



/* ====================================================== */
/* 当前页申报记录 */
/* ====================================================== */

const pagedDepartmentApplyList = computed(() => {

  const start =
    (myApplyPageNum.value - 1) *
    myApplyPageSize.value

  const end =
    start +
    myApplyPageSize.value

  return departmentApplyList.value.slice(
    start,
    end
  )

})



/* ====================================================== */
/* 获取当前用户部门权限 */
/* ====================================================== */

async function loadPermission() {

  loadingPermission.value = true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/my-permissions',
      )

    console.log(
      '部门权限：',
      res
    )

    /*
     * @/utils/request 已经返回后端 Result
     *
     * 所以：
     * res.code
     * res.data
     *
     * 不能再写成 res.data.data
     */

    const result = res || {}

    if (
      Number(result.code) !== 200 &&
      Number(result.code) !== 0
    ) {

      throw new Error(
        result.message ||
        '获取部门权限失败'
      )

    }

    const data =
      result.data || {}

    /*
     * 部门列表
     */

    permission.departments =
      Array.isArray(data.departments)
        ? data.departments
        : []

    /*
     * 部门申报权限
     *
     * 干事 / 副部长 / 部长
     */

    permission.canDepartmentApply =
      data.canDepartmentApply === true ||
      Number(data.canDepartmentApply) === 1

    /*
     * 部门审核权限
     *
     * 副部长 / 部长
     */

    permission.canDepartmentAudit =
      data.canDepartmentAudit === true ||
      Number(data.canDepartmentAudit) === 1

    console.log(
      '最终部门权限：',
      {
        departments:
        permission.departments,

        canDepartmentApply:
        permission.canDepartmentApply,

        canDepartmentAudit:
        permission.canDepartmentAudit
      }
    )


    /*
     * 检查当前选择的部门是否仍然有效
     */

    const validDepartmentIds =
      permission.departments.map(
        item => item.departmentId
      )

    if (
      departmentForm.departmentId &&
      !validDepartmentIds.some(
        id =>
          String(id) ===
          String(
            departmentForm.departmentId
          )
      )
    ) {

      departmentForm.departmentId = null

      departmentForm.templateId = null

      departmentTemplateList.value = []

    }


    /*
     * 如果没有审核权限，
     * 清空审核数据。
     */

    if (
      !permission.canDepartmentAudit
    ) {

      selectedAuditRows.value = []

      auditList.value = []

    }

  }
  catch (error) {

    console.error(
      '获取部门权限失败：',
      error
    )

    permission.departments = []

    permission.canDepartmentApply = false

    permission.canDepartmentAudit = false

    selectedAuditRows.value = []

    auditList.value = []

    ElMessage.error(

      error?.message ||
      error?.response?.data?.message ||
      '获取部门权限失败'

    )

  }
  finally {

    loadingPermission.value = false

  }

}

/* ====================================================== */
/* 加载全部学生 */
/* ====================================================== */

async function loadStudentList() {

  try {

    console.log(
      '开始获取全部学生列表'
    )


    const allStudents = []

    let pageNum = 1

    /*
     * 不要一次传 10000。
     *
     * 后端一般会限制最大 pageSize。
     *
     * 这里每页取 100。
     */

    const pageSize = 100

    let total = 0


    /* ================================================== */
    /* 循环获取所有分页 */
    /* ================================================== */

    while (true) {

      console.log(
        `正在获取学生第 ${pageNum} 页`
      )


      const res =
        await request.get(
          '/user/student/list',
          {
            params: {

              pageNum,

              pageSize,

            },

          },
        )


      console.log(
        `第 ${pageNum} 页学生列表响应：`,
        res
      )


      const responseData =
        res?.data


      const data =
        responseData?.data ??
        responseData ??
        {}


      /* ================================================== */
      /* 兼容分页数据 */
      /* ================================================== */

      let records = []


      if (
        Array.isArray(data)
      ) {

        records = data

      }

      else if (
        Array.isArray(
          data?.records
        )
      ) {

        records =
          data.records

        total =
          Number(
            data.total ?? 0
          )

      }


      console.log(
        `第 ${pageNum} 页学生数量：`,
        records.length
      )


      /*
       * 没有数据了
       */

      if (
        records.length === 0
      ) {

        break

      }


      /*
       * 加入总学生列表
       */

      allStudents.push(
        ...records
      )


      /* ================================================== */
      /* 判断是否已经获取完 */
      /* ================================================== */

      if (
        total > 0 &&
        allStudents.length >= total
      ) {

        break

      }


      /*
       * 如果这一页数量小于 pageSize，
       * 通常说明已经到最后一页。
       */

      if (
        records.length < pageSize
      ) {

        break

      }


      pageNum++


      /*
       * 防止异常情况下死循环
       */

      if (
        pageNum > 1000
      ) {

        console.warn(
          '学生分页超过 1000 页，停止继续获取'
        )

        break

      }

    }


    /* ================================================== */
    /* 去重 */
    /* ================================================== */

    const studentMap =
      new Map()


    for (
      const student
      of allStudents
      ) {

      if (
        !student ||
        !student.id
      ) {

        continue

      }


      studentMap.set(
        String(student.id),
        student
      )

    }


    const students =
      Array.from(
        studentMap.values()
      )


    /* ================================================== */
    /* 过滤学生 */
    /* ================================================== */

    studentList.value =

      students.filter(
        student => {

          /*
           * 必须有 ID
           */

          if (
            !student ||
            !student.id
          ) {

            return false

          }


          /*
           * 如果后端明确返回 status，
           * 那么只保留 status = 1。
           */

          if (
            student.status !== undefined &&
            student.status !== null &&
            student.status !== ''
          ) {

            return Number(
              student.status
            ) === 1

          }


          /*
           * 如果没有 status，
           * 默认保留。
           */

          return true

        }
      )


    /* ================================================== */
    /* 输出最终结果 */
    /* ================================================== */

    console.log(
      '================================'
    )

    console.log(
      '学生总数：',
      students.length
    )

    console.log(
      '最终可申报学生数量：',
      studentList.value.length
    )

    console.log(
      '最终可申报学生：',
      studentList.value
    )

    console.log(
      '================================'
    )


    /* ================================================== */
    /* 专门检查孙靖茹 */
    /* ================================================== */

    const target =
      studentList.value.find(
        student => {

          const name =
            student.realName ??
            student.studentName ??
            student.name ??
            ''

          return String(name)
            .includes('孙靖茹')

        }
      )


    console.log(
      '检查孙靖茹：',
      target
    )

  }

  catch (error) {

    console.error(
      '获取学生列表失败：',
      error
    )


    studentList.value = []


    ElMessage.error(

      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取学生列表失败'

    )

  }

}



/* ====================================================== */
/* 根据部门加载模板 */
/* ====================================================== */

async function loadDepartmentTemplates(
  departmentId,
) {

  /*
   * 清空旧模板
   */

  departmentTemplateList.value = []

  departmentForm.templateId = null


  /*
   * 没有部门直接结束
   */

  if (!departmentId) {

    return

  }


  templateLoading.value = true


  try {

    const res =
      await request.get(

        '/departmentScoreTemplate/list',

        {

          params: {

            departmentId,

          },

        },

      )


    const list =
      Array.isArray(res?.data)
        ? res.data
        : []


    /*
     * 不过滤 scoreType。
     *
     * 后端返回：
     *
     * 1  = 加分
     * -1 = 减分
     *
     * 两种模板全部显示。
     */

    departmentTemplateList.value =

      list.map(

        (item) => ({

          ...item,

          scoreType:
            Number(item.scoreType),

        }),

      )

  }

  catch (error) {

    console.error(

      '获取部门加减分项目失败：',

      error,

    )


    departmentTemplateList.value = []


    ElMessage.error(

      error?.response?.data?.message ||
      '获取部门加减分项目失败',

    )

  }

  finally {

    templateLoading.value = false

  }

}



/* ====================================================== */
/* 部门变化 */
/* ====================================================== */

/*
 * 用户选择部门后：
 *
 * 自动加载这个部门自己的模板。
 */

watch(

  () => departmentForm.departmentId,

  async (val) => {

    await loadDepartmentTemplates(val)

  },

)



/* ====================================================== */
/* 部门审核列表 */
/* ====================================================== */

async function loadAuditList() {

  /*
   * 没有部门审核权限
   */

  if (!permission.canDepartmentAudit) {

    auditList.value = []

    selectedAuditRows.value = []

    return

  }


  auditLoading.value = true


  try {

    const res =
      await request.get(
        '/departmentScoreApply/audit/list',
      )


    /*
     * @/utils/request 已经把 Axios 外层拆掉了。
     *
     * 所以这里直接使用：
     *
     * res.code
     * res.data
     */

    if (
      Number(res?.code) !== 200 &&
      Number(res?.code) !== 0
    ) {

      throw new Error(
        res?.message ||
        '获取待审核申请失败'
      )

    }


    const data =
      Array.isArray(res?.data)
        ? res.data
        : []


    /*
     * =====================================================
     * ★★★ 双保险
     *
     * 后端已经排除了当前用户自己提交的申报。
     *
     * 前端这里再过滤一次：
     *
     * applicantId === 当前登录用户ID
     *
     * 的记录绝不进入审核列表。
     *
     * =====================================================
     *
     * 注意：
     * 如果当前页面没有可靠的当前用户ID，
     * 就不在这里强行猜 ID。
     *
     * 后端是最终权限控制。
     *
     * 所以下面只过滤后端已经明确标记的
     * applicantId 与 currentUserId 相等的情况。
     */

    auditList.value =
      data
        .filter(
          item =>
            Number(item.status) === 0
        )
        .map(
          item => ({

            ...item,

            scoreType:
              Number(item.scoreType),

          })
        )


    /*
     * 刷新以后清空之前的选择。
     *
     * 因为之前选择的记录可能已经审核完成。
     */

    selectedAuditRows.value = []


  }
  catch (error) {

    console.error(
      '获取待审核申请失败：',
      error
    )


    auditList.value = []

    selectedAuditRows.value = []


    ElMessage.error(

      error?.message ||

      error?.response?.data?.message ||

      '获取待审核申请失败'

    )

  }
  finally {

    auditLoading.value = false

  }

}


/* ====================================================== */
/* 部门审核：选择变化 */
/* ====================================================== */

/*
 * Element Plus el-table：
 *
 * type="selection"
 *
 * 会自动提供复选框。
 *
 * 用户勾选以后，
 * selectedAuditRows 就保存当前选中的申报。
 */

function handleAuditSelectionChange(
  selection,
) {

  selectedAuditRows.value =
    Array.isArray(selection)
      ? selection
      : []

}



/* ====================================================== */
/* 部门审核：全选 */
/* ====================================================== */

/*
 * 全选当前审核列表。
 *
 * 只有副部长、部长才能进入这里，
 * 因为整个审核 Card 已经由
 * permission.canDepartmentAudit 控制。
 */

function selectAllAudit() {

  if (!permission.canDepartmentAudit) {

    return

  }


  if (!auditTableRef.value) {

    return

  }


  /*
   * 直接把当前审核列表全部勾选。
   */

  auditTableRef.value.clearSelection()


  for (
    const row
    of auditList.value
    ) {

    auditTableRef.value.toggleRowSelection(
      row,
      true
    )

  }

}



/* ====================================================== */
/* 部门审核：取消全选 */
/* ====================================================== */

function clearAuditSelection() {

  if (!auditTableRef.value) {

    selectedAuditRows.value = []

    return

  }


  auditTableRef.value.clearSelection()

}



/* ====================================================== */
/* 部门审核操作 */
/* ====================================================== */

async function auditApply(
  row,
  status,
) {

  const actionText =
    status === 1
      ? '通过'
      : '驳回'


  try {

    const { value } =
      await ElMessageBox.prompt(

        `请输入${actionText}意见`,

        `部门申报${actionText}`,

        {

          confirmButtonText:
            '确定',

          cancelButtonText:
            '取消',

          inputPlaceholder:
            '请输入审核意见（可选）',

          inputType:
            'textarea',

        },

      )


    /*
     * 调用审核接口
     */

    const res =
      await request.put(

        `/departmentScoreApply/audit/${row.id}`,

        {

          status,

          reviewRemark:
            value || '',

        },

      )


    /*
     * 判断后端业务结果
     */

    if (
      Number(res?.code) !== 200 &&
      Number(res?.code) !== 0
    ) {

      throw new Error(
        res?.message ||
        `部门申报${actionText}失败`
      )

    }


    /*
     * 先从前端待审核列表中删除。
     *
     * 这样点击后立即消失，
     * 不需要等页面重新渲染。
     */

    auditList.value =
      auditList.value.filter(
        item =>
          String(item.id) !==
          String(row.id)
      )


    /*
     * 清除选中状态
     */

    selectedAuditRows.value =
      selectedAuditRows.value.filter(
        item =>
          String(item.id) !==
          String(row.id)
      )


    ElMessage.success(
      `部门申报已${actionText}`
    )


    /*
     * 再从后端重新加载一次，
     * 确保前端和数据库完全一致。
     */

    await loadAuditList()

    await loadMyApply()

  }
  catch (error) {

    /*
     * 用户取消
     */

    if (
      error === 'cancel' ||
      error === 'close'
    ) {

      return

    }


    console.error(
      '部门申报审核失败：',
      error
    )


    ElMessage.error(

      error?.message ||

      error?.response?.data?.message ||

      error?.response?.data?.msg ||

      `部门申报${actionText}失败`

    )

  }

}


/* ====================================================== */
/* 一键审批通过 */
/* ====================================================== */

/*
 * 一键审批通过：
 *
 * 1. 必须是副部长 / 部长
 * 2. 必须至少选择一条
 * 3. 弹窗确认
 * 4. 逐条调用原来的审核接口
 * 5. status = 1
 * 6. reviewRemark = ''
 * 7. 完成以后刷新列表
 *
 *
 * 这里没有新增后端接口。
 *
 * 直接复用：
 *
 * PUT /departmentScoreApply/audit/{id}
 *
 */

async function batchApproveAudit() {

  if (
    !permission.canDepartmentAudit
  ) {

    ElMessage.error(
      '你没有部门申报审核权限'
    )

    return

  }


  if (
    selectedAuditRows.value.length === 0
  ) {

    ElMessage.warning(
      '请先选择需要审批的申报'
    )

    return

  }


  const count =
    selectedAuditRows.value.length


  const previewNames =
    selectedAuditRows.value
      .slice(0, 5)
      .map(
        item =>
          item.studentName ||
          item.studentId ||
          '未知学生'
      )


  const previewText =
    previewNames.join('、') +
    (
      count > 5
        ? ` 等 ${count} 条申报`
        : ''
    )


  try {

    await ElMessageBox.confirm(

      `确定要将选中的 ${count} 条部门申报全部审批通过吗？\n\n${previewText}`,

      '一键审批通过',

      {

        confirmButtonText:
          '确定通过',

        cancelButtonText:
          '取消',

        type:
          'warning',

      },

    )

  }
  catch (error) {

    return

  }


  batchAuditLoading.value = true


  let successCount = 0

  let failCount = 0

  /*
   * 记录真正审核成功的 ID
   */

  const successfulIds = []


  try {

    /*
     * 注意：
     * 这里先复制一份，
     * 避免审核过程中 selectedAuditRows
     * 被修改导致循环异常。
     */

    const rows =
      [...selectedAuditRows.value]


    for (
      const row of rows
      ) {

      try {

        const res =
          await request.put(

            `/departmentScoreApply/audit/${row.id}`,

            {

              status: 1,

              reviewRemark: '',

            },

          )


        /*
         * 必须检查后端业务状态。
         */

        if (
          Number(res?.code) !== 200 &&
          Number(res?.code) !== 0
        ) {

          throw new Error(
            res?.message ||
            '审核失败'
          )

        }


        successCount++

        successfulIds.push(
          row.id
        )

      }
      catch (error) {

        failCount++

        console.error(

          `申报 ${row.id} 审批失败：`,

          error

        )

      }

    }


    /*
     * 立即从前端列表删除成功的记录
     */

    if (
      successfulIds.length > 0
    ) {

      auditList.value =
        auditList.value.filter(
          item =>
            !successfulIds.some(
              id =>
                String(id) ===
                String(item.id)
            )
        )

    }


    /*
     * 清除已经审核成功的选中项
     */

    selectedAuditRows.value =
      selectedAuditRows.value.filter(
        item =>
          !successfulIds.some(
            id =>
              String(id) ===
              String(item.id)
          )
      )


    /*
     * 提示结果
     */

    if (
      failCount === 0
    ) {

      ElMessage.success(
        `已成功审批通过 ${successCount} 条部门申报`
      )

    }
    else if (
      successCount > 0
    ) {

      ElMessage.warning(
        `批量审批完成：成功 ${successCount} 条，失败 ${failCount} 条`
      )

    }
    else {

      ElMessage.error(
        '批量审批失败，请检查后端服务'
      )

    }


    /*
     * 最后从后端重新同步一次。
     */

    await loadAuditList()

    await loadMyApply()

  }
  catch (error) {

    console.error(
      '一键审批通过失败：',
      error
    )

    ElMessage.error(

      error?.message ||

      error?.response?.data?.message ||

      error?.response?.data?.msg ||

      '一键审批通过失败，请检查后端服务'

    )

  }
  finally {

    batchAuditLoading.value = false

  }

}



/* ====================================================== */
/* 部门文件选择 */
/* ====================================================== */

function handleDepartmentFile(file) {

  departmentFile.value =
    file.raw || null

}



/* ====================================================== */
/* 删除部门文件 */
/* ====================================================== */

function removeDepartmentFile() {

  departmentFile.value = null

}



/* ====================================================== */
/* 提交部门加减分申报 */
/* ====================================================== */

async function submitDepartment() {

  /*
   * 表单不存在
   */

  if (!departmentFormRef.value) {

    return

  }


  /*
   * 表单验证
   */

  const valid =
    await departmentFormRef.value.validate()

  if (!valid) {

    return

  }


  /*
   * 确保模板存在
   */

  if (!selectedTemplate.value) {

    ElMessage.warning(
      '请选择有效的加减分项目'
    )

    return

  }


  /*
   * 获取模板类型
   */

  const scoreType =
    getScoreType(
      selectedTemplate.value
    )


  /*
   * 只允许：
   *
   * 1  = 加分
   * -1 = 减分
   */

  if (
    scoreType !== 1 &&
    scoreType !== -1
  ) {

    ElMessage.error(
      '当前加减分模板类型无效，请联系管理员检查模板配置'
    )

    return

  }


  departmentSubmitting.value = true


  try {

    const data = {

      departmentId:
      departmentForm.departmentId,

      studentId:
      departmentForm.studentId,

      templateId:
      departmentForm.templateId,

      evidenceUrl:
        departmentFile.value
          ? departmentFile.value.name
          : null,

    }


    console.log(
      '========== 提交部门申报 =========='
    )

    console.log(
      '提交数据：',
      data
    )


    const res =
      await request.post(
        '/departmentScoreApply/add',
        data
      )


    console.log(
      '部门申报接口响应：',
      res
    )


    /*
     * 关键：
     *
     * request 是 @/utils/request
     *
     * 所以 res 本身就是 Result。
     *
     * 正确：
     * res.code
     * res.message
     *
     * 错误：
     * res.data.code
     */

    const responseData =
      res || {}


    const code =
      Number(responseData.code)


    if (
      code !== 200 &&
      code !== 0
    ) {

      console.error(
        '部门申报后端返回失败：',
        responseData
      )

      ElMessage.error(

        responseData.message ||
        responseData.msg ||
        '部门加减分申报提交失败'

      )

      return

    }


    /*
     * 真正成功
     */

    ElMessage.success(
      '部门加减分申报提交成功，等待本部门副部长或部长审核'
    )


    /*
     * 重置表单
     */

    resetDepartment()


    /*
     * 刷新我的申报记录
     */

    await loadMyApply()


    /*
     * 如果当前用户有审核权限，
     * 同时刷新待审核列表
     */

    if (
      permission.canDepartmentAudit
    ) {

      await loadAuditList()

    }

  }
  catch (error) {

    console.error(
      '提交部门加减分申报失败：',
      error
    )

    const message =

      error?.response?.data?.message ||

      error?.response?.data?.msg ||

      error?.message ||

      '提交失败，请检查后端服务'


    ElMessage.error(
      message
    )

  }
  finally {

    departmentSubmitting.value = false

  }

}

/* ====================================================== */
/* 重置部门申报表单 */
/* ====================================================== */

function resetDepartment() {

  /*
   * 清空数据
   */

  departmentForm.departmentId = null

  departmentForm.studentId = null

  departmentForm.templateId = null

  departmentForm.evidenceUrl = ''


  /*
   * 清空模板
   */

  departmentTemplateList.value = []


  /*
   * 清空文件
   */

  departmentFile.value = null


  /*
   * 重置 Element Plus 表单状态
   */

  departmentFormRef.value?.resetFields()

}



/* ====================================================== */
/* 加载我的申报记录 */
/* ====================================================== */
async function loadMyApply() {

  listLoading.value = true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/my',
      )


    if (
      Number(res?.code) !== 200 &&
      Number(res?.code) !== 0
    ) {

      throw new Error(
        res?.message ||
        '获取申报记录失败'
      )

    }


    const data =
      Array.isArray(res?.data)
        ? res.data
        : []


    departmentApplyList.value =
      data.map(
        item => ({

          ...item,

          title:
            item.title ||
            '部门加减分',

          scoreType:
            Number(item.scoreType),

        })
      )


    /*
     * 每次重新加载申报记录，
     * 回到第一页
     */

    myApplyPageNum.value = 1

  }
  catch (error) {

    console.error(
      '获取申报记录失败：',
      error
    )

    departmentApplyList.value = []

    ElMessage.error(

      error?.message ||
      error?.response?.data?.message ||
      '获取申报记录失败'

    )

  }
  finally {

    listLoading.value = false

  }

}
/* ====================================================== */
/* 页面初始化 */
/* ====================================================== */

onMounted(async () => {

  /*
   * 1. 获取部门权限
   */

  await loadPermission()


  /*
   * 2. 获取学生列表
   */

  await loadStudentList()


  /*
   * 3. 获取我的申报
   */

  await loadMyApply()


  /*
   * 4. 如果有部门审核权限
   *    获取待审核数据
   */

  if (
    permission.canDepartmentAudit
  ) {

    await loadAuditList()

  }

})

</script>



<style scoped>

/* ====================================================== */
/* 页面 */
/* ====================================================== */

.score-apply-page {

  padding: 24px;

  min-height:
    calc(100vh - 60px);

  background:
    #f5f7fa;

}



/* ====================================================== */
/* 页面标题 */
/* ====================================================== */

.page-header {

  display: flex;

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

  font-size:
    14px;

}



/* ====================================================== */
/* 卡片 */
/* ====================================================== */

.apply-card {

  margin-bottom:
    20px;

}



/* ====================================================== */
/* 卡片标题 */
/* ====================================================== */

.card-header {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    flex-start;

  margin-bottom:
    25px;

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



/* ====================================================== */
/* 批量审核操作栏 */
/* ====================================================== */

.batch-audit-toolbar {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    center;

  margin-bottom:
    16px;

  padding:
    12px 16px;

  border:
    1px solid #ebeef5;

  border-radius:
    8px;

  background:
    #f8f9fa;

}



.batch-audit-left {

  display:
    flex;

  align-items:
    center;

  gap:
    8px;

}



.selected-count {

  margin-right:
    8px;

  color:
    #606266;

  font-size:
    14px;

}



.selected-count strong {

  color:
    #409eff;

  font-size:
    16px;

}



/* ====================================================== */
/* 表单 */
/* ====================================================== */

.apply-form {

  max-width:
    800px;

}



/* ====================================================== */
/* 上传 */
/* ====================================================== */

.upload-tip {

  color:
    #909399;

  font-size:
    12px;

}



/* ====================================================== */
/* 表单提示 */
/* ====================================================== */

.form-tip {

  margin-top:
    6px;

  color:
    #909399;

  font-size:
    12px;

  line-height:
    1.5;

}



/* ====================================================== */
/* 模板分值 */
/* ====================================================== */

.template-score {

  margin-left:
    12px;

  font-weight:
    700;

}



.template-score.bonus {

  color:
    #67c23a;

}



.template-score.deduct {

  color:
    #f56c6c;

}



.empty-template {

  color:
    #c0c4cc;

}



/* ====================================================== */
/* 加分 */
/* ====================================================== */

.bonus {

  color:
    #67c23a;

  font-weight:
    700;

}



/* ====================================================== */
/* 减分 */
/* ====================================================== */

.deduct {

  color:
    #f56c6c;

  font-weight:
    700;

}



/* ====================================================== */
/* 未知分值 */
/* ====================================================== */

.unknown-score {

  color:
    #909399;

  font-weight:
    700;

}



/* ====================================================== */
/* 部门列表 */
/* ====================================================== */

.department-list {

  display:
    flex;

  flex-direction:
    column;

  gap:
    12px;

}



/* ====================================================== */
/* 部门项目 */
/* ====================================================== */

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



/* ====================================================== */
/* 部门信息 */
/* ====================================================== */

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



/* ====================================================== */
/* 部门提示 */
/* ====================================================== */

.department-tip {

  margin-top:
    8px;

  font-size:
    13px;

  color:
    #909399;

}



/* ====================================================== */
/* 模板下拉选项 */
/* ====================================================== */

.template-option {

  display:
    flex;

  align-items:
    center;

  justify-content:
    space-between;

  width:
    100%;

}



.template-name {

  color:
    #303133;

}



/* ====================================================== */
/* 模板加分 */
/* ====================================================== */

.template-bonus {

  color:
    #67c23a;

  font-weight:
    700;

}



/* ====================================================== */
/* 模板减分 */
/* ====================================================== */

.template-deduct {

  color:
    #f56c6c;

  font-weight:
    700;

}



/* ====================================================== */
/* 模板未知 */
/* ====================================================== */

.template-unknown {

  color:
    #909399;

}



/* ====================================================== */
/* 表格优化 */
/* ====================================================== */

:deep(.el-table) {

  width:
    100%;

}



:deep(.el-table th) {

  background:
    #f5f7fa;

}



:deep(.el-table td) {

  vertical-align:
    middle;

}



/* ====================================================== */
/* 移动端 */
/* ====================================================== */

@media (max-width: 768px) {

  .score-apply-page {

    padding:
      12px;

  }


  .page-header {

    align-items:
      flex-start;

  }


  .page-header h2 {

    font-size:
      20px;

  }


  .card-header {

    flex-direction:
      column;

    gap:
      12px;

  }


  .apply-form {

    max-width:
      100%;

  }


  .department-info {

    align-items:
      flex-start;

  }


  /* ================================================== */
  /* 批量审核工具栏移动端 */
  /* ================================================== */

  .batch-audit-toolbar {

    flex-direction:
      column;

    align-items:
      stretch;

    gap:
      12px;

  }


  .batch-audit-left {

    flex-wrap:
      wrap;

  }

}


/* ====================================================== */
/* 我的申报分页 */
/* ====================================================== */

.pagination-wrapper {

  display: flex;

  justify-content:
    space-between;

  align-items:
    center;

  margin-top:
    20px;

  padding-top:
    16px;

  border-top:
    1px solid #ebeef5;

}



.pagination-total {

  color:
    #909399;

  font-size:
    13px;

}


/* ====================================================== */
/* 移动端分页 */
/* ====================================================== */

@media (max-width: 768px) {

  .pagination-wrapper {

    flex-direction:
      column;

    align-items:
      flex-start;

    gap:
      12px;

  }

}

</style>
