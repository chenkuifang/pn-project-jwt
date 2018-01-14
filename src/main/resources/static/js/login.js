/**
 * 登陆
 * 
 * @author QuiFar
 */
$(document).ready(function() {
	// 绑定登陆事件
	$("#loginBtn").click(function() {
		if (checkData()) {
			login();
		}
	});
});

// 登陆
function login() {
	var msg = $("#loginResult");
	msg.text("");
	$.ajax({
		type : "post",
		url : "/loginPost",
		data : $("#loginForm").serialize(),
		success : function(r) {
			if (r.code == g.successCode) {
				// 保存 token
				g.setCookie("token", r.token);

				parent.location.href = '/index';
			} else {
				msg.text(r.msg);
			}
		}
	});
}

// 校验数据非空
function checkData() {
	var userName = $("#userName").val();
	var password = $("#password").val();
	var msg = $("#loginResult");
	msg.text("");

	if (g.isEmpty(userName)) {
		msg.text("用户名不能为空!");
		return false;
	}

	if (g.isEmpty(password)) {
		msg.text("密码不能为空!");
		return false;
	}

	return true;
}