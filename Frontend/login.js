let nameValid = document.getElementById("nameValid");
let passwordValid = document.getElementById("passwordValid");
let username = document.getElementById("username");
let password = document.getElementById("password");
let loginBtn = document.getElementById("login-btn");

username.addEventListener("keyup", function () {
    let name = username.value;
    if (name.length < 3) {
        nameValid.textContent = "Username must be at least 3 characters long.";
        username.style.backgroundColor = "red";
        nameValid.style.color = "red";
    } else {
        nameValid.textContent = "Valid Username";
        nameValid.style.color = "green";
        username.style.backgroundColor = "#32CD32";
    }
});

password.addEventListener("keyup", function () {
    let pass = password.value;
    if (pass.length < 8) {
        passwordValid.textContent = "Password must be at least 8 characters long.";
        password.style.backgroundColor = "red";
        passwordValid.style.color = "red";
    }
    else if (!/[A-Z]/.test(pass)) {
        passwordValid.textContent = "Password must contain at least one uppercase letter.";
        password.style.backgroundColor = "red";
        passwordValid.style.color = "red";
    }
    else if (!/[0-9]/.test(pass)) {
        passwordValid.textContent = "Password must contain at least one number.";
        password.style.backgroundColor = "red";
        passwordValid.style.color = "red";
    }
    else if (!/[!@#$%^&*,.?]/.test(pass)) {
        passwordValid.textContent = "Password must contain at least one special character.";
        password.style.backgroundColor = "red";
        passwordValid.style.color = "red";
    }
    else{
        passwordValid.textContent = "Valid Password";
        passwordValid.style.color = "green";
        password.style.backgroundColor = "#32CD32";
    }
});

loginBtn.addEventListener("click", function (event) {
    if(username.style.backgroundColor === "rgb(50, 205, 50)" && password.style.backgroundColor === "rgb(50, 205, 50)") {
        window.location.href = "homepage.html";
    } else {
        event.preventDefault();
    }
});