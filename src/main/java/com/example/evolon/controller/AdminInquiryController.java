package com.example.evolon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.evolon.service.InquiryService;

@Controller
@RequestMapping("/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    public AdminInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /** 問い合わせ一覧（未対応/対応済 全部） */
    @GetMapping
    public String listInquiries(
            Model model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String success) {

        if ("OPEN".equals(status)) {
            model.addAttribute("inquiries", inquiryService.getOpenInquiries());
        } else {
            model.addAttribute("inquiries", inquiryService.getAllInquiries());
        }

        model.addAttribute("success", success);
        model.addAttribute("status", status); // 現在の絞り込み状態
        return "admin_inquiries";
    }

    /** 問い合わせ詳細 */
    @GetMapping("/{id}")
    public String inquiryDetail(@PathVariable Long id, Model model) {
        model.addAttribute("inquiry", inquiryService.getInquiryById(id));
        return "admin_inquiry_detail";
    }

    /** 管理者からの返信（一覧画面に戻る） */
    @PostMapping("/{id}/reply")
    public String replyInquiry(
            @PathVariable Long id,
            @RequestParam("adminReply") String adminReply) {

        inquiryService.replyToInquiry(id, adminReply);
        return "redirect:/admin/inquiries?success=reply";
    }

    /** 問い合わせを "対応済みにする"（一覧画面に戻る） */
    @PostMapping("/{id}/close")
    public String closeInquiry(@PathVariable Long id) {
        inquiryService.closeInquiry(id);
        return "redirect:/admin/inquiries?success=closed";
    }
}
