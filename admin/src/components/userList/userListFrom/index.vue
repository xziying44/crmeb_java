<template>
  <div>
    <el-dialog title="选择用户" :visible.sync="visible" width="900px" :before-close="handleClose">
      <user-list
        v-if="visible"
        :handleNum="handleNum"
        @getTemplateRow="getTemplateRow"
        @getUsers="getUsers"
        @closeDialog="handleClose"
      />
    </el-dialog>
  </div>
</template>

<script>
import userList from '@/components/userList/index.vue';

export default {
  name: 'UserListFrom',
  components: { userList },
  data() {
    return {
      handleNum: '',
      visible: false,
      callback: function () {},
    };
  },
  methods: {
    handleClose() {
      this.visible = false;
    },
    // 单选回调
    getTemplateRow(row) {
      this.callback([row]);
      this.visible = false;
    },
    // 多选回调
    getUsers(users) {
      this.callback(users);
      this.visible = false;
    },
  },
};
</script>

<style scoped>
::v-deep .el-dialog__body {
  padding: 20px 24px 0 24px !important;
}
</style>
