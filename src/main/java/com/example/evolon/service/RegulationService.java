package com.example.evolon.service;

import org.springframework.stereotype.Service;

import com.example.evolon.domain.enums.PrintedRegulation;
import com.example.evolon.domain.enums.Regulation;

@Service
public class RegulationService {

	/**
	 * 印字レギュ(H/I/Jなど)から
	 * 現行ルールで STANDARD / EXTRA を判定
	 */
	public Regulation resolve(PrintedRegulation printed) {
		if (printed == null) {
			return null;
		}

		// 例：2026年ルール
		return switch (printed) {
		case H, I, J -> Regulation.STANDARD;
		default -> Regulation.EXTRA;
		};
	}
}
