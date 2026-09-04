<template>

  <div class="semester-page">

    <!-- =====================================================
         页面标题
    ====================================================== -->

    <div class="page-header">

      <div>

        <h2>
          学期管理
        </h2>

        <p>
          管理系统学期，并设置当前使用的学期
        </p>

      </div>


      <el-button
        type="primary"
        @click="openAddDialog"
      >

        <el-icon>
          <Plus />
        </el-icon>

        新增学期

      </el-button>

    </div>


    <!-- =====================================================
         当前学期提示
    ====================================================== -->

    <el-alert
      v-if="currentSemester"
      type="success"
      :closable="false"
      class="current-alert"
    >

      <template #title>

        当前学期：

        <strong>
          {{ currentSemester.name }}
        </strong>

        <span class="current-date">

          （
          {{ currentSemester.startDate }}
          至
          {{ currentSemester.endDate }}
          ）

        </span>

      </template>

    </el-alert>


    <!-- =====================================================
         学期列表
    ====================================================== -->

    <el-card
      class="table-card"
      shadow="never"
    >

      <el-table
        v-loading="loading"
        :data="semesters"
        stripe
        style="width: 100%"
      >

        <!-- 学期名称 -->

        <el-table-column
          prop="name"
          label="学期名称"
          min-width="220"
        />



        <!-- 开始日期 -->

        <el-table-column
          prop="startDate"
          label="开始日期"
          width="160"
        />



        <!-- 结束日期 -->

        <el-table-column
          prop="endDate"
          label="结束日期"
          width="160"
        />



        <!-- 状态 -->

        <el-table-column
          label="状态"
          width="120"
        >

          <template #default="scope">

            <el-tag
              v-if="Number(scope.row.status) === 1"
              type="success"
            >
              当前学期
            </el-tag>

            <el-tag
              v-else
              type="info"
            >
              非当前
            </el-tag>

          </template>

        </el-table-column>



        <!-- 操作 -->

        <el-table-column
          label="操作"
          width="240"
          fixed="right"
        >

          <template #default="scope">

            <el-button
              link
              type="primary"
              @click="openEditDialog(scope.row)"
            >
              编辑
            </el-button>


            <el-button
              v-if="Number(scope.row.status) !== 1"
              link
              type="success"
              @click="handleSetCurrent(scope.row)"
            >
              设为当前
            </el-button>


            <el-tag
              v-else
              type="success"
              effect="plain"
            >
              正在使用
            </el-tag>

          </template>

        </el-table-column>


        <!-- 空数据 -->

        <template #empty>

          <el-empty
            description="暂无学期，请先新增学期"
          />

        </template>

      </el-table>

    </el-card>


    <!-- =====================================================
         新增 / 编辑弹窗
    ====================================================== -->

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >

        <!-- 学期名称 -->

        <el-form-item
          label="学期名称"
          prop="name"
        >

          <el-input
            v-model="form.name"
            placeholder="例如：2026-2027第一学期"
            maxlength="50"
            show-word-limit
          />

        </el-form-item>



        <!-- 开始日期 -->

        <el-form-item
          label="开始日期"
          prop="startDate"
        >

          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开始日期"
            style="width: 100%"
          />

        </el-form-item>



        <!-- 结束日期 -->

        <el-form-item
          label="结束日期"
          prop="endDate"
        >

          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择结束日期"
            style="width: 100%"
          />

        </el-form-item>


      </el-form>


      <!-- 弹窗底部 -->

      <template #footer>

        <el-button
          @click="dialogVisible = false"
        >
          取消
        </el-button>


        <el-button
          type="primary"
          :loading="saving"
          @click="submitForm"
        >
          确定
        </el-button>

      </template>

    </el-dialog>

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
  Plus
} from '@element-plus/icons-vue'


import {
  getSemesterList,
  getCurrentSemester,
  addSemester,
  updateSemester,
  setCurrentSemester
} from '@/api/semester'


/*
 * =========================================================
 * 数据
 * =========================================================
 */

const semesters =
  ref([])


const currentSemester =
  ref(null)


const loading =
  ref(false)


const saving =
  ref(false)


/*
 * =========================================================
 * 弹窗
 * =========================================================
 */

const dialogVisible =
  ref(false)


const editId =
  ref(null)


const dialogTitle =
  computed(() => {

    return editId.value
      ? '编辑学期'
      : '新增学期'

  })


/*
 * =========================================================
 * 表单
 * =========================================================
 */

const formRef =
  ref(null)


const form =
  reactive({

    name: '',

    startDate: '',

    endDate: ''

  })


/*
 * =========================================================
 * 表单验证
 * =========================================================
 */

const rules = {

  name: [
    {
      required: true,
      message: '请输入学期名称',
      trigger: 'blur'
    }
  ],

  startDate: [
    {
      required: true,
      message: '请选择开始日期',
      trigger: 'change'
    }
  ],

  endDate: [
    {
      required: true,
      message: '请选择结束日期',
      trigger: 'change'
    }
  ]

}


/*
 * =========================================================
 * 统一解析 Result
 * =========================================================
 */

function getResultData(
  response,
  defaultValue
) {

  if (
    response
    && response.data
    && response.data.data !== undefined
  ) {

    return response.data.data

  }


  if (
    response
    && response.data !== undefined
  ) {

    return response.data

  }


  return defaultValue
}


/*
 * =========================================================
 * 加载学期
 * =========================================================
 */

async function loadSemesters() {

  loading.value = true


  try {

    const response =
      await getSemesterList()


    const data =
      getResultData(
        response,
        []
      )


    semesters.value =
      Array.isArray(data)
        ? data
        : []


  } catch (error) {

    console.error(
      '加载学期失败：',
      error
    )


    semesters.value =
      []


    ElMessage.error(
      '学期列表加载失败'
    )

  } finally {

    loading.value = false

  }

}


/*
 * =========================================================
 * 加载当前学期
 * =========================================================
 */

async function loadCurrentSemester() {

  try {

    const response =
      await getCurrentSemester()


    currentSemester.value =
      getResultData(
        response,
        null
      )


  } catch (error) {

    console.error(
      '加载当前学期失败：',
      error
    )


    currentSemester.value =
      null

  }

}


/*
 * =========================================================
 * 刷新全部数据
 * =========================================================
 */

async function loadData() {

  await Promise.all([
    loadSemesters(),
    loadCurrentSemester()
  ])

}


/*
 * =========================================================
 * 重置表单
 * =========================================================
 */

function resetForm() {

  editId.value =
    null


  form.name =
    ''

  form.startDate =
    ''

  form.endDate =
    ''


  if (formRef.value) {

    formRef.value.clearValidate()

  }

}


/*
 * =========================================================
 * 打开新增
 * =========================================================
 */

function openAddDialog() {

  resetForm()


  dialogVisible.value =
    true

}


/*
 * =========================================================
 * 打开编辑
 * =========================================================
 */

function openEditDialog(
  row
) {

  editId.value =
    row.id


  form.name =
    row.name || ''


  form.startDate =
    row.startDate || ''


  form.endDate =
    row.endDate || ''


  dialogVisible.value =
    true

}


/*
 * =========================================================
 * 提交表单
 * =========================================================
 */

async function submitForm() {

  if (!formRef.value) {
    return
  }


  try {

    await formRef.value.validate()

  } catch {

    return

  }


  if (
    form.startDate
    && form.endDate
    && form.endDate < form.startDate
  ) {

    ElMessage.error(
      '结束日期不能早于开始日期'
    )

    return

  }


  saving.value =
    true


  try {

    const data = {

      name:
        form.name.trim(),

      startDate:
      form.startDate,

      endDate:
      form.endDate

    }


    if (editId.value) {

      await updateSemester(
        editId.value,
        data
      )


      ElMessage.success(
        '学期修改成功'
      )

    } else {

      await addSemester(
        data
      )


      ElMessage.success(
        '学期创建成功'
      )

    }


    dialogVisible.value =
      false


    await loadData()


  } catch (error) {

    console.error(
      '保存学期失败：',
      error
    )


    const message =
      error?.response?.data?.message
      || error?.response?.data?.msg
      || '学期保存失败'


    ElMessage.error(
      message
    )

  } finally {

    saving.value =
      false

  }

}


/*
 * =========================================================
 * 设置当前学期
 * =========================================================
 */

async function handleSetCurrent(
  row
) {

  try {

    await ElMessageBox.confirm(

      `确定将「${row.name}」设置为当前学期吗？`,

      '设置当前学期',

      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }

    )

  } catch {

    return

  }


  try {

    await setCurrentSemester(
      row.id
    )


    ElMessage.success(
      '当前学期设置成功'
    )


    await loadData()


  } catch (error) {

    console.error(
      '设置当前学期失败：',
      error
    )


    const message =
      error?.response?.data?.message
      || error?.response?.data?.msg
      || '设置当前学期失败'


    ElMessage.error(
      message
    )

  }

}


/*
 * =========================================================
 * 初始化
 * =========================================================
 */

onMounted(() => {

  loadData()

})

</script>


<style scoped>

.semester-page {

  padding:
    24px;

}


/* =========================================================
   页面头部
========================================================= */

.page-header {

  display:
    flex;

  align-items:
    center;

  justify-content:
    space-between;

  margin-bottom:
    20px;

}


.page-header h2 {

  margin:
    0;

  font-size:
    24px;

  color:
    #303133;

}


.page-header p {

  margin:
    8px 0 0;

  color:
    #909399;

  font-size:
    14px;

}


/* =========================================================
   当前学期
========================================================= */

.current-alert {

  margin-bottom:
    20px;

}


.current-date {

  margin-left:
    6px;

  font-weight:
    normal;

}


/* =========================================================
   表格
========================================================= */

.table-card {

  border-radius:
    8px;

}


</style>
