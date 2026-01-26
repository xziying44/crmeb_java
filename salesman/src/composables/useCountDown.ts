import { ref, onUnmounted } from 'vue'

export function useCountDown(seconds = 60) {
  const count = ref(0)
  const isCounting = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    if (isCounting.value) return

    count.value = seconds
    isCounting.value = true

    timer = setInterval(() => {
      count.value--
      if (count.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isCounting.value = false
    count.value = 0
  }

  onUnmounted(() => {
    stop()
  })

  return {
    count,
    isCounting,
    start,
    stop
  }
}
