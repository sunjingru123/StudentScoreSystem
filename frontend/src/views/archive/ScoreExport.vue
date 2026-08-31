<template>
  <div class="score-export-page">

    <div class="page-header">

      <div>
        <h2>加减分汇总导出</h2>

        <p>
          导出指定学期、指定班级的学生加减分汇总
        </p>
      </div>

    </div>


    <el-card class="export-card">

      <el-form
        :model="form"
        label-width="90px"
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


        <!-- 班级 -->

        <el-form-item label="班级">

          <el-input
            v-model="form.className"
            placeholder="例如：24级3班"
            clearable
          />

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
          姓名、学号、加分、减分、加减分具体情况
        </div>

        <div>
          <strong>详情格式：</strong>
          每条加减分明细使用“；”分隔
        </div>

        <div>
          <strong>注意：</strong>
          加减分具体情况不会自动换行
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


const form = reactive({

  semesterId: null,

  className: ''

})


// =========================================================
// 获取学期
// =========================================================

const loadSemesters = async () => {

  try {

    console.log('========== 开始获取学期 ==========')

    const res =
      await request.get(
        '/scoreExport/semesters'
      )

    console.log(
      '获取学期原始返回：',
      res
    )


    /*
     * 兼容两种情况：
     *
     * 情况1：
     *
     * res.data = {
     *   code: 200,
     *   data: [...]
     * }
     *
     * 情况2：
     *
     * res = {
     *   code: 200,
     *   data: [...]
     * }
     */

    const result =
      res?.data?.code !== undefined
        ? res.data
        : res


    console.log(
      '解析后的返回：',
      result
    )


    if (
      result?.code === 200
      || result?.code === 0
    ) {

      semesters.value =
        result.data || []

      console.log(
        '学期列表：',
        semesters.value
      )


      if (
        semesters.value.length === 0
      ) {

        ElMessage.warning(
          '系统暂时没有学期数据'
        )

      }

    } else {

      ElMessage.error(
        result?.message
        || '获取学期失败'
      )

      console.error(
        '获取学期失败，后端返回：',
        result
      )

    }

  } catch (error) {

    console.error(
      '获取学期请求异常：',
      error
    )


    /*
     * 尽可能显示后端真正返回的错误
     */

    const message =
      error?.response?.data?.message
      || error?.response?.data?.msg
      || error?.message
      || '获取学期失败'


    ElMessage.error(
      message
    )

  }

}


// =========================================================
// 导出 Excel
// =========================================================

const handleExport = async () => {

  if (!form.semesterId) {

    ElMessage.warning(
      '请选择学期'
    )

    return

  }


  if (
    !form.className
    || !form.className.trim()
  ) {

    ElMessage.warning(
      '请输入班级'
    )

    return

  }


  loading.value = true


  try {

    console.log(
      '开始导出：',
      {
        semesterId: form.semesterId,
        className: form.className.trim()
      }
    )


    const response =
      await request.get(
        '/scoreExport/department',
        {

          params: {

            semesterId:
            form.semesterId,

            className:
              form.className.trim()

          },

          responseType:
            'blob'

        }
      )


    /*
     * 创建 Blob
     */

    const blob =
      new Blob(
        [response.data || response],
        {
          type:
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }
      )


    const url =
      window.URL.createObjectURL(
        blob
      )


    const link =
      document.createElement(
        'a'
      )


    link.href = url


    link.download =
      `${form.className.trim()}加减分情况汇总.xlsx`


    document.body.appendChild(
      link
    )


    link.click()


    document.body.removeChild(
      link
    )


    window.URL.revokeObjectURL(
      url
    )


    ElMessage.success(
      'Excel 导出成功'
    )


  } catch (error) {

    console.error(
      'Excel 导出失败：',
      error
    )


    ElMessage.error(
      'Excel 导出失败'
    )

  } finally {

    loading.value = false

  }

}


// =========================================================
// 页面加载
// =========================================================

onMounted(() => {

  loadSemesters()

})
</script>


<style scoped>

.score-export-page {

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
