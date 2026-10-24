const API_URL = "http://localhost:8080/api";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("loginForm");
    const nameValid = document.getElementById("nameValid");
    const passwordValid = document.getElementById("passwordValid");
    const loginBtn = document.getElementById("loginBtn");

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        nameValid.textContent = "";
        passwordValid.textContent = "";

        if (!username || !password) {
            nameValid.textContent = "Please fill in all fields.";
            return;
        }

        loginBtn.disabled = true;
        loginBtn.textContent = "Verifying with Database...";

        try {
            const params = new URLSearchParams({
                username: username,
                password: password
            });

            // Requirement 2: Check database and allow ONLY registered users
            const response = await fetch(`${API_URL}/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            });

            const result = await response.json();

            if (response.ok && result.success) {
                // Store verified session details from Database
                sessionStorage.setItem("loggedInUsername", result.username);
                sessionStorage.setItem("loggedInAccount", result.accountNumber);
                sessionStorage.setItem("userPhone", result.phone || "");

                window.location.href = "homepage.html";
            } else {
                nameValid.textContent = result.message || "Invalid credentials. User not registered in database.";
                loginBtn.disabled = false;
                loginBtn.textContent = "Login";
            }
        } catch (error) {
            console.error("Login connection error:", error);
            nameValid.textContent = "Cannot connect to server. Ensure UPIApplication backend is running on port 8080.";
            loginBtn.disabled = false;
            loginBtn.textContent = "Login";
        }
    });
});
