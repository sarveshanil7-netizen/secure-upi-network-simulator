
const USERS = [
    {
        username: "Sarvesh1",
        password: "Computer1@",
        accountNumber: "111100000001",
        phone: "9000000001",
        balance: 5000
    },
    {
        username: "Srimano2",
        password: "Computer1@",
        accountNumber: "111100000002",
        phone: "9000000002",
        balance: 3500
    },
    {
        username: "Kasi3",
        password: "Computer1@",
        accountNumber: "111100000003",
        phone: "9000000003",
        balance: 4200
    },
    {
        username: "Hasiba4",
        password: "Computer1@",
        accountNumber: "111100000004",
        phone: "9000000004",
        balance: 6000
    }
];

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("loginForm");

    form.addEventListener("submit", event => {
        event.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        const user = USERS.find(
            item =>
                item.username === username &&
                item.password === password
        );

        if (!user) {
            document.getElementById("nameValid").textContent =
                "Invalid username or password.";
            document.getElementById("passwordValid").textContent = "";
            return;
        }

        let state;

        try {
            state = JSON.parse(localStorage.getItem("demoPaymentState"));
        } catch (error) {
            state = null;
        }

        if (
            !state ||
            !Array.isArray(state.accounts) ||
            !Array.isArray(state.transactions)
        ) {
            state = {
                accounts: USERS.map(item => ({
                    ...item
                })),
                transactions: []
            };
        } else {
            // Keep existing balances and transactions.
            USERS.forEach(userData => {
                const account = state.accounts.find(
                    item => item.accountNumber === userData.accountNumber
                );

                if (!account) {
                    state.accounts.push({ ...userData });
                } else {
                    account.username = userData.username;
                    account.phone = userData.phone;
                    account.password = userData.password;
                }
            });
        }

        localStorage.setItem("demoPaymentState", JSON.stringify(state));

        sessionStorage.setItem(
            "loggedInUsername",
            user.username
        );

        document.getElementById("nameValid").textContent = "";
        document.getElementById("passwordValid").textContent = "";

        window.location.href = "homepage.html";
    });
});
