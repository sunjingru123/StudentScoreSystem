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
        @submit.prevent
      >

        <!-- 学期 -->

        <el-form-item
          label="学期"
          required
        >

          <el-select
            v-model="form.semesterId"
            :loading="semesterLoading"
            :disabled="semesterLoading"
            :placeholder="
              semesterLoading
                ? '学期加载中…'
                : '请选择学期'
            "
            :no-data-text="
              semesterLoading
                ? '加载中…'
                : '暂无学期数据'
            "
            style="width: 100%"
          >

            <el-option
              v-for="item in semesters"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />

          </el-select>

          <!-- 学期加载失败：给出明确原因与重试入口，避免下拉显示「无数据」 -->

          <div
            v-if="semesterError"
            class="semester-error"
          >

            <span>{{ semesterError }}</span>

            <el-button
              link
              type="primary"
              :loading="semesterLoading"
              @click="loadSemesters"
            >
              重新加载
            </el-button>

          </div>

        </el-form-item>


        <!-- 班级 -->

        <el-form-item
          label="班级"
          required
        >

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
          每条加减分明细使用“；”分隔，并标注所属周次
        </div>

        <div>
          <strong>周次：</strong>
          第 X 周按所选学期的开始日期推算，学期一更新，导出结果随之更新
        </div>

        <div>
          <strong>注意：</strong>
          加减分具体情况会自动换行，行高由 Excel 自动调整
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

/*
 * 学期列表的加载态与错误态：
 *
 * 未就绪时不能把下拉渲染成「无数据」，
 * 否则用户会以为系统没有学期。
 */

const semesterLoading = ref(false)

const semesterError = ref('')


const form = reactive({

  semesterId: null,

  className: ''

})


// =========================================================
// 获取学期
// =========================================================

const loadSemesters = async () => {

  if (semesterLoading.value) {

    return

  }

  semesterLoading.value = true

  semesterError.value = ''

  try {


    const res =
      await request.get(
        '/scoreExport/semesters'
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




    if (
      result?.code === 200
      || result?.code === 0
    ) {

      semesters.value =
        result.data || []


      /*
       * 默认选中当前学期（status === 1），
       * 没有当前学期时取列表中第一个
       * （后端已按开始日期倒序返回）。
       */

      if (
        !form.semesterId
        && semesters.value.length > 0
      ) {

        const current =
          semesters.value.find(
            item => Number(item.status) === 1
          )


        form.semesterId =
          current
            ? current.id
            : semesters.value[0].id

      }


      if (
        semesters.value.length === 0
      ) {

        semesterError.value =
          '系统暂时没有学期数据，请先在「学期管理」中创建学期'

      }

    } else {

      semesterError.value =
        result?.message
        || result?.msg
        || '获取学期失败'

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

    semesterError.value =
      error?.response?.data?.message
      || error?.response?.data?.msg
      || error?.message
      || '获取学期失败'

  } finally {

    semesterLoading.value = false

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


.semester-error {

  display: flex;

  align-items: center;

  gap: 4px;

  margin-top: 6px;

  font-size: 13px;

  line-height: 1.5;

  color: #f56c6c;

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
