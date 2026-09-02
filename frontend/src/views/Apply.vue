<template>
  <div class="apply-page">

    <el-card class="apply-card">

      <template #header>

        <div class="card-title">
          个人证书申报
        </div>

      </template>


      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >

        <!-- 获奖类别 -->

        <el-form-item
          label="获奖类别"
          prop="awardCategory"
        >

          <el-select
            v-model="form.awardCategory"
            placeholder="请选择获奖类别"
            style="width: 300px"
          >

            <el-option
              label="A类"
              value="A"
            />

            <el-option
              label="B类"
              value="B"
            />

            <el-option
              label="C类"
              value="C"
            />

            <el-option
              label="其他"
              value="OTHER"
            />

          </el-select>

        </el-form-item>


        <!-- 获奖名称 -->

        <el-form-item
          label="获奖名称"
          prop="awardName"
        >

          <el-input
            v-model="form.awardName"
            placeholder="请输入获奖名称"
            maxlength="100"
            show-word-limit
            style="width: 500px"
          />

        </el-form-item>


        <!-- 获奖级别 -->

        <el-form-item
          label="获奖级别"
          prop="awardLevel"
        >

          <el-select
            v-model="form.awardLevel"
            placeholder="请选择获奖级别"
            style="width: 300px"
          >

            <el-option
              label="国家级"
              value="国家级"
            />

            <el-option
              label="省级"
              value="省级"
            />

            <el-option
              label="校级"
              value="校级"
            />

            <el-option
              label="院级"
              value="院级"
            />

          </el-select>

        </el-form-item>


        <!-- 获奖等级 -->

        <el-form-item
          label="获奖等级"
          prop="awardGrade"
        >

          <el-select
            v-model="form.awardGrade"
            placeholder="请选择获奖等级"
            style="width: 300px"
          >

            <el-option
              label="一等奖"
              value="一等奖"
            />

            <el-option
              label="二等奖"
              value="二等奖"
            />

            <el-option
              label="三等奖"
              value="三等奖"
            />

            <el-option
              label="优秀奖"
              value="优秀奖"
            />

            <el-option
              label="其他"
              value="OTHER"
            />

          </el-select>

        </el-form-item>


        <!-- 其他等级 -->

        <el-form-item
          v-if="form.awardGrade === 'OTHER'"
          label="其他等级"
          prop="awardGradeOther"
        >

          <el-input
            v-model="form.awardGradeOther"
            placeholder="请输入其他获奖等级"
            maxlength="50"
            style="width: 300px"
          />

        </el-form-item>


        <!-- 获奖时间 -->

        <el-form-item
          label="获奖时间"
          prop="awardTime"
        >

          <el-date-picker
            v-model="form.awardTime"
            type="date"
            placeholder="请选择获奖时间"
            value-format="YYYY-MM-DD"
            style="width: 300px"
          />

        </el-form-item>


        <!-- 奖项类型 -->

        <el-form-item
          label="奖项类型"
          prop="awardType"
        >

          <el-radio-group
            v-model="form.awardType"
          >

            <el-radio value="个人奖">
              个人奖
            </el-radio>

            <el-radio value="团体奖">
              团体奖
            </el-radio>

          </el-radio-group>

        </el-form-item>


        <!-- 是否有凭证 -->

        <el-form-item
          label="获奖凭证"
          prop="hasCertificate"
        >

          <el-radio-group
            v-model="form.hasCertificate"
          >

            <el-radio value="YES">
              有
            </el-radio>

            <el-radio value="NO">
              无
            </el-radio>

          </el-radio-group>

        </el-form-item>


        <!-- 上传材料 -->

        <el-form-item
          v-if="form.hasCertificate === 'YES'"
          label="证书材料"
          prop="materialFile"
        >

          <el-upload
          class="certificate-upload"
          action="/api/file/upload"
          name="file"
          :headers="uploadHeaders"
          :limit="1"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :on-remove="handleRemove"
          :before-upload="beforeUpload"
        >

            <el-button
              type="primary"
            >
              上传获奖凭证
            </el-button>


            <template #tip>

              <div class="el-upload__tip">

                请上传获奖证书或相关证明材料

                <br />

                支持 PDF、JPG、JPEG、PNG，文件大小不超过 10MB

              </div>

            </template>

          </el-upload>


          <!-- 已上传文件 -->

          <div
            v-if="form.materialFile"
            class="uploaded-file"
          >

            <el-tag type="success">
              文件已上传
            </el-tag>

            <el-button
              link
              type="primary"
              @click="previewFile"
            >
              查看材料
            </el-button>

          </div>

        </el-form-item>


        <!-- 无凭证原因 -->

        <el-form-item
          v-if="form.hasCertificate === 'NO'"
          label="无凭证原因"
          prop="certificateReason"
        >

          <el-input
            v-model="form.certificateReason"
            type="textarea"
            :rows="4"
            placeholder="请说明没有获奖凭证的原因"
            maxlength="300"
            show-word-limit
            style="width: 600px"
          />

        </el-form-item>


        <!-- 申请说明 -->

        <el-form-item
          label="申请说明"
          prop="description"
        >

          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="可补充说明获奖情况"
            maxlength="500"
            show-word-limit
            style="width: 600px"
          />

        </el-form-item>


        <!-- 申请分值 -->

        <el-form-item label="申请分值">

          <el-tag type="info">

            学生无需填写，
            由档案部审核后确定最终加分

          </el-tag>

        </el-form-item>


        <!-- 提交 -->

        <el-form-item>

          <el-button
            type="primary"
            :loading="submitting"
            @click="submitApply"
          >

            提交申报

          </el-button>


          <el-button
            @click="resetForm"
          >

            重置

          </el-button>

        </el-form-item>

      </el-form>

    </el-card>

  </div>
</template>


<script setup>

import {
  reactive,
  ref,
  watch
} from 'vue'

import {
  ElMessage
} from 'element-plus'

import {
  addApply
} from '@/api/apply'


/*
 * =========================================================
 * 表单
 * =========================================================
 */

const formRef =
  ref()


const submitting =
  ref(false)


const form =
  reactive({

    awardCategory: '',

    awardName: '',

    awardLevel: '',

    awardGrade: '',

    awardGradeOther: '',

    awardTime: '',

    awardType: '',

    hasCertificate: '',

    materialFile: '',

    certificateReason: '',

    description: '',

  })


/*
 * =========================================================
 * 表单校验
 * =========================================================
 */

const rules =
  {

    awardCategory: [

      {
        required: true,
        message: '请选择获奖类别',
        trigger: 'change',
      },

    ],


    awardName: [

      {
        required: true,
        message: '请输入获奖名称',
        trigger: 'blur',
      },

    ],


    awardLevel: [

      {
        required: true,
        message: '请选择获奖级别',
        trigger: 'change',
      },

    ],


    awardGrade: [

      {
        required: true,
        message: '请选择获奖等级',
        trigger: 'change',
      },

    ],


    awardGradeOther: [

      {

        validator:
          (rule, value, callback) => {

            if (
              form.awardGrade === 'OTHER' &&
              (
                !value ||
                !value.trim()
              )
            ) {

              callback(
                new Error(
                  '请输入其他获奖等级'
                )
              )

              return

            }

            callback()

          },

        trigger: 'blur',

      },

    ],


    awardTime: [

      {
        required: true,
        message: '请选择获奖时间',
        trigger: 'change',
      },

    ],


    awardType: [

      {
        required: true,
        message: '请选择奖项类型',
        trigger: 'change',
      },

    ],


    hasCertificate: [

      {
        required: true,
        message: '请选择是否有获奖凭证',
        trigger: 'change',
      },

    ],


    materialFile: [

      {

        validator:
          (rule, value, callback) => {

            if (
              form.hasCertificate === 'YES' &&
              !value
            ) {

              callback(
                new Error(
                  '请上传获奖凭证'
                )
              )

              return

            }

            callback()

          },

        trigger: 'change',

      },

    ],


    certificateReason: [

      {

        validator:
          (rule, value, callback) => {

            if (
              form.hasCertificate === 'NO' &&
              (
                !value ||
                !value.trim()
              )
            ) {

              callback(
                new Error(
                  '请填写没有获奖凭证的原因'
                )
              )

              return

            }

            callback()

          },

        trigger: 'blur',

      },

    ],

  }


/*
 * =========================================================
 * 获奖等级变化
 * =========================================================
 */

watch(
  () => form.awardGrade,
  (value) => {

    if (
      value !== 'OTHER'
    ) {

      form.awardGradeOther = ''

    }

  }
)


/*
 * =========================================================
 * 是否有凭证变化
 * =========================================================
 */

watch(
  () => form.hasCertificate,
  (value) => {

    if (
      value === 'YES'
    ) {

      form.certificateReason = ''

    }


    if (
      value === 'NO'
    ) {

      form.materialFile = ''

    }

  }
)


/*
 * =========================================================
 * 上传请求头
 * =========================================================
 *
 * 注意：
 *
 * el-upload 不经过 axios，
 * 所以必须自己设置 Authorization。
 *
 * =========================================================
 */

const uploadHeaders = {
  Authorization:
    localStorage.getItem('token')
      ? 'Bearer ' +
      localStorage.getItem('token')
      : ''
}


/*
 * =========================================================
 * 上传前检查
 * =========================================================
 */

function beforeUpload(
  file
) {

  console.log(
    '准备上传文件：',
    file
  )


  /*
   * 文件类型
   */

  const allowedTypes =
    [

      'application/pdf',

      'image/jpeg',

      'image/png',

    ]


  const fileName =
    file.name
      ? file.name.toLowerCase()
      : ''


  const extensionAllowed =
    fileName.endsWith('.pdf') ||
    fileName.endsWith('.jpg') ||
    fileName.endsWith('.jpeg') ||
    fileName.endsWith('.png')


  const typeAllowed =
    allowedTypes.includes(
      file.type
    )


  if (
    !typeAllowed &&
    !extensionAllowed
  ) {

    ElMessage.error(
      '只能上传 PDF、JPG、JPEG、PNG 文件'
    )

    return false

  }


  /*
   * 文件大小
   */

  const maxSize =
    10 * 1024 * 1024


  if (
    file.size >
    maxSize
  ) {

    ElMessage.error(
      '文件大小不能超过10MB'
    )

    return false

  }


  return true

}


/*
 * =========================================================
 * 上传成功
 * =========================================================
 */

function handleUploadSuccess(
  response,
  uploadFile
) {

  console.log(
    '================================='
  )

  console.log(
    '服务器上传成功返回：',
    response
  )

  console.log(
    '上传文件对象：',
    uploadFile
  )

  console.log(
    '================================='
  )


  /*
   * =====================================================
   * 统一解析后端 Result
   * =====================================================
   */

  let fileData =
    null


  /*
   * 正常情况：
   *
   * {
   *   code: 200,
   *   message: "success",
   *   data: {
   *      id: 1,
   *      url: "/api/file/view/1"
   *   }
   * }
   */

  if (
    response &&
    response.data
  ) {

    fileData =
      response.data

  }


  /*
   * 如果后端直接返回 data
   */

  else if (
    response &&
    response.url
  ) {

    fileData =
      response

  }


  /*
   * =====================================================
   * 没找到
   * =====================================================
   */

  if (
    !fileData ||
    !fileData.url
  ) {

    console.error(
      '服务器确实返回了数据，但是没有找到文件地址：',
      response
    )

    ElMessage.error(
      '文件上传成功，但服务器没有返回文件地址'
    )

    return

  }


  /*
   * =====================================================
   * 保存文件地址
   * =====================================================
   */

  form.materialFile =
    fileData.url


  /*
   * 保存成功后重新校验
   */

  if (
    formRef.value
  ) {

    formRef.value.validateField(
      'materialFile'
    )

  }


  /*
   * =====================================================
   * 上传成功提示
   * =====================================================
   */

  ElMessage.success(
    '获奖凭证上传成功'
  )

}


/*
 * =========================================================
 * 上传失败
 * =========================================================
 */

function handleUploadError(
  error,
  uploadFile
) {

  console.error(
    '========================================'
  )

  console.error(
    '文件上传失败：',
    error
  )

  console.error(
    '上传文件：',
    uploadFile
  )

  console.error(
    '========================================'
  )


  /*
   * 401
   */

  if (
    error?.status === 401
  ) {

    ElMessage.error(
      '登录已失效，请重新登录'
    )

    return

  }


  ElMessage.error(
    '获奖凭证上传失败，请检查登录状态和文件大小'
  )

}


/*
 * =========================================================
 * 删除文件
 * =========================================================
 */

function handleRemove() {

  form.materialFile =
    ''


  if (
    formRef.value
  ) {

    formRef.value.validateField(
      'materialFile'
    )

  }

}


/*
 * =========================================================
 * 查看已经上传的材料
 * =========================================================
 */

function previewFile() {

  if (
    !form.materialFile
  ) {

    ElMessage.warning(
      '当前没有上传文件'
    )

    return

  }


  let url =
    form.materialFile


  /*
   * 如果后端返回的是：
   *
   * uploads/certificate/xxx.pdf
   *
   * 这种路径不能直接访问。
   *
   * 正常情况下我们现在保存的是：
   *
   * /api/file/view/xxx
   */

  if (
    !url.startsWith('/')
  ) {

    url =
      '/' +
      url

  }


  window.open(
    url,
    '_blank'
  )

}


/*
 * =========================================================
 * 提交申报
 * =========================================================
 */

async function submitApply() {

  try {

    /*
     * 表单校验
     */

    const valid =
      await formRef.value.validate()


    if (
      !valid
    ) {

      return

    }


    /*
     * 有凭证必须已经上传
     */

    if (
      form.hasCertificate === 'YES' &&
      !form.materialFile
    ) {

      ElMessage.error(
        '请先上传获奖凭证'
      )

      return

    }


    submitting.value =
      true


    /*
     * =====================================================
     * 提交数据
     * =====================================================
     */

    const data =
      {

        awardCategory:
        form.awardCategory,


        awardName:
        form.awardName,


        awardLevel:
        form.awardLevel,


        awardGrade:
        form.awardGrade,


        awardGradeOther:
          form.awardGrade === 'OTHER'
            ? form.awardGradeOther
            : '',


        awardTime:
        form.awardTime,


        awardType:
        form.awardType,


        hasCertificate:
        form.hasCertificate,


        materialFile:
          form.hasCertificate === 'YES'
            ? form.materialFile
            : '',


        certificateReason:
          form.hasCertificate === 'NO'
            ? form.certificateReason
            : '',


        description:
        form.description,

      }


    console.log(
      '个人证书申报数据：',
      data
    )


    /*
     * =====================================================
     * 调用后台
     * =====================================================
     */

    const res =
      await addApply(
        data
      )


    console.log(
      '个人证书申报接口返回：',
      res
    )


    /*
     * =====================================================
     * 判断成功
     * =====================================================
     */

    if (
      res &&
      (
        res.code === 200 ||
        res.code === 0
      )
    ) {

      ElMessage.success(
        '个人证书申报提交成功，已进入档案部审核'
      )


      resetForm()

    }

    else {

      ElMessage.error(
        res?.message ||
        res?.msg ||
        '申报提交失败'
      )

    }

  } catch (
    error
    ) {

    console.error(
      '个人证书申报失败：',
      error
    )


    ElMessage.error(
      error?.response?.data?.message ||
      '申报提交失败，请稍后重试'
    )

  } finally {

    submitting.value =
      false

  }

}


/*
 * =========================================================
 * 重置
 * =========================================================
 */

function resetForm() {

  form.awardCategory =
    ''

  form.awardName =
    ''

  form.awardLevel =
    ''

  form.awardGrade =
    ''

  form.awardGradeOther =
    ''

  form.awardTime =
    ''

  form.awardType =
    ''

  form.hasCertificate =
    ''

  form.materialFile =
    ''

  form.certificateReason =
    ''

  form.description =
    ''


  if (
    formRef.value
  ) {

    formRef.value.clearValidate()

  }

}

</script>


<style scoped>

.apply-page {

  padding: 30px;

}


.apply-card {

  max-width: 1000px;

  margin: 0 auto;

}


.card-title {

  font-size: 20px;

  font-weight: bold;

}


.el-form {

  max-width: 850px;

}


.certificate-upload {

  width: 600px;

}


.uploaded-file {

  display: flex;

  align-items: center;

  gap: 12px;

  margin-top: 10px;

}

</style>
