<template>
  <van-tabbar v-model="active" route class="safe-area-bottom">
    <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item to="/customer" icon="friends-o">客户</van-tabbar-item>
    <van-tabbar-item to="/promote" icon="qr">推广</van-tabbar-item>
    <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const active = ref(0)
</script>
