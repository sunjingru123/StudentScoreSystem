<template>
  <div class="senior-export-page">

    <div class="page-header">

      <div>
        <h2>加减分汇总导出</h2>

        <p>
          高级辅导员可按全年级、各班导出加减分汇总
        </p>
      </div>

    </div>


    <el-card class="export-card">

      <el-form
        :model="form"
        label-width="90px"
        @submit.prevent
      >

        <!-- 学期 -->

        <el-form-item label="学期">

          <el-select
            v-model="form.semesterId"
            placeholder="请选择学期"
            style="width: 100%"
          >

            <el-option
              v-for="item in semesters"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />

          </el-select>

        </el-form-item>


        <!-- 导出范围 -->

        <el-form-item label="导出范围">

          <el-radio-group
            v-model="form.scope"
          >

            <el-radio-button value="GRADE">
              全年级
            </el-radio-button>

            <el-radio-button value="CLASS">
              各班
            </el-radio-button>

          </el-radio-group>

        </el-form-item>


        <!-- 年级 -->

        <el-form-item
          v-if="form.scope === 'GRADE'"
          label="年级"
        >

          <el-select
            v-model="form.grade"
            placeholder="请选择年级"
            style="width: 100%"
          >

            <el-option
              v-for="item in options.grades"
              :key="item"
              :label="item"
              :value="item"
            />

          </el-select>

        </el-form-item>


        <!-- 班级 -->

        <el-form-item
          v-if="form.scope === 'CLASS'"
          label="班级"
        >

          <el-select
            v-model="form.className"
            placeholder="请选择班级"
            filterable
            style="width: 100%"
          >

            <el-option
              v-for="item in options.classes"
              :key="item"
              :label="item"
              :value="item"
            />

          </el-select>

        </el-form-item>


        <!-- 导出 -->

        <el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            @click="handleExport"
          >
            导出 Excel
          </el-button>

        </el-form-item>

      </el-form>


      <div class="tips">

        <div>
          <strong>导出格式：</strong>
          姓名、学号、加分、减分、加减分具体情况，与档案部导出一致
        </div>

        <div>
          <strong>全年级：</strong>
          例如选择“24级”，导出该年级全部学生混合的汇总表
        </div>

        <div>
          <strong>周次：</strong>
          第 X 周按所选学期的开始日期推算，学期一更新，导出结果随之更新
        </div>

      </div>

    </el-card>

  </div>
</template>


<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'


// =========================================================
// 数据
// =========================================================

const semesters = ref([])

const loading = ref(false)

const options = reactive({

  grades: [],

  classes: []

})


const form = reactive({

  semesterId: null,

  scope: 'GRADE',

  grade: '',

  className: ''

})


// =========================================================
// 读取后端返回体（兼容解包差异）
// =========================================================

function unwrap(res) {

  return res?.data?.code !== undefined
    ? res.data
    : res

}


// =========================================================
// 学期
// =========================================================

const loadSemesters = async () => {

  try {

    const result = unwrap(
      await request.get('/scoreExport/semesters')
    )

    if (result?.code === 200 || result?.code === 0) {

      semesters.value = result.data || []

    } else {

      ElMessage.error(result?.message || '获取学期失败')

    }

  } catch (error) {

    console.error('获取学期失败：', error)

    ElMessage.error('获取学期失败')

  }

}


// =========================================================
// 导出选项（年级 / 班级）
// =========================================================

const loadOptions = async () => {

  try {

    const result = unwrap(
      await request.get('/scoreExport/senior/options')
    )

    if (result?.code === 200 || result?.code === 0) {

      const data = result.data || {}

      options.grades = Array.isArray(data.grades) ? data.grades : []

      options.classes = Array.isArray(data.classes) ? data.classes : []

    } else {

      ElMessage.error(result?.message || '获取导出选项失败')

    }

  } catch (error) {

    console.error('获取导出选项失败：', error)

    ElMessage.error('获取导出选项失败')

  }

}


// =========================================================
// 从 blob 错误响应中提取后端消息
// =========================================================

async function extractBlobError(error) {

  try {

    const data = error?.response?.data

    if (data instanceof Blob) {

      const text = await data.text()

      const parsed = JSON.parse(text)

      return parsed?.message || ''

    }

  } catch (ignored) {

    // 忽略解析失败

  }

  return ''

}


// =========================================================
// 导出
// =========================================================

const handleExport = async () => {

  if (!form.semesterId) {

    ElMessage.warning('请选择学期')

    return

  }

  if (form.scope === 'GRADE' && !form.grade) {

    ElMessage.warning('请选择年级')

    return

  }

  if (form.scope === 'CLASS' && !form.className) {

    ElMessage.warning('请选择班级')

    return

  }

  loading.value = true

  try {

    const response = await request.get('/scoreExport/senior', {

      params: {

        semesterId: form.semesterId,

        scope: form.scope,

        grade: form.scope === 'GRADE' ? form.grade : undefined,

        className: form.scope === 'CLASS' ? form.className : undefined

      },

      responseType: 'blob'

    })

    const blob = new Blob([response.data || response], {

      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

    })

    const url = window.URL.createObjectURL(blob)

    const link = document.createElement('a')

    link.href = url

    link.download = `${exportLabel()}加减分情况汇总.xlsx`

    document.body.appendChild(link)

    link.click()

    document.body.removeChild(link)

    window.URL.revokeObjectURL(url)

    ElMessage.success('Excel 导出成功')

  } catch (error) {

    console.error('Excel 导出失败：', error)

    const message = await extractBlobError(error)

    ElMessage.error(message || 'Excel 导出失败')

  } finally {

    loading.value = false

  }

}


function exportLabel() {

  if (form.scope === 'GRADE') {

    return form.grade

  }

  return form.className

}


// =========================================================
// 页面加载
// =========================================================

onMounted(() => {

  loadSemesters()

  loadOptions()

})
</script>


<style scoped>

.senior-export-page {

  padding: 24px;

  background: #f5f7fa;

  min-height: calc(100vh - 120px);

}


.page-header {

  margin-bottom: 20px;

}


.page-header h2 {

  margin: 0 0 8px;

  font-size: 26px;

  color: #303133;

}


.page-header p {

  margin: 0;

  color: #909399;

  font-size: 14px;

}


.export-card {

  max-width: 700px;

  border-radius: 12px;

}


.tips {

  margin-top: 25px;

  padding: 16px;

  background: #f4f8ff;

  border-radius: 8px;

  color: #606266;

  line-height: 2;

}


.tips strong {

  color: #303133;

}

</style>
