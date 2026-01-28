import uploadFromComponent from './index.vue';

const userListFrom = {};

userListFrom.install = function (Vue) {
  const ToastConstructor = Vue.extend(uploadFromComponent);
  const instance = new ToastConstructor();
  instance.$mount(document.createElement('div'));
  document.body.appendChild(instance.$el);

  Vue.prototype.$modalUserList = function (callback, handleNum) {
    instance.visible = true;
    instance.callback = callback;
    instance.handleNum = handleNum || '';
  };
};

export default userListFrom;
