package com.example.evolon.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalStateException.class)
	public String handleIllegalState(
			IllegalStateException ex,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

		String referer = request.getHeader("Referer");
		if (referer != null && !referer.isBlank()) {
			return "redirect:" + referer;
		}

		return "redirect:/items";
	}

	@ExceptionHandler(ResponseStatusException.class)
	public String handleResponseStatus(
			ResponseStatusException ex,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		// reason が null の可能性があるのでフォールバック
		String msg = (ex.getReason() != null) ? ex.getReason() : "エラーが発生しました。";
		redirectAttributes.addFlashAttribute("errorMessage", msg);

		String referer = request.getHeader("Referer");
		if (referer != null && !referer.isBlank()) {
			return "redirect:" + referer;
		}

		return "redirect:/items";
	}
}
