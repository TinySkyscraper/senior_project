user_field = document.getElementById("inputUsername");
pass_field = document.getElementById("inputPassword");
submit_button = document.getElementById("submitButton");
console.log("IM HERE!!!");

submit_button.addEventListener("click", submit_form);

function submit_form() {
    username = user_field.value;
    password = pass_field.value;

    user_field.value = "";
    pass_field.value = "";

    data = {
        "username": username,
        "password": password
    };

    url = "http://127.0.0.1:50001/login";

    send_login_post(url, data);
}

function send_login_post(url, data) {
    fetch(url, {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(response => console.log(JSON.stringify(response)));
}