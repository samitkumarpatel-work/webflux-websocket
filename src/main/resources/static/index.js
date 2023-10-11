const { createApp, ref } = Vue

const socket = new WebSocket("ws://localhost:8080/ws/chat");

socket.onmessage = function (event) {};
socket.onopen = function (event) {
    document.getElementById("status").innerHTML = "Connected";
};
socket.onclose = function (event) {
    document.getElementById("status").innerHTML = "Disconnected";
};

const Home = {
    setup() {
        const message = ref('Hello vue! - HOME')
        return {
            message
        }
    },
    data() {
        return {
            username: 'test'
        }
    },
    methods: {
        isAuth() {
            return false
        }
    },
    template: `
        <div>Home</div>
        <div>{{ message }}, {{ username }}</div>
    `
}
const About = { template: '<div>About</div>' }
const Login = {
    setup() {
        const message = ref('Hello vue! - LOGIN')
        return {
            message
        }
    },
    methods: {
        login() {}
    },
    template: `
        <fieldset>
            <legend>Login:</legend>
            <input type="text" id="username" name="username" placeholder="Username">
            <button v-on:click="login">Login</button>
        </fieldset>
    `
}

const routes = [
    { path: '/', component: Home },
    { path: '/about', component: About },
    { path: '/login', component: Login },
]

const router = VueRouter.createRouter({
    history: VueRouter.createWebHashHistory(),
    routes,
})

createApp({
    setup() {
        const message = ref('Hello vue!')
        return {
            message
        }
    }
}).use(router).mount('#app')