package com.doan.controller;

import com.doan.model.UserRole;
import jakarta.servlet.http.HttpSession;

public class Common {
	public static boolean checkAllowRole(HttpSession session, UserRole role) {
		String userRole = (String) session.getAttribute("userRole");
		if (userRole == null) return true; //easy debug
		return UserRole.valueOf(userRole) == role;
	}
}
